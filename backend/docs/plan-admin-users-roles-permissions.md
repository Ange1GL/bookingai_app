# Plan — Administración de usuarios, roles y permisos

Estado: **pendiente de aprobación** (no se ha editado código).

## 1. Hallazgos del código actual (condicionan el diseño)

| Hallazgo | Consecuencia |
|---|---|
| No hay `@EnableMethodSecurity`, ni `@PreAuthorize`, ni `hasAuthority` en ningún lado: los permisos se cargan como `GrantedAuthority` (`CustomUserDetails`) pero **nada los exige**. | Hay que activar autorización por permiso antes de exponer endpoints admin; si no, cualquier usuario autenticado podría administrar. |
| `JwtAuthenticationFilter` carga el usuario en **cada request** (`loadUserByUsername`) y el JWT solo lleva `username`. | Cambios de rol/permiso aplican de inmediato (bien). Pero el filtro **no revisa `isEnabled()`**: un usuario desactivado conserva su access token hasta que expire. Hay que corregirlo. |
| `register` está comentado en `AuthController`; el alta de usuarios hoy no existe. | El endpoint admin de crear usuario pasa a ser la única vía de alta. |
| `roles`, `permissions`, `user_app` tienen `deleted` (BaseEntity) pero **no `active`**; `user_roles` / `role_permissions` no tienen `UNIQUE(user_id, role_id)` ni estado. `user_app.active` ya existe. | Migración Flyway V23 (AGENTS.md: Hibernate está en `validate`). |
| `User` no tiene teléfono. `CustomerReplyPort.send(phoneNumberId, to, text)` solo responde dentro de una conversación iniciada por el cliente; el adaptador **traga los errores**. | Para mandar la contraseña por WhatsApp hace falta teléfono en el usuario, un puerto nuevo que informe éxito/fallo y mensajes de **plantilla** (ver §5). |
| `DataInitializer` crea `USER`/`ADMIN` y permisos, pero nadie asigna `ADMIN` a un usuario. | Hay que definir el primer admin (§8, pregunta 1). |
| Paginación existente: `PageResult<T>`, `ListCustomersQuery`, `SortDirection`, `LikePatterns`, controller con `@Min/@Max` y tope 50. | Se replica el mismo patrón para usuarios/roles/permisos. |

## 2. Reglas de negocio (todo lógico, nada se borra físicamente)

**Estados.** `active=false` = deshabilitado (reversible). `DELETE` = baja lógica (`deleted=true`, además `active=false`); las listas excluyen `deleted`. Asignar/quitar rol a usuario y permiso a rol también es lógico (columna `active` en la tabla de unión; reasignar reactiva la fila).

**Permisos**
- No se puede deshabilitar/eliminar un permiso **en uso** = asignado (vínculo activo) a algún rol que no esté deleted. → `409 PermissionInUseException`.
- Permisos `system=true` (los sembrados, referenciados desde código) nunca se deshabilitan ni eliminan. El `name` es inmutable (solo se edita `description`).

**Roles**
- No se puede deshabilitar/eliminar un rol **en uso** = asignado (vínculo activo) a algún usuario no deleted. → `409 RoleInUseException`.
- Roles `system` (`USER`, `ADMIN`): no se deshabilitan, eliminan ni renombran; `ADMIN` no puede perder `ADMIN_ALL`.
- Un rol deshabilitado no se puede asignar a usuarios ni sus permisos se otorgan (el mapeo ignora roles/permisos inactivos al construir authorities).
- Solo se pueden asignar permisos activos.

**Usuarios**
- Un admin no puede deshabilitarse, eliminarse ni quitarse su propio rol ADMIN.
- Nunca puede quedar el sistema sin al menos un admin activo (`LastAdminProtectedException`).
- Todo usuario debe tener ≥1 rol activo.
- Deshabilitar/eliminar usuario → se revocan sus refresh tokens, y el filtro JWT rechaza usuarios inactivos (corta el access token vigente).
- `username` inmutable; `email` y `phone` únicos (con `uk_` nombrado).

## 3. Migración Flyway `V23__admin_users_roles_permissions.sql`

- `permissions`: `+ active boolean not null default true`, `+ system boolean not null default false`.
- `roles`: ídem. Marcar `system=true` en `USER`, `ADMIN` y los 7 permisos sembrados.
- `user_roles`, `role_permissions`: `+ active boolean not null default true`; `UNIQUE` nombrados `uk_user_roles_user_role`, `uk_role_permissions_role_permission` (previa limpieza de duplicados si existen).
- `user_app`: `+ phone varchar(20)` (solo dígitos, formato internacional; `uk_user_app_phone`), `+ must_change_password boolean not null default false`, `+ password_changed_at timestamptz`.
- Seed de permisos nuevos (el enum/catálogo va por migración, no solo por `DataInitializer`): `ROLE_READ`, `ROLE_WRITE`, `PERMISSION_READ`, `PERMISSION_WRITE`; asignarlos a `ADMIN` (junto con `USER_READ/WRITE/DELETE` ya existentes).
- Actualizar las entidades JPA en el mismo commit (checklist AGENTS.md) con el patrón FK doble campo y FK/UK nombradas.

## 4. Endpoints (`/api/v1/{users,roles,permissions}` (antes `/api/v1/admin/**`, ver plan-remove-admin-prefix.md), errores siempre `ErrorResponse` vía `GlobalExceptionHandler`)

Autorización con `@PreAuthorize("hasAuthority('…')")` por permiso (`ADMIN_ALL` también pasa).

**Permisos** (`PERMISSION_READ` / `PERMISSION_WRITE`)
- `GET /permissions?q=&active=&page=&size=&sortBy=&direction=` — paginado + buscador por nombre/descripción
- `GET /permissions/{id}` · `POST /permissions` · `PUT /permissions/{id}` (description)
- `PATCH /permissions/{id}/disable` · `PATCH /permissions/{id}/enable` · `DELETE /permissions/{id}`

**Roles** (`ROLE_READ` / `ROLE_WRITE`)
- `GET /roles?q=&active=&page=&size=…` · `GET /roles/{id}` (con permisos)
- `POST /roles` `{name, description, permissionIds}` · `PUT /roles/{id}` · `PUT /roles/{id}/permissions` `{permissionIds}` (reemplaza el conjunto, lógicamente)
- `PATCH /roles/{id}/disable|enable` · `DELETE /roles/{id}`

**Usuarios** (`USER_READ` / `USER_WRITE` / `USER_DELETE`)
- `GET /users?q=&active=&roleId=&page=&size=&sortBy=&direction=` — **paginado**; `q` busca (contiene, case-insensitive) en username, name, email y phone. `sortBy`: USERNAME, NAME, EMAIL, CREATED_AT. Tope `size` 50.
- `GET /users/{id}` · `POST /users` `{username, email, name, phone, roleIds}` → genera contraseña temporal y la envía por WhatsApp
- `PUT /users/{id}` (name, email, phone) · `PUT /users/{id}/roles` `{roleIds}`
- `PATCH /users/{id}/disable|enable` · `DELETE /users/{id}` · `POST /users/{id}/reset-password` (regenera + reenvía, vuelve a forzar cambio)

**Cambio de contraseña (cualquier usuario autenticado)**
- `POST /api/v1/auth/change-password` `{currentPassword, newPassword}`

Respuestas: `record`s en `rest.dto`, construidas por `XxxRestMapper` (nunca `Map`, nunca `?`). La respuesta de crear usuario incluye `credentialsSent: boolean`, **nunca** la contraseña.

## 5. Contraseña temporal por WhatsApp

- Puerto `TemporaryPasswordGenerator` (adapter con `SecureRandom`, 12 chars, sin caracteres ambiguos, garantiza mayúscula/minúscula/dígito/símbolo).
- Puerto `UserCredentialNotifierPort.send(phone, username, tempPassword): boolean` + adaptador `WhatsAppCredentialNotifierAdapter`. A diferencia de `CustomerReplyPort`, **reporta el resultado**; no se reutiliza ese puerto (otro propósito, otra semántica de errores).
- **Restricción de Meta (prerrequisito externo):** un mensaje iniciado por el negocio a alguien que no escribió en las últimas 24 h solo puede ser un **mensaje de plantilla aprobada** (categoría Authentication recomendada). Se agrega `whatsapp.cloud.credentials-phone-number-id`, `credentials-template-name`, `credentials-template-language`, y el adaptador envía `type: "template"` con la contraseña como parámetro. Sin token → modo demo existente.
- Orden: guardar usuario (`must_change_password=true`, hash BCrypt) en la transacción → enviar **después del commit**. Si el envío falla el usuario queda creado, `credentialsSent=false`, y el admin usa `reset-password` para reintentar (no se hace rollback).
- Seguridad: la contraseña en claro nunca se loguea, ni se devuelve por API, ni se persiste; solo vive en memoria hasta el envío. En perfil `dev` sin token de Meta se necesita una forma de probar: propuesta `admin.users.expose-temp-password=false` por defecto, `true` solo en `application-dev.yaml` (devuelve la clave en la respuesta; imposible en qa/prod por configuración).

## 6. Cambio forzoso de contraseña

- `User.mustChangePassword` → `CustomUserDetails` → `AuthTokenResult`/`AuthSuccessResponse.mustChangePassword` (el front redirige a la pantalla de cambio).
- Login funciona normal con la temporal (emite cookies) pero el backend **lo hace cumplir**: filtro `PasswordChangeRequiredFilter` (después del JWT) responde `403 ErrorResponse` (`PASSWORD_CHANGE_REQUIRED`) a todo salvo `change-password`, `logout`, `refresh`, `csrf`. No depende de que el front se porte bien.
- `ChangePasswordService`: valida contraseña actual (los fallos cuentan para el bloqueo de cuenta existente), nueva ≠ actual, política (mín. 10 caracteres, letra + dígito; configurable por constantes), hashea, `must_change_password=false`, `password_changed_at=now`, **revoca todos los refresh tokens** y emite sesión nueva.
- `reset-password` y la creación ponen `must_change_password=true`.

## 7. Estructura hexagonal (sin renombrar lo existente)

- **domain.model:** `Permission`/`Role` con `active`, `system`; `User` con `phone`, `mustChangePassword`. Reglas de "en uso", último admin, sistema protegido como métodos de dominio/guards (clases pequeñas, p. ej. `RoleDeactivationGuard`, `PermissionDeactivationGuard`, `AdminSafetyGuard`).
- **domain.exception:** `UserNotFound`, `RoleNotFound`, `PermissionNotFound` (404); `RoleInUse`, `PermissionInUse`, `DuplicateRoleName`, `DuplicatePermissionName`, `DuplicatePhone`, `LastAdminProtected`, `SystemRoleProtected` (409); `SelfModification`, `InactiveRoleAssignment`, `WeakPassword` (400/409); `PasswordChangeRequired` (403). Cada una con su `@ExceptionHandler`.
- **application:** un caso de uso por acción (SRP): `ListUsers`, `GetUser`, `CreateUser`, `UpdateUser`, `AssignUserRoles`, `ChangeUserStatus`, `DeleteUser`, `ResetUserPassword`, `ChangePassword`, y análogos para roles y permisos; `command/` y `query/` (`ListUsersQuery`…); `@Transactional` en servicios.
- **ports out:** ampliar `UserRepository` y `RoleRepository` existentes (el rename de nomenclatura de persistencia sigue siendo la tarea pendiente aparte); nuevo `PermissionRepositoryPort`; `TemporaryPasswordGenerator`, `UserCredentialNotifierPort`.
- **infrastructure:** controllers `Admin{Users,Roles,Permissions}Controller`, DTOs + `RestMapper`s; queries paginadas con `@Query` + `LikePatterns` (usuarios con roles: `@EntityGraph`/`@BatchSize` para evitar N+1 sobre `userRoles` EAGER y no paginar en memoria); `@EnableMethodSecurity` en `SecurityConfig`; `PasswordChangeRequiredFilter`; corrección de `JwtAuthenticationFilter` para usuarios inactivos; `CustomUserDetails` ignora roles/permisos inactivos.

## 8. Preguntas abiertas (con mi default si no dices nada)

1. **Primer admin:** ¿qué usuario existente es admin hoy? Default: variable `BOOTSTRAP_ADMIN_USERNAME`; `DataInitializer` le asigna `ADMIN` si existe y aún no lo tiene (idempotente).
2. **Teléfono obligatorio** al crear usuario (necesario para enviar la clave). Default: obligatorio, formato internacional solo dígitos.
3. **Plantilla de WhatsApp:** ¿ya tienes (o puedes crear/aprobar en Meta) una plantilla de autenticación y un `phone_number_id` emisor? Es bloqueante para producción; en desarrollo se usa el flag de §5.
4. **Vínculos lógicos con `active`** (rol↔usuario, permiso↔rol) en vez de borrado físico: es lo que entendí de "todo lógico"; implica filtrar `active` en consultas. Default: sí.
5. **Política de contraseña:** mín. 10, letra + dígito. Sin expiración de la temporal por ahora (se puede añadir `temp_password_expires_at` después).

## 9. Fases (un commit conventional cada una, docs incluidas)

1. `feat(db)`: V23 + entidades/dominio/mappers + seed y flags `system`.
2. `feat(security)`: method security, permisos nuevos, bootstrap admin, rechazo de usuarios inactivos en el filtro JWT.
3. `feat(admin)`: permisos (CRUD lógico + guard "en uso" + lista paginada).
4. `feat(admin)`: roles (CRUD, asignación de permisos, guard "en uso", lista paginada).
5. `feat(admin)`: usuarios (lista paginada con buscador, edición, roles, habilitar/deshabilitar, protecciones de último admin/self).
6. `feat(admin)`: alta de usuario + contraseña temporal + notificación por WhatsApp + reset-password.
7. `feat(auth)`: `must_change_password`, filtro de enforcement y `change-password`.
8. `test` + `docs`: tests unitarios de guards/servicios y de filtros; `docs/admin-users-roles.md` (endpoints con ejemplos request/response, reglas, config de WhatsApp) y referencias en docs existentes (`security-config.md`, `whatsapp-webhook.md`).

Fuera de alcance: UI Angular, expiración de contraseña temporal, auto-registro público, MFA.
