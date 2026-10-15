-- Uno a uno con el dueño del tenant: la clave primaria de company es el propio user_id (sin id sustituto).
-- Al borrar company_id desaparece también la clave primaria anterior; uk_company_user_id queda redundante.
alter table company drop constraint uk_company_user_id;
alter table company drop column company_id;
alter table company add constraint pk_company primary key (user_id);
