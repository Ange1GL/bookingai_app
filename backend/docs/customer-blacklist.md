# Lista negra de clientes (no-shows)

Un cliente que reserva y no se presenta (no-show) puede mandarse a la lista negra del barbero. Un cliente en lista negra **no puede reservar** (REST ni asistente de AI) y sus citas futuras se cancelan.

Todo es por tenant (`userId` de `@CurrentUserId`, ver `customer-multi-tenancy.md`): un barbero nunca ve ni modifica la lista negra de otro; un cliente de otro tenant responde 404.

## Esquema (`V16__create_customer_blacklist.sql`)

La lista negra vive en su propia tabla, igual que `account_blocked`; `customer` no cambia.

| Tabla | Para qué | Claves |
|---|---|---|
| `customer_blacklist` | Presencia de fila = cliente bloqueado. Guarda `reason`, `source` (`MANUAL` \| `AUTO_NO_SHOW`) y `created_at`. | `UNIQUE (customer_id)` |
| `customer_no_show` | Historial de inasistencias. `cleared_at` se llena al sacar al cliente de la lista negra. | `UNIQUE (appointment_id)` (una cita se marca no-show una sola vez) |

A diferencia de `account_blocked`, **no hay expiración por tiempo ni job**: el bloqueo se quita a mano.

## Reglas

- **Marcar no-show** (`POST /appointments/{id}/no-show`): la cita debe ser del tenant, estar `RESERVED` y haber comenzado ya (`startTime <= ahora` en la zona del negocio). Una cita cancelada o futura responde 409; repetirlo sobre la misma cita también (409). El estado de la cita no cambia.
- **Auto-bloqueo**: al acumular `booking.blacklist.no-show-threshold` (default **3**, `application.yaml`) no-shows *vigentes* (`cleared_at IS NULL`) el cliente entra a la lista negra con `source = AUTO_NO_SHOW` y motivo `Auto: N inasistencias`. `0` desactiva el auto-bloqueo.
- **Bloqueo manual** (`PUT /customers/{id}/blacklist`): idempotente; si ya estaba bloqueado se conserva la entrada original (no se pisa el motivo ni la fuente).
- **Al bloquear** (manual o automático) se cancelan, en una sola sentencia, las citas `RESERVED` del cliente con `startTime >= ahora`. Las que ya empezaron no se tocan.
- **Quitar** (`DELETE /customers/{id}/blacklist`): borra la fila y *perdona* los no-shows vigentes (`cleared_at`), así el umbral vuelve a contar desde cero. El historial se conserva. No restaura citas canceladas. 404 si no estaba en la lista.
- **Guarda de reserva**: `CustomerBlacklistGuard` (único punto) se invoca en `CreateAppointmentService` y `BookAppointmentService`, por lo que aplica igual a REST y a las tools de AI (`BookingTools`). `RescheduleAppointmentService` no lo necesita: un cliente bloqueado ya no tiene citas `RESERVED` futuras.

## Endpoints

| Método y ruta | Descripción | Respuesta |
|---|---|---|
| `PUT /api/v1/customers/{id}/blacklist` | Poner en lista negra. Cuerpo opcional `{ "reason": "..." }` (≤ 255) | 200 |
| `DELETE /api/v1/customers/{id}/blacklist` | Quitar de la lista negra | 204 / 404 |
| `GET /api/v1/customers/{id}/no-shows` | Historial de inasistencias | 200 |
| `POST /api/v1/appointments/{id}/no-show` | Marcar la cita como inasistencia | 200 |
| `GET /api/v1/customers?blacklisted=true\|false` | Filtro del listado paginado (ver `customer-list.md`) | 200 |

Errores: `404` cliente/cita inexistente (o de otro tenant), `409` cliente en lista negra al reservar / cita no elegible / no-show duplicado, `400` motivo > 255 o parámetros inválidos.

### Ejemplos

`PUT /api/v1/customers/12/blacklist`

```json
{ "reason": "No se presentó a dos citas" }
```

```json
{ "customerId": 12, "reason": "No se presentó a dos citas", "source": "MANUAL", "cancelledAppointments": 2 }
```

`POST /api/v1/appointments/40/no-show` (tercer no-show: cruza el umbral)

```json
{ "id": 6, "appointmentId": 40, "customerId": 12, "activeNoShows": 3, "customerBlacklisted": true }
```

`GET /api/v1/customers/12/no-shows`

```json
[{ "id": 6, "appointmentId": 40, "createdAt": "2026-10-09T22:07:39.330133Z" }]
```

Reservar con un cliente bloqueado (`POST /api/v1/appointments`):

```json
{ "timestamp": "...", "status": 409, "error": "Conflict", "message": "Customer 12 is blacklisted and cannot book appointments" }
```

## Capas

- Dominio: `CustomerBlacklist`, `NoShow`, `BlacklistPolicy` (umbral), `Appointment.ensureNoShowRegistrable`, excepciones `CustomerBlacklistedException`, `NoShowNotAllowedException`, `NoShowAlreadyRegisteredException` (todas 409).
- Aplicación: `BlacklistCustomerService`, `RemoveCustomerFromBlacklistService`, `RegisterNoShowService`, `ListCustomerNoShowsService`; `BlacklistCustomerAction` (paso común: guardar + cancelar futuras) y `CustomerBlacklistGuard`.
- Infraestructura: adapters `JpaCustomerBlacklistRepositoryAdapter` / `JpaNoShowRepositoryAdapter`, UPDATE masivo `JpaAppointmentJpaRepository.cancelReservedFrom`, `BlacklistProperties` + `BlacklistConfig` (exponen `BlacklistPolicy` sin que `application/` dependa de la config).

## Pendiente (siguiente iteración)

- Refactor de reglas/prompt de AI: exponer `blacklisted` en `CustomerSummary` de `BookingTools` y ajustar las instrucciones del modelo. Hoy la guarda ya corta la reserva, pero el modelo solo ve el error.
- `POST /api/v1/customers` (idempotente por teléfono) no informa `blacklisted` en su respuesta; el listado sí.
- Carrera en alta simultánea del mismo cliente: el `UNIQUE (customer_id)` la protege, pero respondería 500 en vez de tratarla como "ya estaba".
- Sin test de integración contra Postgres (el proyecto no tiene H2/Testcontainers); se verificó manualmente con la app en perfil dev.
