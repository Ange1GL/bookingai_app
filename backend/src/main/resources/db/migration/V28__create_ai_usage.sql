-- Consumo diario de tokens del asistente de AI (texto). Acota el gasto con el proveedor (DeepSeek).
-- Por usuario y dia de negocio; el global suma a todos los usuarios y funciona como tope duro de gasto.
create table ai_usage_daily (
    user_id bigint not null,
    usage_date date not null,
    tokens_used bigint not null default 0 check (tokens_used >= 0),
    request_count integer not null default 0 check (request_count >= 0),
    primary key (user_id, usage_date),
    constraint fk_ai_usage_daily_user foreign key (user_id) references user_app
);

create table ai_usage_global_daily (
    usage_date date primary key,
    tokens_used bigint not null default 0 check (tokens_used >= 0)
);
