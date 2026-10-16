# AglaCita — Gestión de citas con inteligencia artificial

> Documento descriptivo del producto y análisis de la capa de aplicación (`application/port/in` y `application/service`) del backend. Pensado como base para presentar la app, alinear al equipo y priorizar mejoras.

## 1. ¿Qué es AglaCita?

AglaCita es una aplicación de **gestión de citas para pequeños negocios y profesionales independientes** (barberías, estéticas, consultorios, servicios a domicilio…) cuyo diferenciador es que **todo el agendado se hace conversando**, no llenando formularios.

Hoy es un **MVP** que se quiere validar con usuarios reales para saber si el producto les resulta útil.

### Dos formas de conversar con la IA

| Quién | Canal | Qué puede hacer |
|---|---|---|
| **Cliente final** (quien quiere el corte, la consulta, etc.) | WhatsApp | Reservar una cita escribiendo en lenguaje natural. El asistente detecta si el horario ya está ocupado o si el cliente no puede reservar. |
| **Dueño del negocio** (el usuario de la app) | Chat de texto en la web (“Asistente de AI”) y **dictado por voz** | Agendar, cancelar y consultar citas hablando o escribiendo: *“cancela la cita de Juan de mañana a las 4”*. |

La idea detrás de la voz: para quien atiende su negocio todo el día es mucho más rápido **decirle al teléfono** “cancela esta cita” o “agrega una cita” que navegar por pantallas.

### Conceptos del dominio

- **Usuario (tenant):** el dueño. Cada usuario ve y modifica únicamente sus propios datos (todo está acotado por `userId`).
- **Negocio (`Company`):** relación uno a uno con el usuario. Existe porque una persona puede tener su negocio personal y registrar cómo se llama. El nombre del negocio (o del dueño) es lo que usa un cliente por WhatsApp para decir con quién quiere agendar.
- **Cliente (`Customer`):** persona que reserva. Se identifica por teléfono dentro de cada tenant. Puede estar en **lista negra**.
- **Catálogo de precios (`PriceCatalog`):** los servicios que ofrece el negocio. Toda cita referencia un servicio del catálogo.
- **Cita (`Appointment`):** un cliente + un servicio + un rango horario. Estados principales: reservada y cancelada.

### Stack

- **Backend:** Spring Boot, Maven, arquitectura hexagonal (puertos y adaptadores).
- **Frontend:** Angular 21, PrimeNG 21, Tailwind 4.
- **IA:** DeepSeek (agentes de texto) + proveedor de speech-to-text para el dictado.
- **Mensajería:** WhatsApp Cloud API (webhook firmado con HMAC-SHA256, un único número).
- **Infra:** AWS (ECS para backend, S3 + CloudFront para frontend).

---

## 2. Mapa de casos de uso (`application/port/in`)

El paquete contiene **25 puertos de entrada**: cada interfaz es un caso de uso con un único método, que es lo que invocan los controladores REST, el webhook de WhatsApp y las herramientas (*tools*) de los agentes de IA. Los agrupo por responsabilidad:

### 2.1 Citas (corazón del negocio)

| Puerto | Método | Qué hace |
|---|---|---|
| `BookAppointmentUseCase` | `book(cmd)` | Reserva **por teléfono**: si el cliente no existe lo crea al vuelo. Es la vía natural del chat/WhatsApp. |
| `CreateAppointmentUseCase` | `create(cmd)` | Crea la cita para un **cliente ya existente** (por `customerId`). Es la vía de la API REST/UI. |
| `CancelAppointmentUseCase` | `cancel(cmd)` | Cancela una cita en estado reservada. |
| `RescheduleAppointmentUseCase` | `reschedule(cmd)` | Cambia el horario validando rango, que no sea en el pasado y que no choque con otras citas (excluyéndose a sí misma). |
| `QueryAppointmentsUseCase` | `findById`, `findByCustomer`, `findByTimeSlot`, `findByDateRange` | Consultas de lectura para el calendario y para que la IA “vea” la agenda. |

### 2.2 Clientes

| Puerto | Qué hace |
|---|---|
| `CreateCustomerUseCase` | Alta manual. |
| `SearchCustomersUseCase` | Búsqueda por nombre (autocompletado). |
| `ListCustomersUseCase` | Listado paginado con filtros `name`/`phone`. |
| `FindCustomerByPhoneUseCase` | Localiza al cliente por teléfono dentro del tenant. |
| `BlacklistCustomerUseCase` | Bloqueo directo (idempotente): impide reservar y cancela sus citas futuras. |
| `RemoveCustomerFromBlacklistUseCase` | Quita el bloqueo. |

### 2.3 Catálogo de precios

`CreatePriceCatalogUseCase`, `ListPriceCatalogUseCase`, `UpdatePriceCatalogUseCase`, `DeletePriceCatalogUseCase` — CRUD clásico, una interfaz por operación.

### 2.4 Negocio y selección de negocio (WhatsApp)

| Puerto | Qué hace |
|---|---|
| `CreateCompanyUseCase` | El dueño registra el nombre de su negocio. |
| `FindCompanyUseCase` | `findByOwner`, `searchByName`, `searchByOwnerName` (coincidencia parcial, máx. 10). Soporta el caso en que varios dueños comparten nombre y el cliente debe elegir. |
| `CompanySelectionUseCase` | Recuerda, **por conversación**, con qué negocio está hablando el cliente de WhatsApp (`select` / `current`). Como hay un solo número para todos los negocios, esto es lo que permite el multi-tenant en el canal. |

### 2.5 Canal WhatsApp

| Puerto | Qué hace |
|---|---|
| `ReceiveWhatsAppEventUseCase` | Recibe el evento ya validado del webhook y reparte cada mensaje. |
| `HandleCustomerMessageUseCase` | Procesa un mensaje: valida remitente, idempotencia, límite de frecuencia, serializa la conversación, consulta al agente y responde por la Cloud API. |

### 2.6 Voz

| Puerto | Qué hace |
|---|---|
| `TranscribeVoiceUseCase` | Valida el audio, consume cuota diaria/mensual y lo transcribe; el texto termina en la caja del chat del asistente. |

### 2.7 Identidad y sesión

`AuthenticateUserUseCase`, `RegisterUserCase`, `RefreshAccessTokenUseCase`, `LogoutUseCase`, `ChangeEmailUseCase`. Autenticación por cookies HttpOnly con refresh y CSRF doble envío; JWT HS256.

---

## 3. Análisis de los servicios (`application/service`)

### 3.1 Lo que está bien

1. **Arquitectura hexagonal bien aplicada.** Los servicios dependen solo de puertos (`*RepositoryPort`, `CustomerAssistantPort`, `CustomerReplyPort`, `SpeechToTextPort`…). Eso hace posible cambiar DeepSeek, el proveedor de voz o WhatsApp sin tocar la lógica.
2. **Un caso de uso = una interfaz = un servicio.** Alta cohesión, fácil de testear y de leer.
3. **Aislamiento por tenant en cada consulta.** Prácticamente todas las búsquedas reciben `userId` (`findById(id, userId)`), lo que evita fugas entre negocios.
4. **`CustomerBlacklistGuard` como único punto de control.** REST y herramientas de IA pasan por el mismo guard, así que no hay forma de “saltárselo” desde el chat.
5. **`Clock` inyectado.** Hace las reglas de “no en el pasado” testeables.
6. **`HandleCustomerMessageService` está muy bien pensado para un webhook:**
   - responde 200 a Meta y procesa `@Async`;
   - orden de defensas correcto: remitente válido → idempotencia por `messageId` → rate limit → *lock* por conversación;
   - *lock striping* (64 locks) para que dos mensajes seguidos del mismo cliente no se intercalen sin crear un lock por usuario;
   - si el asistente falla, el cliente recibe un mensaje amable en vez de silencio.
7. **Privacidad cuidada:** `PhoneMask`, no se loguea el texto de los clientes.
8. **`TranscribeVoiceService` protege el gasto:** valida formato **por firma binaria** (no solo por `Content-Type`), consume la cuota antes de llamar al proveedor, devuelve la cuota diaria si el proveedor falla, **no** devuelve la mensual (acota el presupuesto), y evita mantener una transacción abierta durante la llamada HTTP.
9. **Lista negra coherente:** bloquear cancela las citas futuras, por eso `RescheduleAppointmentService` puede omitir el guard (y lo deja documentado).

### 3.2 Hallazgos y oportunidades de mejora

Ordenados por impacto aproximado.

#### Alto

1. **Posible condición de carrera al reservar (`Book`/`Create`/`Reschedule`).**
   El patrón es *“consultar `isOverlapping` → guardar”*. Dos solicitudes simultáneas (por ejemplo, dos clientes de WhatsApp para la misma hora del mismo negocio, ya que los locks son por conversación y no por negocio) pueden pasar la validación y crear **dos citas traslapadas**.
   *Sugerencia:* añadir una garantía a nivel de base de datos (restricción de exclusión sobre el rango, p. ej. `EXCLUDE USING gist` en PostgreSQL) o un bloqueo pesimista por `userId` durante el agendado; traducir la violación a `AppointmentOverlapException`.

2. **Estado en memoria (idempotencia, rate limit, locks de conversación, selección de negocio).**
   Está documentado como decisión de MVP, pero en cuanto ECS corra **más de una tarea** o haya un despliegue en caliente: se pierde la idempotencia (Meta reintenta → doble respuesta/doble reserva), el rate limit se multiplica por N y los locks dejan de serializar.
   *Sugerencia:* mientras sea una sola tarea, dejarlo y **anotarlo como restricción explícita de despliegue** (`desired count = 1`); después mover a Redis/DynamoDB o a una tabla con índice único para `messageId`.

3. **Creación implícita de clientes en `BookAppointmentService`.**
   `findByPhone(...).orElseGet(save(...))` no es atómico: dos mensajes simultáneos de un número nuevo pueden crear dos clientes (o fallar por índice único). Además el nombre lo da el LLM: si el cliente existe, nunca se actualiza; si no, se guarda lo que la IA haya inferido.
   *Sugerencia:* índice único `(user_id, phone)` + manejo de la violación con reintento de lectura; validar/normalizar el teléfono (formato E.164) en la frontera.

#### Medio

4. **Duplicación entre `BookAppointmentService` y `CreateAppointmentService`.**
   Comparten ~80 % del flujo (guard de lista negra, solapamiento, catálogo, `Appointment.createNew`, guardar). La única diferencia real es **cómo se resuelve el cliente**. Es candidato a extraer un colaborador (`AppointmentScheduler`/`AppointmentBooker`) que reciba un `Customer` ya resuelto; cada caso de uso solo resolvería el cliente. Esto además evita el clásico “arreglé el bug en uno y no en el otro”. Hoy son dos copias; si aparece una tercera vía (p. ej. reserva desde enlace público) ya amerita extraerlo.

5. **Validaciones de horario no simétricas.**
   `Reschedule` valida explícitamente rango y “no en el pasado” (`validateTimeRange`, `validateNotInPast`). En `Book`/`Create` se delega en `Appointment.createNew`. Está bien **si** `createNew` aplica las mismas reglas; conviene confirmarlo con un test que lo fije, porque la IA puede proponer fechas absurdas (“ayer a las 25:00”).

6. **Reglas de negocio de agenda ausentes.**
   Hoy el único criterio es “no traslapar”. Faltan, para algo usable en producción: **horario laboral y días de descanso** del negocio, duración derivada del servicio del catálogo (¿la IA calcula `endTime` o lo hace el sistema?) y zona horaria por negocio. Sin eso, el agente de WhatsApp puede aceptar citas a las 3 a. m.
   *Sugerencia:* que `endTime` lo calcule el dominio a partir de la duración del servicio y no el LLM.

7. **Granularidad de puertos de entrada.**
   Una interfaz por operación es válido, pero en catálogo (4) y clientes (6) hay mucho ruido para el mismo agregado. Dos alternativas que mantienen la limpieza: agrupar por agregado (`PriceCatalogUseCases`) **o** dejarlo como está. No es urgente; solo decide un criterio y aplícalo parejo.

8. **Inconsistencias de nomenclatura/firmas.**
   - `RegisterUserCase` → debería ser `RegisterUserUseCase`.
   - Orden de parámetros mezclado: `find(phone, userId)` vs `findById(id, userId)` vs `search(name, userId)` vs `findByTimeSlot(userId, date, hour)`. Con dos `Long` seguidos es fácil cruzarlos sin que el compilador avise.
   - Auth recibe 5–6 `String` sueltos (`register(username, email, rawPassword, name, userAgent, ip)`); un *command* los haría legibles y menos propensos a errores de orden.
   - `QueryAppointmentsUseCase` acumula cuatro consultas: aceptable como puerto de lectura (CQRS ligero), pero es el que más crecerá (la IA pedirá “mis citas de la semana”).

9. **Cuota mensual de voz global.**
   `monthlyLimit` es “entre todos los usuarios”: un solo usuario intensivo puede agotarle el mes a los demás. Para un MVP es una defensa de presupuesto razonable, pero conviene un mensaje distinto al usuario y una métrica/alerta cuando se acerque al tope.

#### Bajo

10. **`CompanySelectionService.current` sin `@Transactional`** y `select` con `readOnly = true` aunque escribe en un puerto en memoria: funciona hoy porque no es JPA; si se migra a base de datos, el `readOnly` engañará.
11. **Cancelación sin motivo ni auditoría.** Para validar el MVP sería valioso saber *quién* canceló (cliente por WhatsApp, dueño, o bloqueo por lista negra) y cuándo.
12. **Cancelar/reagendar desde WhatsApp.** `CustomerBookingTools` es “solo reserva” (decisión consciente). Si los usuarios piden poder cancelar, el cliente tendrá que identificar su cita; ya existe `findByCustomer`, así que la base está.
13. **Notificaciones.** No hay recordatorios ni confirmación al dueño cuando un cliente reserva por WhatsApp. En validación de producto suele ser la funcionalidad más pedida (menos “no-shows”, que además es el dolor real de estos negocios).

---

## 4. Opinión general

La base técnica es **sólida para un MVP**: la separación de capas está limpia, la seguridad multi-tenant se respeta de forma consistente y los puntos delicados de integración (webhook idempotente, protección de gasto en IA, privacidad de datos) están pensados con más cuidado del habitual en una validación.

Los riesgos reales no están en el estilo del código sino en **tres huecos de robustez**: (1) integridad de agenda bajo concurrencia, (2) estado en memoria que ata el despliegue a una sola instancia y (3) reglas de agenda que hoy delegan demasiado en el LLM (duración, horarios, zona horaria). Resolver esos tres convierte el demo en algo que puedes ofrecer a un negocio sin miedo a que un cliente vea dos citas a la misma hora.

En cuanto a producto, la propuesta de valor más fuerte no es “agendar con IA” en sí, sino **“tu WhatsApp agenda por ti y tú gestionas hablando”**. Para validar con usuarios yo priorizaría: recordatorios/confirmaciones, horario laboral configurable y reducción de no-shows, por encima de añadir más canales.

## 5. Próximos pasos sugeridos

1. Restricción de no-traslape en BD + manejo de la excepción (impacto alto, bajo esfuerzo).
2. Documentar la restricción de “una sola instancia” hasta externalizar el estado.
3. Extraer el flujo común de `Book`/`Create`.
4. Modelar horario laboral y duración por servicio; que el dominio calcule `endTime`.
5. Unificar nomenclatura y orden de parámetros de los puertos (`RegisterUserCase`, `userId` siempre último o primero).
6. Recordatorios y notificación al dueño por WhatsApp.
