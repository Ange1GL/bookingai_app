-- La lista negra ahora solo se alimenta automaticamente por inasistencias (se elimino el bloqueo
-- manual), asi que "source" ya no aporta nada.
alter table customer_blacklist drop column source;

-- Longitud fija del motivo: 250 caracteres (misma cifra que domain.model.NoShow.MAX_REASON_LENGTH).
alter table customer_blacklist alter column reason type varchar(250);

-- Motivo opcional que el barbero escribe al marcar una inasistencia.
alter table customer_no_show add column reason varchar(250);
