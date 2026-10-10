# Listado paginado de clientes

`GET /api/v1/customers` devuelve los clientes del usuario autenticado (tenant = `customer.user_id`), paginados, con filtros opcionales por nombre y teléfono.

El `userId` sale siempre de `@CurrentUserId` (ver `customer-multi-tenancy.md`); no se acepta por parámetro.

## Request

| Parámetro   | Default | Reglas                                                       |
|-------------|---------|--------------------------------------------------------------|
| `name`      | —       | Opcional. Contiene, sin distinguir mayúsculas. Máx. 100.     |
| `phone`     | —       | Opcional. Contiene. Máx. 20.                                 |
| `blacklisted` | —     | Opcional. `true` = solo en lista negra, `false` = solo los que no. Ver `customer-blacklist.md`. |
| `page`      | `0`     | `>= 0`.                                                      |
| `size`      | `20`    | `1..50`.                                                     |
| `sortBy`    | `NAME`  | `NAME` \| `CREATED_AT` (lista blanca).                       |
| `direction` | `ASC`   | `ASC` \| `DESC`.                                             |

Un filtro en blanco equivale a no filtrar. Los filtros se combinan con AND. `%`, `_` y `\` del input se escapan, así que se buscan literalmente.

Valores fuera de rango o enums desconocidos responden `400` con el `ErrorResponse` estándar.

## Response

```json
{
  "content": [{ "id": 1, "name": "Ana López", "phone": "5551234567", "blacklisted": false }],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## Diseño

- `ListCustomersUseCase` (nuevo) en vez de modificar `SearchCustomersUseCase`, que sigue devolviendo `List` para `BookingTools` (asistente de AI).
- `PageResult<T>` y `ListCustomersQuery` viven en `application/`: Spring Data `Page`/`Pageable` no sale de la capa de persistencia.
- El adapter traduce `sortBy` a la propiedad de la entidad y añade `id` como desempate para que el orden entre páginas sea estable.
- **Sin `Specification`**: solo hay dos filtros opcionales, así que basta un `@Query` JPQL (`JpaCustomerJpaRepository.findPage`). `LikePatterns` construye los patrones (`%` si no hay filtro) para no enviar parámetros `null` que Postgres no pueda tipar. Pasar a `Specification` si aparecen 4+ filtros, rangos u orden dinámico.
- El flag `blacklisted` de cada ítem sale de **una sola consulta `IN`** sobre los ids de la página (`CustomerBlacklistRepositoryPort.findBlacklistedIds`), sin N+1. El filtro `blacklisted` usa un parámetro entero (nunca `null`) con `EXISTS` sobre `customer_blacklist`.
- Migración `V15`: índice `(user_id, created_at)`. El `LIKE '%x%'` no usa btree; si el volumen crece, añadir `pg_trgm` + GIN sobre `lower(full_name)` y `phone`.

## Pendiente

- No hay test de integración del `@Query` contra Postgres (el proyecto no tiene H2 ni Testcontainers); verificar manualmente al levantar la app con la BD.
