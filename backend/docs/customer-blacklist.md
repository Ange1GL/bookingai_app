# Lista negra de clientes

El barbero bloquea a un cliente directamente (`POST /customers/{id}/blacklist`, motivo opcional). Un cliente bloqueado no puede reservar (REST ni asistente de AI) y sus citas futuras se cancelan.

**Es la única forma de agregar a la lista.** No hay umbrales, conteos de inasistencias ni auto-bloqueo. Sí se puede *quitar* con `DELETE`.

Todo es por tenant (`userId` de `@CurrentUserId`, ver `customer-multi-tenancy.md`): un barbero nunca ve ni modifica la lista negra de otro; un cliente de otro tenant responde 404.

## Esquema (`V16`, `V17`, `V18`)

La lista negra vive en su propia tabla, igual que `account_blocked`; `customer` no cambia.

| Tabla | Para qué | Claves |
|---|---|---|
| `customer_blacklist` | Presencia de fila = cliente bloqueado. Guarda `reason` (`varchar(250)`, opcional) y `created_at`. | `UNIQUE (customer_id)` |

`V18` elimina `customer_no_show` (historial de inasistencias de la versión anterior).

**Longitud del motivo: 250 caracteres, fija.** Está definida en la BD (`varchar(250)`) y la refleja una única constante en código, `CustomerBlacklist.MAX_REASON_LENGTH`, que usan el dominio, el DTO de entrada (`@Size`) y la entidad (`@Column(length = 250)`). Para cambiarla hay que crear una migración nueva y actualizar esa constante.

No hay expiración por tiempo ni job: el bloqueo se quita a mano.

## Reglas

- **Bloquear** (`POST /customers/{id}/blacklist`, cuerpo opcional `{ "reason": "..." }`): el cliente debe ser del tenant (404 si no). El motivo se recorta (`strip`) y, si queda en blanco, se guarda `null`; más de 250 caracteres responde 400. Es **idempotente**: si ya estaba en la lista responde 204 sin duplicar ni cambiar el motivo.
- **Al bloquear** se cancelan, en una sola sentencia, las citas `RESERVED` del cliente con `startTime >= ahora`. Las que ya empezaron no se tocan.
- **Quitar** (`DELETE /customers/{id}/blacklist`): borra la fila. No restaura citas canceladas. 404 si no estaba en la lista.
- **Guarda de reserva**: `CustomerBlacklistGuard` (único punto) se invoca en `CreateAppointmentService` y `BookAppointmentService`, por lo que aplica igual a REST y a las tools de AI (`BookingTools`). `RescheduleAppointmentService` no lo necesita: un cliente bloqueado ya no tiene citas `RESERVED` futuras.

## Endpoints

| Método y ruta | Descripción | Respuesta |
|---|---|---|
| `POST /api/v1/customers/{id}/blacklist` | Bloquear al cliente. Cuerpo opcional `{ "reason": "..." }` (≤ 250) | 204 / 404 / 400 |
| `DELETE /api/v1/customers/{id}/blacklist` | Quitar de la lista negra | 204 / 404 |
| `GET /api/v1/customers?blacklisted=true\|false` | Filtro del listado paginado (ver `customer-list.md`) | 200 |

Errores: `404` cliente inexistente (o de otro tenant) o que no estaba en la lista al quitar, `409` cliente en lista negra al reservar, `400` motivo > 250.

### Ejemplos

`POST /api/v1/customers/12/blacklist`

```json
{ "reason": "No avisó y no llegó" }
```

Respuesta: `204 No Content`.

Motivo demasiado largo (`400`):

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "reason: El motivo no puede superar los 250 caracteres" }
```

Reservar con un cliente bloqueado (`POST /api/v1/appointments`, `409`):

```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "message": "Customer 12 is blacklisted and cannot book appointments" }
```

## Capas

- Dominio: `CustomerBlacklist` (fábrica `create`, normaliza el motivo), excepción `CustomerBlacklistedException` (409).
- Aplicación: `BlacklistCustomerService`, `RemoveCustomerFromBlacklistService`; `BlacklistCustomerAction` (guardar + cancelar futuras) y `CustomerBlacklistGuard`.
- Infraestructura: `JpaCustomerBlacklistRepositoryAdapter`, UPDATE masivo `JpaAppointmentJpaRepository.cancelReservedFrom`, `CustomerController`.

## Asistente de AI

- `searchCustomersByName` devuelve `blacklisted` por cliente (`SearchCustomersService` lo resuelve con una sola consulta `findBlacklistedIds`).
- Tools `blacklistCustomerById(customerId, reason)` y `removeCustomerFromBlacklistById(customerId)` reutilizan los casos de uso del REST.
- Flujo del prompt (`AiConfig`): si el cliente buscado está bloqueado, el asistente **no agenda**, avisa y pregunta si se quita de la lista negra o se mantiene; solo desbloquea si el usuario lo acepta. Para bloquear: buscar, confirmar (avisando que se cancelan sus citas futuras) y pedir motivo opcional sin inventarlo. La guarda sigue siendo la red de seguridad si el modelo ignora el aviso.

## Pendiente (siguiente iteración)

- `POST /api/v1/customers` (idempotente por teléfono) no informa `blacklisted` en su respuesta; el listado sí.
- Carrera en bloqueo simultáneo del mismo cliente: el `UNIQUE (customer_id)` la protege, pero respondería 500 en vez de tratarla como "ya estaba".
- Sin test de integración contra Postgres (el proyecto no tiene H2/Testcontainers); la migración `V18` no se ha ejecutado contra una base real.
