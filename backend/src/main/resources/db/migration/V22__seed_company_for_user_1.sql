-- Negocio de prueba del usuario 1 para probar el flujo de WhatsApp. Idempotente (crea o renombra) y solo si el
-- usuario existe, para no romper bases nuevas donde aun no hay usuario 1 (la FK fallaria).
insert into company (user_id, name, created_at)
select user_id, 'Angel Test', now()
from user_app
where user_id = 1
on conflict (user_id) do update set name = excluded.name;
