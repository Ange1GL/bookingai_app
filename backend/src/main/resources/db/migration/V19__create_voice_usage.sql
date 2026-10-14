-- Contadores del dictado por voz del asistente (acotan el gasto con el proveedor de transcripcion).
-- Diario por usuario y dia de negocio; mensual global (todos los usuarios), usage_month = dia 1 del mes.
create table voice_usage_daily (
    user_id bigint not null,
    usage_date date not null,
    request_count integer not null check (request_count >= 0),
    primary key (user_id, usage_date),
    constraint fk_voice_usage_daily_user foreign key (user_id) references user_app
);

create table voice_usage_monthly (
    usage_month date primary key,
    request_count integer not null check (request_count >= 0)
);
