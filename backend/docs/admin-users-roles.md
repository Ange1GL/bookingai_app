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
