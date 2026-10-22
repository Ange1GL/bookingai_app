# Recordatorios push de citas

El dueño del negocio activa las notificaciones en su navegador y recibe, **5 minutos antes de cada cita**, una
notificación del sistema (*"En 5 min: Juan Pérez · Corte y barba · 4:30 p. m."*), con la web cerrada. Usa
**Web Push** (RFC 8030/8291/8292): el backend decide cuándo avisar y el servicio push del navegador
(Google/Apple/Mozilla/Microsoft) lo entrega al service worker. Plan completo: `.plans/appointment-push-reminders.md`.

## Activación del entorno (claves VAPID)

Sin claves el push queda **desactivado** (la app arranca igual, el job no hace nada y `GET /push/public-key`
responde `503`).

```bash
node scripts/generate-vapid-keys.mjs
```

Pega la salida en `backend/.env` (dev) o en Parameter Store como `SecureString` (qa), y referencia las variables en
`.aws/task-definition-backend.json` (con `ssm:GetParameters` para el rol de ejecución, como `API_OPENAI_KEY`):

| Variable | Qué es |
|---|---|
| `VAPID_PUBLIC_KEY` | Clave pública (el navegador la recibe en `/push/public-key`) |
| `VAPID_PRIVATE_KEY` | Clave privada: **secreta**. Si cambia, todas las suscripciones dejan de funcionar y los usuarios deben reactivar |
| `VAPID_SUBJECT` | Contacto para el servicio push: `mailto:...` o `https://...` |

## Endpoints (`/api/v1/push`, autenticados y con CSRF)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/public-key` | `{ "publicKey": "B..." }` para `pushManager.subscribe`. `503` si no hay claves |
| POST | `/subscriptions` | Registra el dispositivo. Cuerpo = `PushSubscription.toJSON()` del navegador |
| DELETE | `/subscriptions` | `{ "endpoint": "..." }`. Idempotente y acotado al usuario |
| POST | `/test` | Envía una notificación de prueba a los dispositivos del usuario → `{ "delivered": 1 }` |

```json
// POST /subscriptions → 201
{ "endpoint": "https://fcm.googleapis.com/fcm/send/...", "expirationTime": null,
  "keys": { "p256dh": "BN...", "auth": "ab..." } }
```

| Código | Cuándo |
|---|---|
| 400 | Falta un campo o el endpoint no es de un servicio push conocido |
| 503 | Push sin configurar en este entorno |

## Cómo funciona el aviso

1. `AppointmentReminderJob` corre cada `app.push.job-interval-seconds` (30 s).
2. `SendAppointmentRemindersService` pide `claimDue(ahora, ahora + lead-minutes)`.
3. `JdbcAppointmentReminderRepositoryAdapter` **reclama de forma atómica** (`UPDATE ... RETURNING` con
   `FOR UPDATE SKIP LOCKED`) las citas `RESERVED` de usuarios con algún dispositivo suscrito, que empiezan en la
   ventana y tienen `reminder_sent_at IS NULL`, y las marca como avisadas. Dos instancias nunca avisan la misma cita.
4. `PushNotifier` envía a **todos** los dispositivos del dueño (`WebPushSenderAdapter`, TTL 300 s, urgencia alta).
   `404/410` del servicio push → se borra esa suscripción; otros fallos solo se registran.

Reglas: reagendar una cita pone `reminder_sent_at` en `null` (se avisa de nuevo para el nuevo horario); cancelar la
saca de `RESERVED`; una cita creada con menos de 5 min de anticipación se avisa en el siguiente ciclo. **No hay
reintentos**: un recordatorio tardío ya no sirve. Espera ±1 minuto de precisión (intervalo del job + latencia del
servicio push).

## Seguridad

- **SSRF:** el servidor hace POST a cada `endpoint` suscrito, así que solo se aceptan `https`, sin usuario ni puerto
  distinto de 443, y cuyo host termine (en el límite de un punto) en `app.push.allowed-endpoint-hosts`.
- `p256dh`/`auth` son secretos del dispositivo: no se registran en logs; del proveedor solo se registra el código HTTP.
- El contenido viaja cifrado de extremo a extremo hacia el dispositivo; el servicio push solo ve el tamaño.
- Un dispositivo es de un solo usuario: si otro usuario se suscribe desde el mismo navegador, pasa a ese usuario.

## Base de datos (`V29__create_push_subscription.sql`)

- `push_subscription (id, user_id, endpoint UNIQUE, p256dh, auth, user_agent, created_at)`.
- `appointment.reminder_sent_at` + índice parcial `idx_appointment_reminder_due` (citas aún sin avisar).

## Configuración

```yaml
app:
  push:
    vapid-public-key: ${VAPID_PUBLIC_KEY:}
    vapid-private-key: ${VAPID_PRIVATE_KEY:}
    subject: ${VAPID_SUBJECT:}
    lead-minutes: 5
    job-interval-seconds: 30
    allowed-endpoint-hosts: [fcm.googleapis.com, push.services.mozilla.com, push.apple.com, notify.windows.com]
```

## Dependencias

`nl.martijndwars:web-push` 5.1.2 (cifrado aes128gcm y firma VAPID). Se excluye su `bcprov-jdk15on` 1.70 (sin
soporte) a favor de `bcprov-jdk18on` 1.86 (mismos paquetes) y se sube `jose4j` a 0.9.7.

## Límites conocidos

- Sin límite de frecuencia en `POST /test` (solo afecta a los dispositivos del propio usuario).
- Si el envío falla para todos los dispositivos, la cita ya quedó marcada como avisada y no se reintenta.
- Entrega verificada de extremo a extremo solo con el spike (`scripts/push-spike`); la ruta Java está probada
  contra un servicio push simulado (cifrado, firma VAPID y cabeceras), no contra Google/Apple reales.
