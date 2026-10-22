# Calendario de citas (`feature/appointments`)

Calendario de las citas del usuario (con alta manual, ver más abajo): mes y semana, mobile-first, con agenda del día y detalle en un drawer inferior. Calendario propio (Tailwind + PrimeNG), sin FullCalendar.

## Contrato con el backend
`GET /api/v1/appointments?from=&to=` (`AppointmentController.findByDateRange`) → `AppointmentResponse[]`.

`GET /api/v1/appointments/{id}` (`AppointmentController.findById`) → `AppointmentResponse`; 404 si la cita no existe o es de otro tenant. Cada `AppointmentResponse` incluye el catálogo de precio de la cita: `priceCatalogId`, `serviceLabel` (nombre del servicio) y `price`.
- Rango **semiabierto** `[from, to)`: `to` es la medianoche posterior al último día visible.
- `from`/`to` van como `YYYY-MM-DDTHH:mm:ss` en **hora local sin `Z`** (`toLocalIso`); nunca `toISOString()`, que convertiría a UTC y desplazaría el rango. La API trabaja con `LocalDateTime` en la zona del negocio (`app.timezone`).
- Una sola consulta por periodo visible; cambiar de día dentro del mismo periodo no vuelve a pedir datos.

## Estados
Persistidos: `RESERVED (1)` y `CANCELLED (2)`. "En curso" y "finalizada" no existen en BD: `displayStatus` los deriva de `startTime/endTime` y la hora actual.

## Estructura
```
feature/appointments/
  appointments.routes.ts
  models/{appointment.dto,calendar.model,status-style.model}.ts
  service/appointments.service.ts          # usa SKIP_LOADER: el calendario pinta skeletons
  utils/{calendar-date,calendar-format}.util.ts   # lógica de fechas pura y testeada
  page/calendar-page/
  components/{calendar-toolbar,month-grid,week-strip,day-agenda,appointment-card,appointment-detail}/
```

## Comportamiento móvil
Objetivos táctiles ≥ 44 px, swipe horizontal para cambiar de periodo, navegación inferior en el layout y detalle como `p-drawer` desde abajo. Al abrir el detalle se consulta `GET /api/v1/appointments/{id}` y se muestran el servicio y su precio (MXN); mientras carga hay skeletons y si falla se indica "No disponible". En `md+` el mes muestra chips de cita y la agenda del día queda en un panel lateral.

## Crear cita manualmente (respaldo cuando el asistente de AI no está disponible)
Botón "Nueva cita" en el calendario → `AppointmentFormDialogComponent` → `POST /api/v1/appointments` con `{ customerId, startTime, endTime, priceCatalogId }` (`AppointmentsService.create`). Respuesta: `AppointmentResponse`.
- Cliente: `p-autocomplete` con búsqueda en servidor (`GET /api/v1/customers`, nombre o teléfono, hasta 50 resultados con scroll); los clientes en lista negra aparecen deshabilitados. "¿Cliente nuevo? Registrarlo" abre `CustomerFormDialogComponent` (compartido en `shared/components/customer-form-dialog`, también usado por Clientes), crea el cliente (`POST /api/v1/customers`, idempotente por teléfono) y lo deja seleccionado.
- Servicio: `p-select` con `GET /api/v1/price-catalog`.
- Inicio/fin: cada uno es un `app-date-time-field` (`feature/appointments/components/date-time-field`): `p-datepicker` solo para el **día** + `p-inputmask` `99:99` para escribir la **hora** (`17:13`, cualquier minuto; el selector de hora de PrimeNG solo avanza por pasos y no es editable). El valor es `null` hasta que la hora está completa y es válida (nada de horas a medias ni `25:70`, que se marca con "Escribe una hora válida"). El inicio no puede estar en el pasado (`notBefore`). Al cambiar el inicio, el fin conserva la duración que ya tenía (`endFollowingStart`). Al abrir sugiere la siguiente hora completa (hoy) o las 09:00 del día seleccionado, con 60 min de duración; el fin debe ser posterior al inicio. Las fechas se envían con `toLocalIso`, sin segundos ni `Z`.
- Errores del backend (solape 409, cita en el pasado 400, cliente bloqueado 409) los muestra `errorInterceptor` como toast y el diálogo queda abierto. Al éxito se cierra, se muestra un toast y el calendario salta al día de la cita.
- `BookingLookupService` contiene las dos consultas de apoyo para respetar la regla feature → feature; PrimeNG está en español (`core/config/primeng-es.ts`).

## Cancelar cita
El drawer de detalle muestra "Cancelar cita" solo si la cita está reservada o en curso (no si ya finalizó o está cancelada). El botón abre `ConfirmDialogComponent` (severidad danger, con cliente, día y hora) y solo al confirmar llama a `POST /api/v1/appointments/{id}/cancel` (`AppointmentsService.cancel`), que devuelve la cita actualizada. Al éxito se cierran el modal y el drawer, se muestra un toast y se recarga el periodo visible (la cita pasa a "cancelada", no desaparece). Si el backend rechaza la acción, el toast del interceptor explica el motivo y el modal queda abierto.

## Fuera de alcance
Arrastrar y soltar, vista por horas.
