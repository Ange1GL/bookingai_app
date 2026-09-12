INSERT INTO status_appointment (status_id, nombre) VALUES (1, 'PENDING')     ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (2, 'CANCELLED')   ON CONFLICT DO NOTHING;
INSERT INTO status_appointment (status_id, nombre) VALUES (3, 'IN_PROGRESS') ON CONFLICT DO NOTHING;
