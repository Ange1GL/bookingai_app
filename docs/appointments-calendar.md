# Calendario de citas (`feature/appointments`)

Vista de solo lectura de las citas del usuario: mes y semana, mobile-first, con agenda del día y detalle en un drawer inferior. Calendario propio (Tailwind + PrimeNG), sin FullCalendar.

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
Objetivos táctiles ≥ 44 px, swipe horizontal para cambiar de periodo, navegación inferior en el layout y detalle como `p-drawer` desde abajo. En `md+` el mes muestra chips de cita y la agenda del día queda en un panel lateral.

## Fuera de alcance (fase 2)
Crear/cancelar citas desde la UI, arrastrar y soltar, vista por horas.
