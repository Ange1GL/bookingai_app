# Especificaciones del MVP — BookingApp

## 1. Objetivo y alcance

BookingApp es un SaaS multi-tenant para barberos. **Cada barbero (`User`) es un tenant/admin**: solo ve y gestiona sus propios clientes y citas (ver `docs/customer-multi-tenancy.md`).

Este proyecto tambien es para que el desarrollador aprenda mas sobre AI y los Agentes ya que esta en tendencia este 2026


Decisiones de producto para el MVP:

- El frontend consume la API REST para mostrar las citas en una **vista tipo Google Calendar** (con su estatus).
- **No se envían notificaciones a los clientes** (implica costo: SMS/WhatsApp/email).
- Las notificaciones son **solo para el barbero**: p. ej. *"En 5 minutos tienes cita con {cliente}"*.


---

## 2. Estado actual (análisis de `application/service` y `domain/model`)

| Capacidad | Estado | Clases |
|---|---|---|
| Registro, login, refresh, logout | Existe | `RegisterUserService`, `AuthenticateUserService`, `RefreshTokenService`, `LogoutService` |
| Bloqueo de cuenta / rate limiting | Existe | `AccountBlocked`, `docs/account-lockout.md`, `docs/rate-limiting.md` |
| Cliente: crear (find-or-create por teléfono + `userId`) y buscar por nombre | Existe (sin REST de búsqueda) | `CreateCustomerService`, `SearchCustomersService` |
| Cita: crear / agendar con cliente nuevo / reagendar / cancelar | Existe en application | `CreateAppointmentService`, `BookAppointmentService`, `RescheduleAppointmentService`, `CancelAppointmentService` |
| Consulta de citas | Parcial: por cliente y por minuto exacto | `QueryAppointmentsService` |
| Estatus de cita | Solo `RESERVED` y `CANCELLED` (V8). "En curso"/"terminada" se derivan de la hora | `StatusAppointment` |
| Endpoints REST de citas | **No existen** (solo `/api/v1/auth` y `/api/v1/agent`) | — |
| Jobs programados | Existen (limpieza de tokens, desbloqueo) | `SchedulingConfig`, `ResetAccountLockedJob`, `RevokedAccessTokenCleanupJob` |
| Notificaciones al barbero | **No existen** | — |

### Brechas detectadas

1. **El frontend no puede consumir citas**: falta el controlador REST de `Appointment`.
2. **Bug multi-tenant en anti-empalme**: `AppointmentRepositoryPort.isOverlapping(...)` y `findByTimeSlot(...)` **no filtran por `userId`**. Un barbero puede ser bloqueado por la cita de otro barbero. Debe corregirse antes de abrir el MVP a más de un tenant.
3. **Falta consulta por rango** (día/semana/mes), necesaria para el calendario.
4. ~~El estatus no evoluciona~~: decidido. Solo se guardan `RESERVED` y `CANCELLED`; cancelar o reagendar exige `RESERVED` (409 si no).
5. `Appointment` no tiene servicio, notas ni precio.
6. Se usa `LocalDateTime` sin zona horaria del tenant: afecta recordatorios y calendario.
7. No hay canal de notificación hacia el barbero.

---

## 3. Funcionalidades del MVP

Prioridad: **P0** = imprescindible para el MVP, **P1** = deseable si el tiempo alcanza.

### F1 — Calendario de citas (P0)

Endpoint para que el frontend pinte el calendario (vista día/semana/mes) con el estatus de cada cita.

```
GET /api/v1/appointments?from=2026-10-05T00:00:00&to=2026-10-12T00:00:00
Authorization: cookie HttpOnly (JWT)
```

- `from` y `to` obligatorios, ISO-8601; Las canceladas no se devuelven; el estatus de cada cita viaja en la respuesta y el front puede filtrar.
- Siempre filtrado por el usuario autenticado (`@CurrentUserId`); el `userId` nunca viaja en la request.
- Validar rango máximo (p. ej. 45 días) para evitar consultas enormes.
- Ordenado por `startTime` ascendente.

Respuesta (`AppointmentResponse`, `record` en `infrastructure.adapter.in.rest.dto`):

```json
[
  {
    "id": 42,
    "startTime": "2026-10-05T15:00:00",
    "endTime": "2026-10-05T15:45:00",
    "status": "RESERVED",
    "customer": { "id": 7, "name": "Sonia Acosta", "phone": "527714056025" }
  }
]
```

Sugerencia para el front: `RESERVED` azul; "en curso" (ámbar) y "terminada" (verde) se calculan con `startTime`/`endTime` y la hora actual, sin guardarse en la BD.

**Aceptación:** un barbero nunca recibe citas de otro; el rango es inclusivo en `from` y exclusivo en `to`; el estatus viene en cada cita.

### F2 — Gestión de citas por REST (P0)

Exponer los casos de uso que ya existen:

| Acción | Endpoint | Use case existente |
|---|---|---|
| Crear con cliente existente | `POST /api/v1/appointments` | `CreateAppointmentUseCase` |
| Crear con cliente nuevo (nombre + teléfono) | `POST /api/v1/appointments/book` | `BookAppointmentUseCase` |
| Ver detalle | `GET /api/v1/appointments/{id}` | nuevo `findById` en `QueryAppointmentsUseCase` |
| Reagendar (drag & drop en calendario) | `PATCH /api/v1/appointments/{id}/reschedule` | `RescheduleAppointmentUseCase` |
| Cancelar | `PATCH /api/v1/appointments/{id}/cancel` | `CancelAppointmentUseCase` |

Errores de dominio (`AppointmentOverlapException`, `AppointmentNotFoundException`, `InvalidAppointmentTimeRangeException`) se traducen en el `@ControllerAdvice` existente a `ErrorResponse`. Una cita de otro usuario responde como *not found* (404), no 403.

### F3 — Anti-empalme correcto por tenant (P0)

- `isOverlapping` debe recibir `userId` y comparar solo contra citas de ese barbero.
- Las citas `CANCELLED` **no** bloquean horario.
- `findByTimeSlot` debe filtrar por `userId` salvo que se documente el uso global.
- Considerar bloqueo de concurrencia (dos requests simultáneas al mismo hueco): validar en transacción y, si es posible, restricción a nivel BD.

### F4 — Estatus de la cita (P0, simplificado)

Solo se persisten dos estatus (migración V8):

```
RESERVED ──► CANCELLED
```

- `RESERVED` (id 1): cita creada y vigente. `CANCELLED` (id 2): libera el horario en el anti-empalme.
- "En curso" y "terminada" **no se guardan**: se derivan de `startTime`/`endTime` y la hora actual del negocio (ver `app.timezone`). No hay job de estatus.
- Solo una cita `RESERVED` puede cancelarse o reagendarse (`Appointment.ensureReserved()`); en otro caso responde 409 (`InvalidStatusTransitionException`).
- Evolución futura (P1): acción manual del barbero para marcar asistencia (`FINALIZED`/`NO_SHOW`), que sí requeriría guardarse.

### F5 — Recordatorios al barbero (P0)

Aviso al barbero **5 minutos antes** de su próxima cita: *"En 5 minutos tienes cita con {cliente}"*.

- Job `@Scheduled` cada minuto: busca citas `RESERVED` con `startTime` dentro de los próximos *N* minutos y `reminder_sent_at IS NULL`; envía el aviso y marca `reminder_sent_at` (evita duplicados). Requiere migración Flyway (columna nueva en `appointment`).
- *N* configurable en `application.yaml` (por defecto `5`), sin números mágicos en el código.
- Se puentea con un puerto de salida `BarberNotificationPort` (hexagonal), para cambiar el canal sin tocar la lógica.
- **Canal sugerido para el MVP (costo cero):** notificación hacia el frontend abierto (SSE o Web Push). Alternativa gratuita: bot de Telegram del barbero. Decidir antes de implementar (ver sección 6).
- Las citas `CANCELLED` o reagendadas no deben disparar recordatorio (al reagendar se limpia `reminder_sent_at`).
- Solo el barbero dueño de la cita recibe el aviso. **Nunca** se notifica al cliente.

### F6 — Clientes por REST (P1)

`GET /api/v1/customers?name=` (búsqueda, hoy solo disponible vía tool de IA), `POST /api/v1/customers`, `GET /api/v1/customers/{id}`. Reutiliza `SearchCustomersUseCase` y `CreateCustomerUseCase`.

### F7 — Zona horaria del tenant (P1)

Campo `timezone` (IANA, p. ej. `America/Mexico_City`) en `User`. Lo usan el calendario (interpretación de `from`/`to`) y el job de recordatorios. Sin esto, los avisos fallan si el servidor corre en UTC.

### F8 — Disponibilidad básica (P1)

Horario laboral del barbero (días/horas) y endpoint de huecos libres de un día, para que el front resalte horas disponibles y evite crear citas fuera de horario.

### F9 — Datos de la cita (P1)

`serviceName`, `notes` y, opcionalmente, `price` en `Appointment` para mostrar contexto en el calendario ("Corte y barba — 45 min").

---

## 4. Fuera del MVP

- Notificaciones al cliente (SMS, WhatsApp, email).
- Reservas públicas hechas por el propio cliente.
- Pagos y cobros.
- Varios barberos/empleados dentro de un mismo tenant.
- Citas recurrentes.
- Sincronización con Google Calendar.

---

## 5. Cambios técnicos implicados

Respetando `AGENTS.md` (arquitectura hexagonal, inyección por constructor, `record` en REST, sin `Map` ni `?`):

| Capa | Cambio |
|---|---|
| `domain` | Reglas de transición de estatus; excepción `InvalidStatusTransitionException`; campos nuevos opcionales en `Appointment` |
| `application/port/in` | `QueryAppointmentsUseCase.findByRange(userId, from, to, statuses)` y `findById`; `FinalizeAppointmentUseCase` |
| `application/port/out` | `AppointmentRepositoryPort`: `isOverlapping` con `userId`, `findByRange`, `findDueForReminder`, `markReminderSent`, `findStartingBetween`; `BarberNotificationPort` |
| `application/service` | Servicios nuevos (`FinalizeAppointmentService`, `SendAppointmentRemindersService`, `UpdateAppointmentStatusesService`) |
| `infrastructure/adapter/in/rest` | `AppointmentController`, `CustomerController`, DTOs (`AppointmentResponse`, `CreateAppointmentRequest`, …) y `AppointmentRestMapper` |
| `infrastructure/adapter/in/scheduler` | Jobs de estatus y recordatorios (junto a los existentes) |
| `infrastructure/adapter/out` | Query por rango en `JpaRepository`; adaptador del canal de notificación |
| Migraciones Flyway | `V7+`: `reminder_sent_at`, `timezone`, campos de F9, índice `(user_id, start_time)` |
| Docs | Actualizar este documento y documentar endpoints con ejemplos al implementarlos |

---

## 6. Orden sugerido y preguntas abiertas

**Orden de implementación:**

1. F3 (corregir `userId` en overlap) — es un bug, bloquea todo lo demás.
2. F1 + F2 (REST de citas y calendario) — desbloquea al frontend.
3. F4 (estatus) — el calendario muestra información real.
4. F5 (recordatorios al barbero).
5. P1: F7, F6, F9, F8.

**Preguntas abiertas:**

- ¿Qué canal usará el recordatorio del barbero: SSE/Web Push en el front, Telegram u otro?
- ¿Zona horaria única (México) para el MVP o por tenant desde el inicio?
- ¿Se permite agendar citas en el pasado (registro retroactivo)?
