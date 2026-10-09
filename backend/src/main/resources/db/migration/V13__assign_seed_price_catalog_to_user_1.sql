-- El catalogo es por usuario: el "Corte basico" sembrado en V10 era solo para probar un flujo
-- y pertenece al usuario 1. Se elimina el concepto de catalogo global (user_id NULL).
UPDATE price_catalog SET user_id = 1 WHERE user_id IS NULL;
