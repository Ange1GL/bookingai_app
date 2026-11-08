# Plan v2 — Invalidar todas las sesiones de un usuario con `token_version` (opción B)

> Estado: **solo plan, sin código modificado**. Reemplaza la recomendación de `plan-invalidate-user-sessions.md` con la opción B ya elegida.

## 1. Conceptos: ¿qué es el `jti` y qué es la versión?

### `jti` (JWT ID)
Un JWT es un texto firmado con *claims* (datos): `sub` (quién), `exp` (cuándo vence), `iat` (cuándo se emitió), etc.
El claim **`jti`** es un **identificador único de ese token en particular**, como el número de serie de un billete.

En este proyecto (`JwtTokenService.generateToken`) se genera con `UUID.randomUUID()`: dos tokens del mismo usuario, emitidos incluso en el mismo segundo, tienen `jti` distintos.

Para qué sirve:
- Un JWT es *stateless*: el servidor no guarda nada, solo verifica la firma. Por eso **no se puede "borrar" un token**: mientras la firma y `exp` sean válidos, se acepta.
- Con el `jti` puedes guardar una **lista negra** (`revoked_access_token`): "el token con este `jti` ya no vale". El filtro lo consulta en cada request (`existsByJti`) y lo rechaza con `TOKEN_REVOKED`.
- Se guarda junto con `exp` para que un job borre la fila cuando el token ya habría vencido de todos modos.

Limitación: revoca **un token a la vez** y necesitas conocer su `jti`. Por eso sirve para logout (el cliente manda su token) pero no para "expulsar a un usuario" desde el admin, porque no sabemos qué `jti` tiene.

### `token_version` (la versión de sesión)
Es un número entero en el usuario (`user_app.token_version`, empieza en 0) y el mismo número va **dentro de cada JWT** como claim `ver`.

- Al emitir un token: `ver = user.tokenVersion` actual.
- En cada request, el filtro compara `ver` del token con el `tokenVersion` que ya cargó de BD.
- Si **no coinciden** → el token es de una "generación" anterior → 401.
- Para invalidar a un usuario basta **sumar 1** a su `token_version`: todos sus tokens vivos (en todos los dispositivos) quedan obsoletos de golpe, sin conocer ningún `jti`.

| | `jti` (lista negra) | `token_version` |
|---|---|---|
| Alcance | un token | todos los tokens del usuario |
| Necesita conocer el token | sí | no |
| Almacenamiento | una fila por token revocado + job de limpieza | una columna |
| Uso ideal | logout, cambio de contraseña (el token actual) | "purgar" a un usuario sospechoso |

Se **conservan ambos**: cada uno resuelve un caso distinto.

## 2. Problema actual (resumen)
`invalidSession` → `RevokeSessionByUserIdService` solo revoca refresh tokens. El access token sigue válido hasta 15 min (`security.jwt.expiration`).

## 3. Diseño

### 3.1 Migración `V31__add_token_version_to_user.sql`
```sql
alter table user_app add column token_version integer not null default 0;
```
Tokens ya emitidos no traen `ver` → se interpretan como `0`, igual al default: nadie pierde sesión al desplegar.

### 3.2 Dominio y persistencia (hexagonal)
- `domain.model.User`: campo `int tokenVersion`.
- `UserEntity`: `@Column(name = "token_version", nullable = false, updatable = false)`.
  `updatable = false` evita que un `save()` con un `User` leído antes pise la versión (lost update).
- Mapper Entity ↔ Domain actualizado.
- Puerto `UserRepository`: `void incrementTokenVersion(Long userId)`.
- Adaptador + `JpaUserRepository`: `@Modifying @Query("update UserEntity u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")`. Incremento atómico en BD. La transacción queda en el service (regla del proyecto).
- No se agrega ninguna `@ManyToOne`, así que el patrón FK + relación no aplica; sí lleva su migración (checklist AGENTS.md).

### 3.3 Emisión del token
- `TokenService.generateToken(Long subject, String username, int tokenVersion)`.
- `JwtClaims`: constante `TOKEN_VERSION = "ver"` (sin strings mágicos).
- `JwtTokenService` agrega `.claim(JwtClaims.TOKEN_VERSION, tokenVersion)`.
- `AuthTokenIssuer.issue` pasa `user.getTokenVersion()`. El `User` viene fresco de BD en login y refresh.
- Revisar todos los callers de `generateToken` y los tests que lo mockean.

### 3.4 Lectura y validación
- `DecodedToken(username, jti, expiresAt)` → añade `tokenVersion` (si falta el claim, `0`).
- `JwtTokenService.decode` lee `jwt.getClaim("ver")` de forma segura (puede ser `Long`/`Integer`/null).
- `JwtAuthenticationFilter`, orden de chequeos:
  1. decode (firma, exp)
  2. `existsByJti` → `TOKEN_REVOKED` (ya existe)
  3. `loadUserByUsername`
  4. `isEnabled` → `USER_DISABLED` (ya existe)
  5. **nuevo:** `decoded.tokenVersion() != user.getTokenVersion()` → `JwtErrorCode.SESSION_REVOKED` (nuevo código, mensaje claro para el front)
- Costo: cero queries adicionales (el usuario ya se carga por request).

### 3.5 Caso de uso
`RevokeSessionByUserIdService.revokeSessionByUserId(actorId, userId)` en una transacción:
1. `userFinder.getOrThrow(userId)`
2. `refreshTokenRepositoryPort.revokeAllByUserId(userId)`
3. `userRepository.incrementTokenVersion(userId)`
4. `log.warn("Sessions revoked target={} actor={}", ...)`

Corregir el comentario obsoleto del service. Si se acepta, `adminGuard.ensureNotSelf` (pregunta 2).

### 3.6 Controller
`UserController.invalidSession`: pasar `actorId` al caso de uso; renombrar el campo a `revokeSession`. Sin cambios de contrato HTTP (204).

### 3.7 Excepciones / errores
- `JwtErrorCode.SESSION_REVOKED` + mensaje en español para el usuario.
- Sigue saliendo por `SecurityEntryPoint` con `ErrorResponse.of(...)`, igual que el resto (regla AGENTS.md).

## 4. Reutilización (fase 2, opcional)
Llamar `incrementTokenVersion` también en:
- `ChangePasswordService` (hoy solo revoca el access token actual; los de otros dispositivos duran 15 min). Incrementar **antes** de emitir el token nuevo.
- Reset de contraseña por admin, `disable`, borrado de usuario.

## 5. Tests
- `RevokeSessionByUserIdServiceTest`: revoca refresh + incrementa; usuario inexistente → excepción y sin incremento.
- `JwtAuthenticationFilterTest`: versión vieja → 401 `SESSION_REVOKED`; sin claim y usuario v0 → pasa; versión igual → pasa; `jti` revocado sigue funcionando.
- `JwtTokenService`: el token emitido incluye `ver`; decode sin claim devuelve 0.
- Adaptador JPA: incremento de +1; un `save()` posterior de un `User` viejo no revierte la versión.
- Flujo: login → invalidar → access token viejo = 401 → refresh = falla → nuevo login = 200.

## 6. Documentación
- Nuevo `docs/session-invalidation.md` (jti vs versión, cuándo usar cada uno).
- Actualizar `docs/jwt-authentication-filter.md` (está desactualizado respecto al filtro actual).
- `CLAUDE.md`/`README.md` solo si cambia algo de setup/convenciones.

## 7. Riesgos
- Si el admin se invalida a sí mismo pierde su sesión (decidir bloqueo).
- El front recibe 401 `SESSION_REVOKED`: confirmar que su interceptor cierra sesión (el refresh también fallará).
- Overflow del entero: irrelevante en la práctica.
- Rollback: el claim extra es ignorado por versiones anteriores; la columna puede quedarse.

## 8. Orden de implementación
1. V31 + `User`/`UserEntity`/mapper
2. Puerto + adaptador `incrementTokenVersion`
3. `JwtClaims`, `TokenService`, `JwtTokenService`, `DecodedToken`, `AuthTokenIssuer`
4. `JwtErrorCode.SESSION_REVOKED` + chequeo en el filtro
5. `RevokeSessionByUserIdService` + controller
6. Tests
7. Documentación
8. (Fase 2) cambio de contraseña, reset, disable, delete

Un commit convencional por paso lógico (`feat(auth): ...`).

## 9. Preguntas abiertas
1. ¿Bloquear que un admin invalide su propia sesión?
2. ¿Hacemos la fase 2 en el mismo PR o aparte?
3. ¿El permiso `INVALID_SESSION_TOKEN` está asignado al rol admin? (V30 solo lo inserta.)
