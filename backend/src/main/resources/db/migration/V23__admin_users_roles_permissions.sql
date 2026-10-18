-- Administracion de usuarios, roles y permisos: estado logico (active), proteccion de catalogo del sistema
-- (is_system), vinculos con estado, y datos de contacto/credenciales del usuario.

-- ---------------------------------------------------------------------------------------------------------------
-- Estado logico en roles y permisos
-- ---------------------------------------------------------------------------------------------------------------
alter table permissions add column active boolean not null default true;
alter table permissions add column is_system boolean not null default false;
alter table roles add column active boolean not null default true;
alter table roles add column is_system boolean not null default false;

-- ---------------------------------------------------------------------------------------------------------------
-- Vinculos con estado: quitar un rol/permiso es logico (active=false); reasignar reactiva la fila.
-- Se eliminan antes duplicados (conservando el id menor) para poder imponer la unicidad.
-- ---------------------------------------------------------------------------------------------------------------
delete from user_roles a using user_roles b
where a.user_id = b.user_id and a.role_id = b.role_id and a.id > b.id;

delete from role_permissions a using role_permissions b
where a.role_id = b.role_id and a.permission_id = b.permission_id and a.id > b.id;

alter table user_roles add column active boolean not null default true;
alter table role_permissions add column active boolean not null default true;

alter table user_roles add constraint uk_user_roles_user_role unique (user_id, role_id);
alter table role_permissions add constraint uk_role_permissions_role_permission unique (role_id, permission_id);

-- ---------------------------------------------------------------------------------------------------------------
-- Usuario: telefono (envio de la contrasena temporal por WhatsApp) y cambio forzoso de contrasena
-- ---------------------------------------------------------------------------------------------------------------
alter table user_app add column phone varchar(20);
alter table user_app add column must_change_password boolean not null default false;
alter table user_app add column password_changed_at timestamp(6) with time zone;
alter table user_app add constraint uk_user_app_phone unique (phone);

-- ---------------------------------------------------------------------------------------------------------------
-- Catalogo del sistema. Idempotente e independiente de DataInitializer (en una base nueva corre antes que el).
-- ---------------------------------------------------------------------------------------------------------------
insert into permissions (name, description, deleted, active, is_system, created_at, created_by)
values ('APPOINTMENT_READ', 'Read appointments', false, true, true, now(), 'flyway'),
       ('APPOINTMENT_CREATE', 'Create appointments', false, true, true, now(), 'flyway'),
       ('APPOINTMENT_DELETE', 'Delete appointments', false, true, true, now(), 'flyway'),
       ('USER_READ', 'Read user data', false, true, true, now(), 'flyway'),
       ('USER_WRITE', 'Create and update users', false, true, true, now(), 'flyway'),
       ('USER_DELETE', 'Delete users', false, true, true, now(), 'flyway'),
       ('ADMIN_ALL', 'Full administrative access', false, true, true, now(), 'flyway'),
       ('ROLE_READ', 'Read roles', false, true, true, now(), 'flyway'),
       ('ROLE_WRITE', 'Create and update roles', false, true, true, now(), 'flyway'),
       ('PERMISSION_READ', 'Read permissions', false, true, true, now(), 'flyway'),
       ('PERMISSION_WRITE', 'Create and update permissions', false, true, true, now(), 'flyway')
on conflict (name) do update set is_system = true;

insert into roles (name, description, deleted, active, is_system, created_at, created_by)
values ('USER', 'Standard user', false, true, true, now(), 'flyway'),
       ('ADMIN', 'Administrator', false, true, true, now(), 'flyway')
on conflict (name) do update set is_system = true;

insert into role_permissions (role_id, permission_id)
select r.role_id, p.id
from roles r
         join permissions p on p.name in ('USER_READ', 'USER_WRITE', 'USER_DELETE', 'ADMIN_ALL',
                                          'ROLE_READ', 'ROLE_WRITE', 'PERMISSION_READ', 'PERMISSION_WRITE')
where r.name = 'ADMIN'
on conflict (role_id, permission_id) do nothing;

insert into role_permissions (role_id, permission_id)
select r.role_id, p.id
from roles r
         join permissions p on p.name in ('USER_READ', 'USER_WRITE', 'APPOINTMENT_CREATE',
                                          'APPOINTMENT_DELETE', 'APPOINTMENT_READ')
where r.name = 'USER'
on conflict (role_id, permission_id) do nothing;
