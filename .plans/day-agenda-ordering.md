# Plan: orden de la agenda del día (`day-agenda`)

**Estado:** propuesto, pendiente de aprobación (no se ha editado código)
**Archivos:** `frontend/src/app/feature/appointments/components/day-agenda/` (y la página `calendar-page` que lo usa)
**Rama sugerida:** `feature_day_agenda_order` (nueva, desde `main`)

---

## 1. Cómo está hoy

- `calendar-page` agrupa las citas por día (`groupByDay`) y cada grupo queda **cronológico ascendente** (09:00 → 18:00). `day-agenda` solo pinta esa lista tal cual.
- El estado visible (`reserved` / `in-progress` / `finished` / `cancelled`) se **calcula una sola vez al cargar** (`displayStatus` con la hora de ese momento). Si dejas la pantalla abierta, una cita no pasa sola a "Finalizada".

Efecto en el uso real: a media tarde las citas ya terminadas están al principio y **la próxima cita queda enterrada en medio de la lista**; hay que buscarla.

## 2. Mi opinión sobre "las finalizadas arriba"

Entiendo el argumento de la trazabilidad, pero **yo no las pondría arriba**. Quien abre la agenda quiere saber *qué sigue*, no revisar lo que ya pasó. Si las finalizadas suben, cada cita que termina empuja a la próxima hacia abajo y al final del día la lista empieza con 8 citas viejas.

La trazabilidad no se pierde si las finalizadas **siguen visibles pero agrupadas abajo** (ordenadas de la más reciente a la más antigua, como pides). Propongo:

| # | Sección | Qué contiene | Orden dentro de la sección |
|---|---|---|---|
| 1 | **En curso** | La cita que está pasando ahora | — |
| 2 | **Próximas** | Reservadas que aún no empiezan | La más cercana primero |
| 3 | **Finalizadas** | Ya terminaron | **De la más reciente a la más antigua** (la que acaba de terminar, arriba) |
| 4 | **Canceladas** | Canceladas | Cronológico |

Reglas de borde:
- Un día **pasado** (todas finalizadas) o **futuro** (todas próximas) no muestra encabezados de sección: una sola lista, como hoy.
- "Finalizadas" aparece **colapsada** cuando hay citas en curso o próximas (muestra "Finalizadas (3)" y se expande con un clic), y **expandida** si no hay nada pendiente. Así la trazabilidad está a un clic y no estorba.
- Semana y mes **no cambian** (siguen cronológicas): son vistas de calendario, donde el orden por hora es lo esperado.

### Alternativas descartadas
- **Tu versión literal (finalizadas arriba, luego en curso, luego próximas):** funciona al inicio del día y empeora cada hora. Si aun así la prefieres, el cambio es solo el orden de las secciones (una constante).
- **Orden cronológico con scroll automático a la próxima cita y una línea "Ahora":** también válido; es menos código pero no resuelve que las finalizadas sigan ocupando la pantalla en un móvil.

## 3. Qué se construye

1. **`utils/day-agenda.util.ts`** (función pura, con tests): `buildAgendaSections(appointments, selectedDate, now)` → `AgendaSection[]` (`id`, `title`, `appointments`, `collapsible`, `collapsedByDefault`). Orden por rango de estado (en curso, próximas, finalizadas, canceladas) y, dentro de cada una, la regla de la tabla.
2. **`models/day-agenda.model.ts`**: `AgendaSection` y el tipo de sección.
3. **`day-agenda.component`**: reemplaza la lista plana por secciones (`@for`), con encabezado de sección + conteo; la de finalizadas es un botón con `aria-expanded` que muestra u oculta su lista. Estado de colapso en un `signal` por sección (se reinicia al cambiar de día).
4. **Estados al día con el reloj:** `calendar-page` hoy deriva el estado una vez. Se agrega un `now` (signal que se actualiza cada minuto, limpiado al destruir el componente) para que una cita pase sola de "Próxima" → "En curso" → "Finalizada" sin recargar. Hay que revisar cómo se arma `groups()` para derivar el estado desde ese `now` y no al cargar.
5. **Tarjeta:** `appointment-card` ya muestra el estado; las finalizadas pueden verse atenuadas (clase de opacidad) para distinguirlas de lo pendiente.

## 4. Pruebas y documentación

- `day-agenda.util.spec.ts`: orden por estado; próximas ascendente; finalizadas descendente; día pasado/futuro sin secciones; cita que empieza justo ahora; canceladas al final; lista vacía.
- `day-agenda.component.spec.ts`: secciones renderizadas, finalizadas colapsadas con pendientes y expandidas sin ellas, botón de expandir, vacío.
- Prueba en el navegador con tu sesión (crear citas de prueba o usar las existentes).
- Actualizar `docs/appointments-calendar.md` (orden y secciones de la agenda del día).

## 5. Orden de ejecución

| Paso | Contenido |
|---|---|
| 1 | Util + modelo + tests |
| 2 | `now` reactivo en `calendar-page` |
| 3 | Componente con secciones y colapso + tests |
| 4 | Probar en el navegador, docs y commit (`feat(appointments): group the day agenda by what is next`) |

## 6. Preguntas para ti

1. ¿Te late agrupar así (en curso → próximas → finalizadas abajo) o prefieres de verdad las finalizadas **arriba**?
2. ¿Finalizadas **colapsadas** cuando hay pendientes, o siempre visibles?
