# Recordatorios push en la web (`shared/components/push-notifications-card`)

Pide al usuario permiso para notificaciones y suscribe su dispositivo para recibir un aviso **5 minutos antes de cada
cita**, con la web cerrada. Backend: `backend/docs/push-reminders.md` (job, endpoints `/api/v1/push`, claves VAPID).

## Qué ve el usuario

La tarjeta **Recordatorios de citas** aparece en el inicio (`/home`) y cambia según el estado de ese dispositivo
(`PushStatus`, calculado por `PushNotificationsService.status`):

| Estado | Cuándo | Qué muestra |
|---|---|---|
| `off` | Soportado y sin suscribir | Botón **Activar recordatorios** (el permiso se pide al hacer clic: iOS lo exige) |
| `on` | Suscrito | "Activados en este dispositivo", **Enviar prueba** y **Desactivar** |
| `blocked` | El usuario negó el permiso | Cómo reactivarlo en los ajustes del navegador (no se puede forzar desde la web) |
| `needs-install` | iPhone/iPad en una pestaña de Safari | Pasos: Compartir → Añadir a pantalla de inicio → abrir desde el ícono |
| `unsupported` | Sin Web Push (navegador viejo, `ng serve`) | La tarjeta no se muestra |

## Cómo funciona

1. `enable()`: `GET /push/public-key` → `SwPush.requestSubscription` (aquí el navegador pide el permiso) →
   `POST /push/subscriptions` con `subscription.toJSON()`.
2. El backend envía la push cifrada; el service worker de Angular (`ngsw-worker.js`) la recibe y muestra la
   notificación aunque la web esté cerrada. Al tocarla enfoca o abre `/appointments`
   (`navigateLastFocusedOrOpen`, definido en el payload que arma `WebPushSenderAdapter`).
3. `syncExisting()` vuelve a registrar la suscripción una vez por sesión (el backend hace upsert por endpoint): así
   un dispositivo pasa al usuario que inicie sesión después.
4. `disable()`: `DELETE /push/subscriptions` + `SwPush.unsubscribe()`.

Los errores se muestran en la tarjeta (`SKIP_ERROR_TOAST`, `SKIP_LOADER`). Negar el permiso no muestra error: el
estado pasa a `blocked` y la tarjeta explica cómo deshacerlo.

## PWA (necesaria para iPhone)

- `public/manifest.webmanifest` + íconos en `public/icons/` (192, 512, maskable 512 y 180 para `apple-touch-icon`).
- `ngsw-config.json` y `provideServiceWorker('ngsw-worker.js', { enabled: !isDevMode() })` en `app.config.ts`.
  **El service worker solo existe en el build de producción**: `ng serve` no lo tiene, por eso allí la tarjeta no se
  muestra. Para probarlo: `ng build --configuration production` y servir `dist/diamond-ng/browser` con HTTPS
  (o `localhost`), o probar ya desplegado.
- `.github/workflows/frontend.yaml` sube `ngsw.json`, `ngsw-worker.js`, `safety-worker.js`, `worker-basic.min.js` y
  `manifest.webmanifest` con `Cache-Control: no-cache` (no llevan hash en el nombre; con `immutable` los navegadores
  se quedarían con un service worker viejo).

## Plataformas

| Plataforma | Requisito |
|---|---|
| Android (Chrome/Edge/Firefox) | Solo aceptar el permiso |
| iPhone/iPad (iOS 16.4+) | Instalar la web en la pantalla de inicio y activar desde el ícono; el permiso se pide con un clic |
| Escritorio | Aceptar el permiso; el navegador debe estar abierto o en segundo plano |

## Estructura

```
core/model/push.model.ts                 # PushStatus, DTOs, REMINDER_LEAD_MINUTES (solo para textos)
core/util/pwa.util.ts                    # isIos, isStandalone, supportsPush + PWA_ENVIRONMENT (para pruebas)
core/service/push-notifications.service.ts
shared/components/push-notifications-card/
```

## Pruebas

`push-notifications.service` (estados, enable/disable/test/sync, permiso negado, backend sin configurar) y
`push-notifications-card` (cada estado y su acción, errores). `SwPush` y las capacidades del navegador se simulan.
