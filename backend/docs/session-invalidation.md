# Invalidación de sesiones

Hay dos mecanismos independientes para que un access token deje de valer antes de expirar (`security.jwt.expiration`, 15 min).
El filtro `JwtAuthenticationFilter` aplica ambos en cada request.

| | `jti` (lista negra) | `token_version` |
|---|---|---|
| Alcance | un token | todos los access tokens del usuario |
| Almacenamiento | tabla `revoked_access_token` (+ `RevokedAccessTokenCleanupJob`) | columna `user_app.token_version` |
| Código de error | `TOKEN_REVOKED` | `SESSION_REVOKED` |
| Se usa en | logout, cambio de contraseña (el token actual) | `DELETE /api/v1/users/{id}/invalid-session` |

## `jti`
Identificador único (UUID) de cada JWT, generado en `JwtTokenService.generateToken`. Al revocarlo se guarda en `revoked_access_token`
junto con su `exp`; el filtro rechaza cualquier token cuyo `jti` esté ahí. Necesita conocer el token concreto.

## `token_version`
- `user_app.token_version` (entero, default 0, migración `V31`).
- Cada access token lleva la versión vigente al emitirse en el claim `ver` (`JwtClaims.TOKEN_VERSION`). Sin claim = 0.
- El filtro compara `ver` con la versión del usuario que ya carga de BD (sin query extra) y rechaza con `SESSION_REVOKED` si difieren.
- `UserRepository.incrementTokenVersion(userId)` suma 1 con un `UPDATE` atómico. La columna es `updatable = false` y
  `UserMapper.updateEntity` la ignora, para que un `save()` nunca pise la versión.

## Invalidar la sesión de un usuario
Para ver las sesiones de un usuario o cerrar solo una, ver `user-sessions.md`.

`DELETE /api/v1/users/{id}/invalid-session` (permiso `INVALID_SESSION_TOKEN`) → `RevokeSessionByUserIdService`, en una transacción:
1. valida que el usuario exista,
2. revoca todos sus refresh tokens (no puede renovar),
3. incrementa `token_version` (sus access tokens vivos dejan de valer de inmediato),
4. registra en log quién lo hizo y a quién.

Respuesta: `204`. El cliente afectado recibe `401 SESSION_REVOKED` y, al fallar el refresh, debe cerrar sesión.

## Pendiente (fase 2, no implementada)
Reutilizar `incrementTokenVersion` en cambio de contraseña, reset por admin, `disable` y borrado de usuario.
