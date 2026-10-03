-- Soporta las consultas de calendario: citas de un usuario filtradas por rango de start_time.
create index idx_appointment_user_start_time on appointment (user_id, start_time);
