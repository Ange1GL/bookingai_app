# Plan: ver y cerrar sesiones de un usuario (admin)

Estado: **propuesto, sin implementar**. Orden: backend completo (con tests) → frontend.

## Decisiones asumidas (cambiar antes de aprobar si no aplican)

1. Cerrar una sesión individual revoca solo su refresh token; el access token vive hasta 15 min (`security.jwt.expiration`). Revocación inmediata (guardar `jti` en la fila) queda como mejora futura.
2. Se reusa el permiso `INVALID_SESSION_TOKEN` (o `ADMIN_ALL`) para listar y cerrar. Sin migración de permisos.
3. El `user_agent` se interpreta en el frontend (utilidad sin dependencias); el backend devuelve el crudo.
4. "Sesión activa" = refresh token con `revoked=false` y `expires_at > now`. No significa "en línea ahora"; la última actividad se aproxima con `issued_at` del token vigente (precisión ~15 min).

## Endpoints (todos bajo `/api/v1/users`, `@PreAuthorize(Authorities.CAN_INVALIDATE_SESSIONS)`)

| Método | Ruta | Acción | Estado |
|---|---|---|---|
| GET | `/{id}/sessions` | Lista sesiones activas | nuevo |
| DELETE | `/{id}/sessions/{sessionId}` | Cierra una sesión | nuevo |
| DELETE | `/{id}/invalid-session` | Cierra todas (revoca refresh tokens + sube `token_version`) | existente, sin cambios |

## Backend

1. **Migración Flyway `V32__add_session_started_at_to_refresh_token.sql`**
   - `add column session_started_at timestamptz`; `update ... set session_started_at = issued_at`; luego `set not null`.
   - Índice `idx_refresh_token_user_active (user_id, revoked, expires_at)`.
2. **Persistencia**
   - `RefreshTokenEntity`: campo `sessionStartedAt` (+ `@Index`).
   - `RefreshToken` (dominio): `sessionStartedAt` y `isActive()`.
   - `RefreshTokenMapper`: mapear el campo nuevo.
   - `JpaRepositoryRefreshToken`: `findAllByUserIdAndRevokedFalseAndExpiresAtAfterOrderByIssuedAtDesc(userId, now)` y `findByIdAndUserId`.
   - Puerto `RefreshTokenRepositoryPort` + `JpaRefreshTokenRepositoryAdapter`: `findActiveByUserId(userId, now)`, `findByIdAndUserId(id, userId)`.
3. **Conservar el inicio de sesión al rotar**
   - `AuthTokenIssuer.issue(...)` recibe `sessionStartedAt`; login, create-password y change-password pasan `now`; `RefreshTokenService.refresh` pasa `stored.getSessionStartedAt()`.
4. **Casos de uso**
   - `port.in`: `ListUserSessionsUseCase`, `RevokeUserSessionUseCase`.
   - `application/result`: `UserSessionResult` (id, userAgent, ipAddress, startedAt, lastActivityAt, expiresAt).
   - `application/service`: `ListUserSessionsService` (valida usuario con `UserFinder`, `@Transactional(readOnly = true)`) y `RevokeUserSessionService` (`@Transactional`; 404 si la sesión no existe, no es del usuario o ya no está activa; `log.warn` con actor, usuario y sesión).
5. **Excepción**: `domain.exception.SessionNotFoundException` + `@ExceptionHandler` 404 en `GlobalExceptionHandler` (vía `errorBody`).
6. **REST**: `UserSessionResponse` (record en `dto`), mapper (`UserRestMapper` o `UserSessionRestMapper`), y los dos endpoints nuevos en `UserController`.
7. **Tests**: servicios de listar/cerrar (activa, de otro usuario, ya revocada, inexistente), `RefreshTokenService` conserva `sessionStartedAt` al rotar, mapper, y permisos de los endpoints (403 sin permiso).
8. **Docs**: `docs/user-sessions.md` (contrato con ejemplos request/response, limitación de 15 min, significado de "activa"), actualizar CLAUDE.md/README si aplica.
9. Verificar: `mvnw test`; la app debe arrancar con `ddl-auto: validate`.

## Frontend (Angular 21 + PrimeNG; reglas de `frontend/AGENTS.md`)

1. **Modelos y servicio** en `feature/users`: `user-session.dto.ts`; métodos `listSessions(userId)`, `revokeSession(userId, sessionId)`, `revokeAllSessions(userId)` en `UsersService` o un `UserSessionsService`.
2. **Utilidad** `parse-user-agent.ts` (navegador, SO, tipo de dispositivo) en `shared/`, con tests.
3. **Ruta** `users/:id/sessions` en `users.routes.ts` (lazy) y permiso requerido.
4. **Acceso**: botón `pi-desktop` "Ver sesiones" en `user-card` y en el diálogo de detalle, dentro de `*appHasPermission`.
5. **Página** `user-sessions-page`: encabezado con usuario, volver, refrescar y "Cerrar todas las sesiones" (con confirmación). Estados: cargando, vacío, error con reintento, botones bloqueados mientras se procesa. `rxResource` como en `user-detail-dialog`.
6. **Componente** `session-card`: ícono de dispositivo, "Chrome en Windows", IP, inicio, última actividad (relativa), vencimiento y "Cerrar esta sesión" (con confirmación). Toast al terminar y recarga de la lista.
7. **Tests**: servicio, utilidad, página.
8. Verificar: `ng build`, `ng lint`, `ng test`; revisión visual en el navegador.

## Entrega

- Rama nueva desde `main` (p. ej. `feature_user_sessions`); hoy estás en `feature_ui_disable_user`.
- Commits separados Conventional Commits: `feat(sessions): ...` backend, luego `feat(users-ui): ...` frontend.
