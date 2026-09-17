-- Catalogo de estados de cita. Debe reflejar 1:1 el enum domain.model.StatusAppointment:
-- el mapeo BD<->dominio se hace por status_id (StatusAppointment.fromId), asi que los ids
-- de aqui y los del enum tienen que coincidir. Al agregar un valor al enum, agregar su INSERT
-- en una migracion nueva (nunca editar esta).
--
-- Los roles y permisos NO se siembran aqui: los gestiona DataInitializer (ApplicationRunner).
INSERT INTO status_appointment (status_id, nombre) VALUES (1, 'PENDING')     ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (2, 'CANCELLED')   ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (3, 'IN_PROGRESS') ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (4, 'FINALIZED')   ON CONFLICT DO NOTHING;
