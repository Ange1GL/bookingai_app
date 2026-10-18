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
