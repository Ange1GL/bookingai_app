# Plan — Purgar la sesión completa de un usuario (refresh + access token)

> Estado: **solo plan, sin código modificado**. Pendiente de aprobación.

## 1. Problema

`DELETE /api/v1/users/{id}/invalid-session` (`UserController.invalidSession`) llama a
`RevokeSessionByUserIdService`, que únicamente ejecuta `refreshTokenRepositoryPort.revokeAllByUserId(userId)`.
El usuario no puede renovar, pero **su access token sigue válido hasta `security.jwt.expiration` = 15 min**
(no 5; ver `application.yaml:123`). Para un incidente de seguridad (actividad sospechosa, cuenta activa) es una ventana inaceptable.

El comentario del service ("el access token vive como máximo 15 min... igual criterio que `disable`") justifica la
omisión, pero `disable` sí corta el acceso de inmediato porque el filtro revisa `userDetails.isEnabled()`.
`invalid-session` **no** deshabilita al usuario, así que ese respaldo no aplica.

## 2. Qué ya existe y se puede reciclar

| Pieza | Ubicación | Estado |
|---|---|---|
| Tabla de blacklist por `jti` | `V3__create_revoked_access_token.sql`, `RevokedAccessTokenEntity`, `RevokedAccessToken`, `JpaRepositoryRevokedAccessToken`, `JpaRevokedAccessTokenRepositoryAdapter`, `RevokedAccessTokenMapper` | **Ya implementada** (tu idea de "session invalid" ya está hecha) |
| Puerto | `AccessTokenRevocationRepositoryPort` (`revoke`, `existsByJti`, `deleteExpiredBefore`) | Existe |
| Chequeo en el filtro | `JwtAuthenticationFilter:59` → `existsByJti` → `TOKEN_REVOKED` | Existe |
| Limpieza de filas vencidas | `RevokedAccessTokenCleanupJob` | Existe |
| Revocar un access token puntual | `AccessTokenRevoker` (usado por `LogoutService`, `ChangePasswordService`) | Existe, package-private |
| `jti` único por token | `JwtTokenService.generateToken` (`UUID`) | Existe |
| Permiso `INVALID_SESSION_TOKEN` | `V30` + `Authorities.CAN_INVALIDATE_SESSIONS` | Existe (verificar que esté asignado al rol admin) |
| `revokeAllByUserId` para refresh | `RefreshTokenRepositoryPort` | Existe |
| Carga del usuario desde BD **en cada request** | `JwtAuthenticationFilter` → `CustomUserDetailsService` | Existe (clave para la opción B) |
| `User.passwordChangedAt` | `V23`, `UserEntity:50` | Existe, sirve de precedente para una columna de corte |

## 3. Por qué la blacklist por `jti` no basta para "purgar a un usuario"

`revoked_access_token` necesita el `jti` de cada token. En logout/cambio de contraseña lo tenemos porque el cliente
manda su propio token. En `invalid-session` el admin actúa sobre **otro** usuario y **no guardamos qué `jti` emitimos**
(tampoco `refresh_token` lo guarda). No hay a quién insertar en la tabla.

## 4. Opciones evaluadas

### A. Guardar el `jti` de cada access token emitido y, al invalidar, volcarlos a `revoked_access_token`
- Requiere columna/tabla nueva y una escritura extra en cada login y **cada refresh** (cada 15 min por sesión).
- Con rotación hay hasta ~2 tokens vivos por sesión; hay que consultar los refresh de los últimos 15 min.
- Más piezas móviles, más limpieza, riesgo de dejar tokens sin cubrir. **No recomendada.**

### B. Marca de corte por usuario (versión de sesión) — **RECOMENDADA**
Una columna en `user_app` y un claim en el JWT; el filtro compara. Invalida **todos** los access tokens del usuario
(cualquier dispositivo) con una sola escritura, sin tabla nueva, sin job de limpieza y **sin query extra** porque el
filtro ya carga el `User` en cada request.

Dos variantes:
- **B1 `token_version` (int)** — claim `ver` en el JWT; invalidar = `token_version + 1`; el filtro rechaza si `claim != user.tokenVersion`.
  Exacto, sin problemas de reloj. **Es la que propongo.**
- **B2 `sessions_valid_after` (timestamp)** — rechaza si `iat < corte`. `iat` va en segundos, así que hay una ventana ≤1 s
  donde un token recién emitido tras la invalidación podría rechazarse (o aceptarse) según el redondeo. Da el "cuándo" para auditoría,
  pero se puede obtener también con un log.

### C. Combinar B + blacklist por `jti` (lo que ya tienes)
Se conserva `revoked_access_token` para logout y cambio de contraseña (revocar un token puntual).
B se usa para "kill de todo el usuario". No se duplica responsabilidad: **jti = un token; version = todos los tokens del usuario**.

### D. Descartadas
- Acortar `expiration` (p. ej. 5 min): reduce la ventana pero no la elimina y sube la carga de refresh.
- Redis/Caffeine: innecesario; ya hay lectura de usuario por request y no hay Redis en el stack.

## 5. Diseño propuesto (variante B1)

### Migración — `V31__add_token_version_to_user.sql`
```sql
alter table user_app add column token_version integer not null default 0;
```
Los tokens ya emitidos no traen el claim `ver`: tratarlo como `0` (coincide con el default, nadie queda fuera al desplegar).

### Dominio / persistencia
- `User` (domain): campo `tokenVersion` (int).
- `UserEntity`: `@Column(name = "token_version", nullable = false, updatable = false)` — **`updatable = false`** para que un `save()` de un `User`
  cargado antes (p. ej. un `update` de perfil concurrente) no pise el valor con uno viejo.
- Mapper `Entity <-> Domain` actualizado.
- Puerto `UserRepository`: `void incrementTokenVersion(Long userId)`. Adaptador con `@Modifying @Query("update UserEntity u set u.tokenVersion = u.tokenVersion + 1 where u.id = :id")`
  (incremento atómico, evita lost update). `@Transactional` sigue en el service, no en el adaptador.

### Emisión del token
- `TokenService.generateToken(Long subject, String username, int tokenVersion)`; `JwtTokenService` agrega `.claim(JwtClaims.TOKEN_VERSION, tokenVersion)`.
- `AuthTokenIssuer.issue` pasa `user.getTokenVersion()` (el `User` viene fresco de BD en login y refresh).
- Revisar todos los callers de `generateToken` y los tests que lo mockean.

### Decodificación y filtro
- `DecodedToken` suma `tokenVersion` (si falta el claim → `0`).
- `JwtAuthenticationFilter`, después de `loadUserByUsername` y del chequeo de `isEnabled`:
  rechazar con `JwtErrorCode.TOKEN_REVOKED` (o un `SESSION_REVOKED` nuevo si quieres distinguirlo en el front) cuando `decoded.tokenVersion() != user.getTokenVersion()`.
  Exponer la versión vía `CustomUserDetails` (ya expone `user()`), sin castear desde el filtro si se prefiere un método `tokenVersion()` en un tipo propio.

### Caso de uso
`RevokeSessionByUserIdService.revokeSessionByUserId(userId)`, en una sola transacción:
1. `userFinder.getOrThrow(userId)`
2. `refreshTokenRepositoryPort.revokeAllByUserId(userId)` (ya existe)
3. `userRepository.incrementTokenVersion(userId)` (nuevo)
4. `log.warn` / auditoría: quién (`actorId`), a quién, cuándo.

Actualizar el comentario obsoleto del service.

### Controller
`UserController.invalidSession` ya está bien (delgado, permiso correcto). Ajustes menores a decidir:
- `actorId` hoy no se usa → pasarlo al caso de uso para auditoría y/o `adminGuard.ensureNotSelf` (ver preguntas).
- Renombrar el campo `revokeSessionByUserIdUseCase` a `revokeSession` para igualar el estilo del resto.

### Reutilizar el mismo mecanismo (opcional, mismo cambio o siguiente)
- `ChangePasswordService`: hoy revoca solo el access token **actual**; los de otros dispositivos viven 15 min. `incrementTokenVersion` cierra ese hueco
  (ojo: el token nuevo se emite después del incremento, con la versión nueva).
- Reset de contraseña por admin, `disable`, borrado de usuario: incrementar versión es barato y deja un único criterio.

## 6. Tests
- `RevokeSessionByUserIdServiceTest` (nuevo): llama a revoke de refresh + incremento; usuario inexistente lanza excepción y no incrementa.
- `JwtAuthenticationFilterTest` (existe): token con versión vieja → 401 `TOKEN_REVOKED`; sin claim `ver` y usuario en versión 0 → pasa; versión igual → pasa.
- `JwtTokenService`: el token emitido contiene `ver`.
- Adaptador JPA: `incrementTokenVersion` incrementa en 1 y un `save()` posterior de un `User` viejo no lo revierte.
- Flujo: login → invalidar → petición con el access token viejo = 401; login nuevo = 200.

## 7. Documentación a actualizar (regla del proyecto)
- `docs/jwt-authentication-filter.md` (hoy describe una versión anterior del filtro; aprovechar para actualizarlo con `jti` y `ver`).
- Nuevo `docs/session-invalidation.md` o sección en `docs/security-config.md`: jti vs. versión, cuándo usar cada uno.
- `AGENTS.md` / `CLAUDE.md`: solo si se agrega convención nueva.

## 8. Riesgos y consideraciones
- **Costo**: cero queries nuevas por request (el usuario ya se carga). Una escritura por invalidación.
- **Despliegue**: seguro; default `0` y claim ausente = `0`.
- **Sesión del propio admin**: si invalida su propio usuario perderá su sesión (decidir si se bloquea).
- **Frontend**: recibirá 401 `TOKEN_REVOKED`; su interceptor intentará refresh, fallará (refresh revocado) y deberá cerrar sesión. Confirmar ese flujo.
- **Reusar refresh revocado**: `RefreshTokenService` ya trata un refresh revocado como reutilización y vuelve a revocar todo; con esto el usuario verá "sesión inválida" al intentar renovar, comportamiento esperado.

## 9. Pasos de implementación (cuando apruebes)
1. Migración V31 + `User`/`UserEntity`/mapper.
2. Puerto + adaptador `incrementTokenVersion`.
3. `TokenService`/`JwtTokenService`/`DecodedToken`/`AuthTokenIssuer` (claim `ver`).
4. Chequeo en `JwtAuthenticationFilter` (+ `JwtErrorCode` si se agrega uno).
5. `RevokeSessionByUserIdService` + ajustes de controller.
6. (Opcional) reutilizar en `ChangePasswordService` y reset por admin.
7. Tests y documentación.
8. Commit convencional por paso lógico (`feat(auth): ...`).

## 10. Preguntas abiertas
1. ¿B1 (`token_version`) o B2 (timestamp)? Recomiendo B1.
2. ¿Permitir que un admin invalide su propia sesión (`ensureNotSelf`)?
3. ¿Extender el mecanismo ya a cambio de contraseña / reset por admin / borrado, o solo `invalid-session` por ahora?
4. ¿Código de error distinto (`SESSION_REVOKED`) para que el front muestre "tu sesión fue cerrada por un administrador"?
5. ¿Confirmamos que el permiso `INVALID_SESSION_TOKEN` está asignado al rol admin (V30 solo lo inserta)?
