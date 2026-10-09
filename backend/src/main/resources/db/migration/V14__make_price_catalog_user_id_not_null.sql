-- El catalogo es siempre por usuario (ya no existen servicios globales con user_id NULL).
-- Salvaguarda idempotente igual que V13: cualquier fila huerfana pasa al usuario 1.
UPDATE price_catalog SET user_id = 1 WHERE user_id IS NULL;
ALTER TABLE price_catalog ALTER COLUMN user_id SET NOT NULL;

-- Sin coalesce: user_id ya no es nulo. Se conserva el nombre porque el adapter lo usa para traducir a 409.
DROP INDEX uk_price_catalog_user_label_active;
CREATE UNIQUE INDEX uk_price_catalog_user_label_active ON price_catalog (user_id, lower(label)) WHERE active;
