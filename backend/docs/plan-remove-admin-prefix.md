# Plan: quitar el prefijo `admin` de los endpoints de usuarios, roles y permisos

## Objetivo

| Antes | Después |
|---|---|
| `/api/v1/admin/users` | `/api/v1/users` |
| `/api/v1/admin/roles` | `/api/v1/roles` |
| `/api/v1/admin/permissions` | `/api/v1/permissions` |

Solo cambia la ruta. La autorización sigue igual: cada método conserva su `@PreAuthorize`
(`USER_READ/WRITE/DELETE`, `ROLE_*`, `PERMISSION_*`, o `ADMIN_ALL`). Quitar `admin` de la URL **no** abre
nada: los endpoints siguen exigiendo permiso.

## Hallazgos (impacto real)

- **Sin colisiones:** no existe ningún controller en `/api/v1/users`, `/roles` ni `/permissions`.
- **Seguridad:** `SecurityConfig` no tiene `requestMatchers` por `/admin/**` (solo `permitAll` de auth/health/webhook y
  `anyRequest().authenticated()`), así que no hay reglas por URL que migrar.
- **Frontend:** no hay ninguna referencia a esas rutas (0 coincidencias en `frontend/`), no hay cliente que romper.
- **Breaking change de API:** cualquier consumidor externo (Postman, scripts) que use `/admin/...` recibirá 404.
  Sin versión nueva (`v2`) porque el frontend aún no los consume.

## Cambios

### 1. Controllers (`infrastructure/adapter/in/rest/controller/`)

1. `AdminUserController.java:45` → `@RequestMapping("/api/v1/users")`
2. `AdminRoleController.java:42` → `@RequestMapping("/api/v1/roles")`
3. `AdminPermissionController.java:40` → `@RequestMapping("/api/v1/permissions")`

### 2. Renombrar clases (decisión pendiente, recomendado)

`AdminUserController` → `UserController`, `AdminRoleController` → `RoleController`,
`AdminPermissionController` → `PermissionController`, para que el nombre coincida con la ruta y con el resto
(`CustomerController`, `AppointmentController`…). Usar `git mv` para conservar el historial. Antes de renombrar
hay que verificar que no choquen con otros beans/clases de ese nombre (`UserController` etc. no existen hoy en
la carpeta de controllers).

### 3. Tests

- `SecurityAccessDeniedHandlerTest.java:20` usa `"/api/v1/admin/users"` como ruta de ejemplo → `"/api/v1/users"`
  (solo cosmético, el handler no depende de la ruta).
- Si se renombran las clases, actualizar cualquier test que las referencie (la búsqueda no encontró ninguno).

### 4. Comentarios y migración

- `V24__revoke_user_management_permissions_from_user_role.sql:2` menciona `/api/v1/admin/users` en un comentario.
  **No editar** una migración ya aplicada (Flyway valida checksum, aunque sea solo un comentario). Se deja tal cual.
- `application.yaml:104` solo referencia `docs/admin-users-roles.md`; no cambia.

### 5. Documentación (obligatoria en el mismo commit)

Actualizar las rutas en:
- `docs/admin-users-roles.md` (10 ocurrencias; títulos de fase 3, 4 y 5, ejemplos y tabla de endpoints)
- `docs/otp-password-creation.md` (6 ocurrencias, incluido el diagrama)
- `docs/whatsapp-plantilla-otp.md:143`
- `docs/plan-admin-users-roles-permissions.md:47` (`/api/v1/admin/**`) — anotar que fue reemplazado por este plan
- `CLAUDE.md` / `README.md` si listan endpoints (revisar al ejecutar)

El nombre del archivo `admin-users-roles.md` se mantiene: describe la funcionalidad de administración, no la URL.

## Verificación

1. `./mvnw test` (tests de seguridad, `MethodSecurityTest`, servicios).
2. Arrancar la app y comprobar:
   - `GET /api/v1/users` con token ADMIN → 200.
   - `GET /api/v1/users` con rol USER sin permiso → 403 con `ErrorResponse`.
   - `GET /api/v1/admin/users` → 404 (ruta vieja ya no existe).
3. Buscar restos: `grep -r "api/v1/admin" backend frontend` solo debe quedar la migración V24.

## Commit

Un solo commit:
`refactor(api)!: remove admin prefix from users, roles and permissions endpoints`
con footer `BREAKING CHANGE: /api/v1/admin/{users,roles,permissions} moved to /api/v1/{users,roles,permissions}`.

## Decisiones para ti

1. ¿Renombrar también las clases a `UserController`/`RoleController`/`PermissionController`? (recomendado: sí)
2. ¿Dejar un alias temporal de las rutas viejas? (recomendado: no, no hay consumidores)
