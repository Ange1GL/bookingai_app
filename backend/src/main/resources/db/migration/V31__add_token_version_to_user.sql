-- Versión de sesión del usuario: va como claim "ver" en cada access token y el filtro JWT la compara.
-- Subirla invalida de golpe todos los access tokens vivos del usuario (ver docs/session-invalidation.md).
-- Los tokens ya emitidos no traen el claim y se interpretan como 0, igual al default: nadie pierde sesión al desplegar.
alter table user_app add column token_version integer not null default 0;
