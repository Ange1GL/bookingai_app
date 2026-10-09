-- Cada cita referencia el servicio del catalogo que se va a cobrar.
-- Las citas existentes se asignan al servicio global "Corte básico".
alter table appointment add column price_catalog_id integer;
alter table appointment add constraint fk_appointment_price_catalog foreign key (price_catalog_id) references price_catalog;
update appointment set price_catalog_id = (
    select price_catalog_id from price_catalog where label = 'Corte básico' and user_id is null limit 1
);
alter table appointment alter column price_catalog_id set not null;
create index idx_appointment_price_catalog_id on appointment (price_catalog_id);
