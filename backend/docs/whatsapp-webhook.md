# Webhook de WhatsApp Cloud API

Endpoint público que Meta usa para verificar la URL (GET) y entregar mensajes y estados (POST).
Fase 1: valida y registra metadatos. El procesamiento con el agente de IA es la Fase 2 (ver `plan-whatsapp-webhook.md`).

## Endpoints

Base: `/api/v1/webhooks/whatsapp` (constante `WhatsAppWebhookController.WEBHOOK_PATH`).

| Método | Qué hace | Respuestas |
|---|---|---|
| `GET ?hub.mode=subscribe&hub.verify_token=…&hub.challenge=…` | Verificación al pulsar "Verificar y guardar" en Meta | `200 text/plain` con el challenge; `403` si el modo o el token no coinciden |
| `POST` + `X-Hub-Signature-256: sha256=<hmac>` | Evento (mensaje o estado) | `401` si falta o no coincide la firma; `200` vacío en cualquier otro caso (incluido JSON ilegible, para que Meta no reintente) |

Ejemplo:

```bash
curl "http://localhost:8082/api/v1/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=<TOKEN>&hub.challenge=123"
# -> 123
```

## Seguridad

- Ruta en `permitAll` y excluida de CSRF en `SecurityConfig` (Meta no es un navegador y no puede enviar el token CSRF). Sin esa exclusión el POST daría 403 en `qa`.
- Autenticación propia: token de verificación (GET) y HMAC-SHA256 del **cuerpo crudo en bytes** con el App Secret (POST), ambos comparados en tiempo constante (`WebhookSignatureVerifier`). No se valida por IP: Meta no publica una lista estable.
- El cuerpo nunca se loguea; del mensaje solo se registra id, tipo y el teléfono enmascarado (datos personales).
- Sigue pasando por `RateLimitFilter` (60 req/min por IP real).

## Configuración

| Variable | Propiedad | Origen |
|---|---|---|
| `WHATSAPP_VERIFY_TOKEN` | `whatsapp.webhook.verify-token` | Cadena inventada por nosotros; la misma se pega en el panel de Meta |
| `WHATSAPP_APP_SECRET` | `whatsapp.webhook.app-secret` | Meta → Configuración de la app → Básica |

Sin valor por defecto: si faltan o están vacías, la app no arranca. En dev van en `.env`; en QA, en Parameter Store (SecureString) inyectadas como `secrets` de la task definition de ECS. Nunca en el repo ni en la imagen.

Generar el token de verificación (32 bytes aleatorios en hex):

```bash
openssl rand -hex 32
```

## Infraestructura (AWS)

- API Gateway: la ruta debe aceptar `GET` y `POST` hacia el VPC Link (`ANY /api/{proxy+}` ya lo cubre).
- CloudFront: métodos GET/POST permitidos, caché desactivada (o reenviando query strings, si no `hub.challenge` no llega) y política de origen que reenvíe `X-Hub-Signature-256` y el cuerpo sin modificar.
- Meta: URL `https://<dominio-cloudfront>/api/v1/webhooks/whatsapp`, mismo token, y suscripción al campo `messages`. Con la app en modo Desarrollo solo llegan eventos de números de prueba y administradores.

## Pruebas

`WebhookSignatureVerifierTest` y `WhatsAppWebhookControllerTest` (MockMvc standalone). La exclusión de CSRF/permitAll de `SecurityConfig` no está cubierta por tests automáticos: se comprueba con el `curl` anterior contra un entorno con perfil `qa`.
