# Sesiones de un usuario (ver y cerrar)

Permite a un administrador ver dónde está abierta la sesión de un usuario (dispositivo, IP, fechas) y cerrarla, una por una o todas.

Permiso: `INVALID_SESSION_TOKEN` (o `ADMIN_ALL`) en los tres endpoints.

## Qué es una "sesión"

Una sesión es un `refresh_token` con `revoked = false` y `expires_at > now`. Cada renovación (`/auth/refresh`) revoca el token anterior y emite uno nuevo,
así que hay exactamente una fila viva por sesión. Datos que se guardan al emitir el token (login, create-password, change-password, refresh):
`user_agent` y `ip_address` (resuelta con `ClientIpResolver`).

`session_started_at` (V32) se fija al abrir la sesión y se hereda en cada rotación. `issued_at` sí se reinicia en cada rotación, por eso se expone como
`lastActivityAt`.

Limitaciones que conviene conocer:

- "Activa" significa sesión válida, **no** "en línea ahora". Quien cerró el navegador sigue apareciendo hasta que cierre sesión o venza el refresh token (30 días).
- `lastActivityAt` solo avanza cuando el access token se renueva (`security.jwt.expiration`, 15 min), no con cada petición.
- Cerrar **una** sesión revoca su refresh token; el access token ya emitido sigue valiendo hasta 15 min. Para cortar todo de inmediato se usa el endpoint de cerrar todas
  (sube `token_version`, ver `session-invalidation.md`).

## Endpoints (`/api/v1/users`)

### `GET /{id}/sessions` — sesiones activas, la más reciente primero

```json
[
  {
    "id": 42,
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ... Chrome/130.0 Safari/537.36",
    "ipAddress": "187.190.10.20",
    "startedAt": "2026-11-01T10:00:00Z",
    "lastActivityAt": "2026-11-07T09:00:00Z",
    "expiresAt": "2026-12-07T09:00:00Z"
  }
]
```

`404` si el usuario no existe. Lista vacía si no tiene sesiones activas.

### `DELETE /{id}/sessions/{sessionId}` — cierra una sesión

`204` sin cuerpo. `404` si el usuario no existe o la sesión no existe, no es de ese usuario, ya está revocada o ya venció (no se distingue una ajena de una inexistente).

### `DELETE /{id}/invalid-session` — cierra todas (sin cambios)

Revoca todos los refresh tokens y sube `token_version`. Ver `session-invalidation.md`.

## Código

`ListUserSessionsService`, `RevokeUserSessionService`, `RefreshTokenRepositoryPort` (`findActiveByUserId`, `findByIdAndUserId`), `UserSessionRestMapper`,
`SessionNotFoundException` (404). Migración `V32__add_session_started_at_to_refresh_token.sql`.
