-- Baja logica del catalogo: las citas referencian price_catalog por FK, asi que "eliminar"
-- un servicio solo lo marca inactivo (deja de listarse y de asignarse a citas nuevas).
alter table price_catalog add column active boolean not null default true;
-- No se repite el nombre de un servicio activo dentro del mismo usuario (globales: user_id NULL).
create unique index uk_price_catalog_user_label_active on price_catalog (coalesce(user_id, 0), lower(label)) where active;
