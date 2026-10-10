-- Soporta el listado paginado de clientes por tenant (GET /api/v1/customers) y su orden por fecha.
create index idx_customer_user_id_created_at on customer (user_id, created_at);
