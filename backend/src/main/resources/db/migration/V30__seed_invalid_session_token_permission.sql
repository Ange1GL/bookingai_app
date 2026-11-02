-- Permiso para invalidar (revocar) las sesiones de otro usuario. Ver Authorities.INVALID_SESSION_TOKEN.
insert into permissions (name, description, deleted, active, is_system, created_at, created_by)
values ('INVALID_SESSION_TOKEN', 'Invalidate the sessions of a user', false, true, true, now(), 'flyway')
on conflict (name) do update set is_system = true;
