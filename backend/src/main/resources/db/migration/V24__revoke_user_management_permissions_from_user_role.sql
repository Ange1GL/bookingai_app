-- USER_READ / USER_WRITE nunca se exigieron en ningun endpoint, asi que el rol USER (clientes del negocio)
-- los tenia sin efecto. Ahora los endpoints /api/v1/admin/users los exigen: sin esta migracion cualquier
-- usuario normal podria listar y editar a todos los demas. La revocacion es logica (active=false).
update role_permissions
set active = false
where role_id = (select role_id from roles where name = 'USER')
  and permission_id in (select id from permissions where name in ('USER_READ', 'USER_WRITE'));
