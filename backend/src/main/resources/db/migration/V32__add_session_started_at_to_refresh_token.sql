-- Inicio de la sesión: el refresh token rota en cada renovación y issued_at se reinicia, así que sin esta columna
-- se pierde cuándo se inició la sesión. Las filas existentes arrancan con su issued_at (mejor aproximación disponible).
alter table refresh_token add column session_started_at timestamp(6) with time zone;
update refresh_token set session_started_at = issued_at;
alter table refresh_token alter column session_started_at set not null;

-- Soporta el listado de sesiones activas de un usuario (revoked = false y no vencidas).
create index idx_refresh_token_user_active on refresh_token (user_id, revoked, expires_at);
