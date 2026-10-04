-- Catalogo de estados reducido a lo que realmente se guarda: RESERVED y CANCELLED.
-- "En curso" y "terminada" se derivan de start_time/end_time, no se persisten.
-- Debe seguir reflejando 1:1 el enum domain.model.StatusAppointment (mapeo por status_id).
--
-- Se conserva el id 1 (antes PENDING) para no tocar la tabla appointment.
-- Si alguna cita apunta a los ids 3 o 4, el DELETE falla por fk_appointment_status
-- y la migracion se revierte, en vez de dejar datos huerfanos.
UPDATE status_appointment SET nombre = 'RESERVED' WHERE status_id = 1;
DELETE FROM status_appointment WHERE status_id IN (3, 4);
