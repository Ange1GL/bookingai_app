# Plan: recordatorios push de citas (web, sin app nativa)

**Estado:** propuesto, pendiente de aprobación (no se ha editado código)
**Rama sugerida:** `feature_push_reminders` (nueva, desde `main` una vez mergeada `feature_managment_credits_ai`)

---

## 0. Qué se construye

El dueño del negocio activa los recordatorios una vez en su celular. **5 minutos antes de cada cita** le llega una notificación del sistema operativo: *"En 5 min: Juan Pérez · Corte y barba · 4:30 p. m."*. Al tocarla se abre el calendario en esa cita. Funciona con la app cerrada y la pantalla bloqueada.

## 1. Tu pregunta: ¿el navegador puede forzar notificaciones al sistema operativo?

**Sí puede mostrarlas, pero no "forzarlas": el usuario siempre tiene que aceptar el permiso una vez.** Y hay una diferencia clave:

| Mecanismo | ¿Sirve para recordatorios? |
|---|---|
| `new Notification(...)` (Notification API sola) | **No.** Solo funciona mientras la página está abierta. Cerrada la pestaña, no hay nada que dispare la notificación. |
| Notificaciones programadas locales (Notification Triggers) | **No.** Chrome lo experimentó y lo abandonó; no existe en Safari. |
| **Web Push + Service Worker** | **Sí.** El servidor envía el aviso a través del servicio push del navegador (Google/Apple/Mozilla); el service worker lo recibe aunque la web esté cerrada y lo muestra como notificación nativa del sistema. Es lo que usan Gmail web, Slack, etc. |

Por eso la solución es **Web Push**: el *backend* decide cuándo avisar, no el navegador.

### Limitaciones reales por plataforma

| Plataforma | Qué se necesita | Riesgos |
|---|---|---|
| **Android (Chrome, Edge, Firefox)** | Solo aceptar el permiso. No hace falta instalar nada | El ahorro de batería puede retrasar mensajes; se mitiga con `Urgency: high` |
| **iPhone/iPad (iOS 16.4+)** | La web debe estar **instalada en la pantalla de inicio** (Compartir → "Añadir a pantalla de inicio"), tener manifest y service worker, HTTPS, y el permiso se pide **desde un botón** (no automático) | En una pestaña normal de Safari **no funciona**. Hay reportes de que a veces iOS deja de entregar; Safari revoca la suscripción si una push no muestra notificación visible |
| **Escritorio (Chrome/Edge/Firefox/Safari)** | Aceptar el permiso | El navegador debe estar abierto o en segundo plano (en Chrome/Edge basta con que el proceso exista) |

Fuente: [WebKit: Web Push para web apps en iOS y iPadOS](https://webkit.org/blog/13878/web-push-for-web-apps-on-ios-and-ipados/).

Consecuencias de diseño: la UI debe **detectar iOS sin instalar y mostrar las instrucciones** para añadirla a la pantalla de inicio, y el botón "Activar recordatorios" es obligatorio (gesto del usuario). Si más adelante haces la app nativa, las push nativas (FCM/APNs) reemplazan esto sin cambiar la lógica de "cuándo avisar".

## 2. Decisiones (con recomendación)

| # | Tema | Recomendación | Alternativa |
|---|---|---|---|
| D1 | Anticipación | **5 min fijo**, configurable en `application.yaml` (`app.reminders.lead-minutes`) | Ajuste por usuario (fase 3) |
| D2 | Envío desde Java | Librería `nl.martijndwars:web-push` + BouncyCastle (cifrado y VAPID ya resueltos) | Implementar RFC 8291/8292 a mano (innecesario) |
| D3 | Cuándo se dispara | Job cada 30 s en el backend que **reclama** las citas por vencer con un `UPDATE ... RETURNING` atómico | Programar un timer por cita en memoria (se pierde al reiniciar; no escala) |
| D4 | Idempotencia | Columna `appointment.reminder_sent_at`: nunca se avisa dos veces, aunque haya 2 tareas ECS o se reintente | Solo estado en memoria (se duplica al escalar) |
| D5 | Dispositivos | Varias suscripciones por usuario (celular + laptop) | Una sola |
| D6 | Respaldo por WhatsApp | **No** en esta entrega (las plantillas de WhatsApp tienen costo y reglas de ventana de 24 h) | Fase 3 si iOS resulta poco confiable |

## 3. BACKEND

### 3.1 Migración `V29__create_push_subscription.sql`

```sql
create table push_subscription (
    id bigserial primary key,
    user_id bigint not null,
    endpoint text not null,
    p256dh text not null,
    auth text not null,
    user_agent varchar(255),
    created_at timestamp not null,
    constraint fk_push_subscription_user foreign key (user_id) references user_app,
    constraint uk_push_subscription_endpoint unique (endpoint)
);
create index idx_push_subscription_user on push_subscription (user_id);

alter table appointment add column reminder_sent_at timestamp null;
create index idx_appointment_reminder_due on appointment (start_time) where reminder_sent_at is null;
```

(Se verifica el nombre real de la tabla de citas y que V29 siga libre. Los tokens `p256dh`/`auth` son secretos del dispositivo: no se loguean.)

### 3.2 Dominio y aplicación (hexagonal)

- `domain/model/PushSubscription` (userId, endpoint, p256dh, auth, userAgent).
- Puertos de entrada: `SubscribeToPushUseCase`, `UnsubscribeFromPushUseCase`, `GetPushPublicKeyUseCase`, `SendTestPushUseCase`, `SendAppointmentRemindersUseCase`.
- Puertos de salida: `PushSubscriptionRepositoryPort`, `WebPushSenderPort` (`send(subscription, payload)` → `SENT | GONE | FAILED`), y en `AppointmentRepositoryPort` un método `claimDueForReminder(from, to)` que marca `reminder_sent_at` y devuelve las citas en la misma sentencia.
- `SendAppointmentRemindersService`: toma las citas `RESERVED` con `start_time` entre *ahora* y *ahora + lead* que aún no se avisaron, arma el payload (cliente, servicio, hora) y lo envía a **todas** las suscripciones del dueño. Si el push responde **404/410** borra esa suscripción (el usuario la revocó). Un fallo de un dispositivo no frena al resto.
- Si una cita se **reagenda**, `reminder_sent_at` vuelve a `null` (en `updateTimeSlot`); si se **cancela**, ya no cumple `RESERVED` y no se avisa.
- Una cita creada con menos de 5 min de anticipación igual se avisa en el siguiente ciclo (la ventana es "falta ≤ 5 min y aún no empieza").

### 3.3 Infraestructura

- `adapter/in/scheduler/AppointmentReminderJob` (`@Scheduled(fixedDelay = 30s)`), mismo patrón que `ResetAccountLockedJob`; usa el `Clock` del negocio.
- `adapter/out/push/WebPushSenderAdapter` (librería web-push): `TTL` corto (5 min, un recordatorio viejo no sirve) y `Urgency: high`. Mensajes genéricos al cliente; el detalle del proveedor solo al log, sin endpoint ni claves.
- `adapter/out/persistence/...JdbcPushSubscriptionRepositoryAdapter` (SQL nativo o JPA siguiendo `AGENTS.md`).
- `PushProperties` (`app.push.vapid-public-key`, `vapid-private-key`, `subject`): **claves en variables de entorno** (`VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`), en QA desde Parameter Store como las demás. Nunca en el repo. Se generan una vez con un script Node (`scripts/generate-vapid-keys.mjs`, siguiendo tu regla de scripts multiplataforma).
- `PushController`: 
  - `GET /api/v1/push/public-key`
  - `POST /api/v1/push/subscriptions` (`endpoint`, `keys.p256dh`, `keys.auth`) — upsert por `endpoint`
  - `DELETE /api/v1/push/subscriptions` (por `endpoint`)
  - `POST /api/v1/push/test` — manda una notificación de prueba al usuario actual (con límite de frecuencia)
  Autenticados y con CSRF como el resto; DTOs `record`, errores por `GlobalExceptionHandler`.

### 3.4 Pruebas

`SendAppointmentRemindersServiceTest` (ventana, idempotencia, borra suscripciones 410, un dispositivo falla y otro recibe, sin suscripciones no hace nada), adaptador JDBC (el `claim` no devuelve dos veces la misma cita), controller, y migración con Flyway.

### 3.5 Despliegue (ojo)

El job corre en cada tarea de ECS; el `claim` atómico evita duplicados aunque haya más de una. Variables nuevas en `.aws/task-definition-backend.json` y permiso `ssm:GetParameters` para los dos parámetros VAPID.

## 4. FRONTEND (PWA + suscripción)

Usa `@angular/service-worker` (`SwPush`), compatible con Angular 21, siguiendo `frontend/AGENTS.md`.

### 4.1 Hacer la web instalable (hoy no lo es)

- `ng add @angular/service-worker` → `provideServiceWorker('ngsw-worker.js', { enabled: !isDevMode() })` y `ngsw-config.json`.
- `public/manifest.webmanifest` (nombre, `display: "standalone"`, colores del tema, `start_url`), iconos 192/512 y `apple-touch-icon`, `<link rel="manifest">` en `index.html`. **Sin esto iOS no permite push.**
- **Cache del service worker (CloudFront):** `frontend.yaml` sube todo con `immutable` salvo `index.html`. `ngsw-worker.js`, `ngsw.json` y `manifest.webmanifest` deben subirse con `Cache-Control: no-cache`, o los usuarios se quedan con un service worker viejo. Se ajusta el workflow.
- Nota: en `ng serve` el service worker está deshabilitado; se prueba con `ng build` + servidor estático o ya desplegado (HTTPS obligatorio fuera de `localhost`).

### 4.2 Feature `feature/notifications`

- `models/push.model.ts`: `PushSubscriptionRequestDto`, `PushStatus = 'unsupported' | 'needs-install' | 'blocked' | 'off' | 'on'`.
- `PushNotificationsService`: `status` (signal calculado con `SwPush.isEnabled`, `Notification.permission`, detección de iOS no instalado vía `display-mode: standalone`/`navigator.standalone`), `enable()` (pide la clave pública, `swPush.requestSubscription`, `POST /push/subscriptions`), `disable()` (`unsubscribe` + `DELETE`), `sendTest()`.
- `NotificationSettingsComponent` (tarjeta en el perfil/ajustes, o en la página de citas):
  - `off` → botón **Activar recordatorios** (gesto del usuario).
  - `needs-install` (iPhone en Safari) → pasos con "Compartir → Añadir a pantalla de inicio".
  - `blocked` → cómo reactivarlo en los ajustes del navegador (no se puede forzar).
  - `unsupported` → mensaje claro.
  - `on` → "Te avisaremos 5 min antes de cada cita" + botón **Enviar prueba** + **Desactivar**.
- Al tocar la notificación (`swPush.notificationClicks`) se navega a `/appointments` con la cita (se reutiliza el enrutado del calendario).
- Re-suscribir si `swPush.subscription` cambió (el navegador rota las suscripciones de vez en cuando).

### 4.3 Pruebas

Specs de `PushNotificationsService` (estados, enable/disable, errores) y del componente (cada estado y su acción), con `SwPush` mockeado.

## 5. Orden de ejecución y commits

| Fase | Contenido | Commit |
|---|---|---|
| 0 | **Spike:** generar claves VAPID y mandar una push de prueba a tu Android y a tu iPhone (instalada) con una página mínima. Confirma que el flujo funciona en *tus* dispositivos antes de construir | — |
| 1 | Backend: migración, suscripciones, envío, job, endpoints, tests, docs (`backend/docs/push-reminders.md`) | `feat(push): add appointment reminders via web push` |
| 2 | Frontend: PWA (manifest, service worker, cache en workflow), servicio, tarjeta de ajustes, tests, `docs/` | `feat(notifications): enable push reminders from the web app` |
| 3 (opcional) | Anticipación por usuario; aviso al dueño cuando un cliente reserva por WhatsApp (ya estaba en tu lista de mejoras, reutiliza todo); respaldo por WhatsApp si iOS falla | por separado |

## 6. Riesgos y qué verificar primero

1. **iOS:** es la plataforma más delicada. La fase 0 existe para probarlo antes de invertir; si falla en tu iPhone, el respaldo es WhatsApp (D6).
2. **Entrega no garantizada al segundo:** el job corre cada 30 s y el servicio push puede retrasar; espera ±1 min, no precisión exacta.
3. **Privacidad:** el nombre del cliente viaja cifrado de extremo a extremo hacia el dispositivo, pero pasa por Google/Apple/Mozilla como transporte. Si prefieres, el texto puede ser genérico ("Tienes una cita en 5 min") y el detalle se ve al abrir.
4. **Versión de la librería web-push y BouncyCastle:** se verifica la vigente al implementar (no la fijo aquí de memoria).

## 7. Fuera de alcance

App nativa (FCM/APNs), notificaciones por correo/SMS, recordatorios al **cliente final**, sonidos/acciones personalizadas en la notificación, resumen diario de la agenda.
