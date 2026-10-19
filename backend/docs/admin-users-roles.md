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

## Fase 5 — Usuarios (`/api/v1/admin/users`)

Lectura: `USER_READ`; edición/roles/estado: `USER_WRITE`; baja: `USER_DELETE` (todas también con `ADMIN_ALL`).
El alta de usuarios y el reset de contraseña llegan en la fase 6.

| Método y ruta | Acción |
|---|---|
| `GET /` | Lista **paginada** con buscador. Query: `q` (contiene, sin distinguir mayúsculas, sobre username, nombre, email y teléfono), `active`, `roleId`, `page` (0), `size` (20, máx. 50), `sortBy` (`USERNAME`\|`NAME`\|`EMAIL`\|`CREATED_AT`), `direction` |
| `GET /{id}` | Detalle |
| `PUT /{id}` | Edita `name`, `email` y `phone` (`phone` null = no cambiar). El `username` es inmutable |
| `PUT /{id}/roles` | **Reemplaza** los roles: `{"roleIds":[…]}` (mínimo uno, todos activos) |
| `PATCH /{id}/enable` · `/disable` | Habilita / deshabilita (idempotentes). Deshabilitar revoca sus refresh tokens |
| `DELETE /{id}` | Baja lógica (`deleted=true`, `active=false`), 204 |

```http
GET /api/v1/admin/users?q=ang&active=true&roleId=2&page=0&size=20&sortBy=USERNAME&direction=ASC

200 OK
{ "content": [ { "id": 1, "username": "angel", "name": "Angel", "email": "angel@example.com",
                 "phone": "5215512345678", "active": true, "mustChangePassword": false,
                 "roles": [ { "id": 2, "name": "ADMIN" } ] } ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
```

La respuesta nunca incluye la contraseña ni datos de sesión.

Reglas:

- **No se puede** deshabilitarse, eliminarse ni quitarse el rol ADMIN a uno mismo → `409`.
- **Último admin:** deshabilitar, eliminar o quitar ADMIN al único admin activo → `409`.
- Roles inexistentes o deshabilitados, o lista vacía → `400`.
- Email o teléfono duplicado → `409` (los valores de usuarios eliminados siguen reservados por los UNIQUE).
- El teléfono se normaliza a solo dígitos (8–15, formato internacional; se aceptan `+`, espacios y guiones).
- Un usuario eliminado deja de poder iniciar sesión o renovar su sesión (las búsquedas por username/email/id
  ignoran `deleted`).
- Quitar un rol es lógico (`user_roles.active=false`); reasignarlo reactiva la misma fila.

### Corrección de seguridad incluida (V24)

El rol `USER` (los dueños de negocio) tenía `USER_READ` y `USER_WRITE`, que hasta ahora ningún endpoint
exigía. Como los endpoints admin sí los exigen, **cualquier usuario normal habría podido listar y editar a
todos los demás**. `V24` revoca (lógicamente) esos dos permisos del rol `USER` y `DataInitializer` ya no los
asigna. `USER_READ/WRITE/DELETE` quedan solo para `ADMIN`.

### Cambios que tocan código previo

- `UserRepository.save` ahora actualiza la fila existente (MapStruct `@MappingTarget`) en vez de reemplazarla:
  conserva `deleted` y la auditoría. Sigue siendo solo-añadir en roles, pero ahora **reactiva** un vínculo
  revocado (antes `BootstrapAdminInitializer` habría dado por asignado un ADMIN revocado sin efecto).
- `@BatchSize(50)` en `UserEntity.userRoles` y `RoleEntity.rolePermissions` para no hacer N+1 al listar.
