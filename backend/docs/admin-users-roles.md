# Administración de usuarios, roles y permisos

Plan completo y fases: [plan-admin-users-roles-permissions.md](plan-admin-users-roles-permissions.md).
Este documento describe lo ya implementado (se amplía en cada fase).

## Fase 1 — Modelo de datos (`V23`)

| Tabla | Cambio |
|---|---|
| `permissions`, `roles` | `active` (deshabilitado lógico) e `is_system` (catálogo base, intocable) |
| `user_roles`, `role_permissions` | `active` (vínculo revocado lógicamente; reasignar reactiva la fila) y `UNIQUE` nombrado |
| `user_app` | `phone` (único), `must_change_password`, `password_changed_at` |

Semántica ya aplicada en el mapeo:

- `UserEntity.getRoleEntities()` solo devuelve roles con vínculo activo, rol `active` y no `deleted`.
- `RoleEntity.getPermissionEntities()` análogo para permisos. Por eso `CustomUserDetails` ya no
  otorga authorities de roles/permisos deshabilitados.
- Dominio: `Role`/`Permission` exponen `active` y `system`; `User` expone `phone`,
  `mustChangePassword` y `passwordChangedAt`.

Pendiente para fases siguientes: las consultas `existsBy...` de los vínculos siguen viendo filas
inactivas (a propósito, para poder reactivarlas en lugar de violar el `UNIQUE`); los adaptadores de
guardado aún no preservan `deleted` al hacer `save` desde el dominio (se resuelve cuando se
implemente la baja lógica).

## Fase 2 — Autorización por permiso y primer admin

- **`@EnableMethodSecurity`** en `SecurityConfig`. Hasta ahora los permisos se cargaban como
  `GrantedAuthority` pero ningún endpoint los exigía. Los endpoints admin usarán
  `@PreAuthorize("hasAnyAuthority(<permiso>, ADMIN_ALL)")`, con los nombres en
  `infrastructure.security.Authorities` (sin strings sueltos).
- **403 uniforme:** `SecurityAccessDeniedHandler` responde `ErrorResponse` (status 403) cuando un usuario
  autenticado no tiene el permiso. (Sin autenticar sigue siendo 401 por `SecurityEntryPoint`.)
- **Usuario deshabilitado = sin acceso inmediato:** `JwtAuthenticationFilter` rechaza con 401
  (`JwtErrorCode.USER_DISABLED`) a quien tenga `active=false`, aunque su access token no haya expirado.
- **Primer administrador:** variable de entorno `BOOTSTRAP_ADMIN_USERNAME` (`admin.bootstrap.username`).
  Al arrancar, `BootstrapAdminInitializer` (después de `DataInitializer`) asigna `ADMIN` a ese usuario
  existente. Idempotente; vacío = no hace nada; si el usuario no existe solo registra un warning.
  Pasar a ser admin conserva los roles que ya tenía.

> Despliegue: antes de usar los endpoints admin, define `BOOTSTRAP_ADMIN_USERNAME` con el username del
> dueño actual (en `.env` para dev; en Parameter Store para QA). Si no, nadie tendrá `ADMIN_ALL`.

## Fase 3 — Permisos (`/api/v1/admin/permissions`)

Lectura: `PERMISSION_READ` o `ADMIN_ALL`. Escritura: `PERMISSION_WRITE` o `ADMIN_ALL` (403 si falta).
Errores siempre con cuerpo `ErrorResponse`. Los eliminados lógicamente nunca aparecen ni se pueden consultar (404).

| Método y ruta | Acción |
|---|---|
| `GET /` | Lista paginada. Query: `q` (nombre o descripción, contiene, sin distinguir mayúsculas), `active` (true/false), `page` (0), `size` (20, máx. 50), `sortBy` (`NAME`\|`CREATED_AT`), `direction` (`ASC`\|`DESC`) |
| `GET /{id}` | Detalle |
| `POST /` | Crea (201). `name` se normaliza a MAYÚSCULAS y debe ser `UPPER_SNAKE_CASE` (≤100); `description` ≤255, opcional |
| `PUT /{id}` | Solo cambia `description` (el nombre es inmutable: el código autoriza por nombre) |
| `PATCH /{id}/disable` · `/enable` | Deshabilita / rehabilita (idempotentes) |
| `DELETE /{id}` | Baja lógica (`deleted=true`, `active=false`), 204 |

```http
POST /api/v1/admin/permissions
{ "name": "report_read", "description": "Read reports" }

201 Created
{ "id": 12, "name": "REPORT_READ", "description": "Read reports", "active": true, "system": false }
```

```http
GET /api/v1/admin/permissions?q=user&active=true&page=0&size=20&sortBy=NAME&direction=ASC

200 OK
{ "content": [ { "id": 4, "name": "USER_READ", "description": "Read user data", "active": true, "system": true } ],
  "page": 0, "size": 20, "totalElements": 3, "totalPages": 1 }
```

Reglas de negocio:

- **En uso** = asignado con vínculo activo a algún rol no eliminado. Deshabilitar o eliminar un permiso en
  uso → `409` ("…está asignado a uno o más roles…").
- Permisos `system` (catálogo base) nunca se deshabilitan ni eliminan → `409`.
- Nombre duplicado → `409`. El nombre es único **incluso entre eliminados**; para reutilizarlo hay que
  rehabilitar (fase posterior) o elegir otro.
- Deshabilitar un permiso ya inactivo (o habilitar uno activo) no falla ni cambia nada.
- Un permiso deshabilitado deja de otorgarse a los usuarios (ver `RoleEntity.getPermissionEntities()`).

Código: casos de uso `List/Get/Create/Update/ChangeStatus/DeletePermission*`, guard compartido
`PermissionDeactivationGuard`, puerto `PermissionRepositoryPort` y adaptador `JpaPermissionRepositoryAdapter`.
`PageResponse<T>` es el contenedor genérico de página que reutilizarán roles y usuarios.

## Fase 4 — Roles (`/api/v1/admin/roles`)

Lectura: `ROLE_READ` o `ADMIN_ALL`. Escritura: `ROLE_WRITE` o `ADMIN_ALL`. Mismo formato de lista, errores y
baja lógica que permisos.

| Método y ruta | Acción |
|---|---|
| `GET /` | Lista paginada (`q`, `active`, `page`, `size`≤50, `sortBy` `NAME`\|`CREATED_AT`, `direction`). Cada rol incluye sus permisos vigentes |
| `GET /{id}` | Detalle con permisos |
| `POST /` | Crea (201). `name` se normaliza a MAYÚSCULAS, `UPPER_SNAKE_CASE`, ≤50. `permissionIds` opcional |
| `PUT /{id}` | Cambia `name` y `description` |
| `PUT /{id}/permissions` | **Reemplaza** el conjunto de permisos: `{"permissionIds":[…]}` (lista vacía = quitar todos) |
| `PATCH /{id}/enable` · `/disable` | Habilita / deshabilita (idempotentes) |
| `DELETE /{id}` | Baja lógica, 204 |

```http
POST /api/v1/admin/roles
{ "name": "receptionist", "description": "Front desk", "permissionIds": [1, 2] }

201 Created
{ "id": 3, "name": "RECEPTIONIST", "description": "Front desk", "active": true, "system": false,
  "permissions": [ { "id": 1, "name": "APPOINTMENT_READ", "description": "Read appointments", "active": true, "system": true },
                   { "id": 2, "name": "APPOINTMENT_CREATE", "description": "Create appointments", "active": true, "system": true } ] }
```

Reglas de negocio:

- **En uso** = asignado con vínculo activo a algún usuario no eliminado. Deshabilitar/eliminar un rol en
  uso → `409`. Roles `system` (`USER`, `ADMIN`) nunca se deshabilitan, eliminan ni renombran → `409`
  (su descripción sí se puede editar).
- `ADMIN` no puede perder `ADMIN_ALL` al reasignar permisos → `409`.
- `permissionIds` con ids inexistentes/eliminados o permisos deshabilitados → `400` (`InvalidFieldException`).
- Quitar un permiso a un rol es lógico (`role_permissions.active=false`); volver a asignarlo reactiva la
  misma fila, no crea otra (respeta `uk_role_permissions_role_permission`).
- Nombre duplicado → `409`, incluso contra roles eliminados.

Detalles de implementación: `RolePermissionsAssigner` valida y aplica el conjunto (lo usan alta y
reasignación); `RoleDeactivationGuard` concentra las reglas de disable/delete; `ActiveFilter` es el filtro de
estado compartido (entero 0/1/2) de las consultas paginadas de permisos y roles.
`RoleRepository` (puerto existente) se amplió en vez de renombrarse, por la tarea pendiente de nomenclatura
de persistencia. Los listados cargan permisos de forma EAGER (hasta 50 roles por página).
