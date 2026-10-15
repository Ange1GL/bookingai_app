# Webhook de WhatsApp Cloud API

Endpoint público que Meta usa para verificar la URL (GET) y entregar mensajes y estados (POST).
Valida la firma, registra metadatos y entrega cada mensaje de texto al agente de IA para clientes (demo, ver
"Agente para clientes"). Plan completo en `.plans/whatsapp-customer-booking-flow.md`.

## Agente para clientes (demo)

Los clientes finales escriben al número de WhatsApp del negocio y el agente los guía; se reutiliza este mismo webhook
(Meta solo permite una URL por app).

```
POST webhook -> ReceiveWhatsAppEventService (log de metadatos)
             -> HandleCustomerMessageService -> CustomerAssistantPort (SpringAiCustomerAssistantAdapter + CustomerBookingTools)
             -> CustomerReplyPort (LoggingCustomerReplyAdapter: demo, solo log; sin envío a Meta todavía)
```

1. **Elegir negocio**: saludo → pide el negocio → el cliente puede dar el nombre del **negocio**
   (`findCompanyByName`) o el del **dueño** (`findCompanyByOwnerName`); ambas búsquedas son parciales y no distinguen
   mayúsculas. Sin resultados avisa que no está registrado; con varios (también varios dueños con el mismo nombre)
   muestra negocio + dueño y pide elegir; con uno lo confirma → `selectCompany(businessId)` (el `businessId` es el id
   del dueño, ver `company.md`). El negocio elegido se guarda en el servidor (`CustomerConversationPort`, en memoria,
   clave `phoneNumberId:from`, máx. 1000 conversaciones, se pierde al reiniciar).
2. **Reservar**: `getMyProfile` (identifica al cliente por su número y pide el nombre si es nuevo) → `listServices` →
   `bookMyAppointment`, que reutiliza `BookAppointmentUseCase` (crea/busca cliente por teléfono bajo el tenant del
   negocio y respeta la lista negra).

Seguridad del agente:
- `CustomerBookingTools` es un set aparte de `BookingTools` (que depende del JWT del administrador) y solo reserva:
  sin lista negra, sin buscar clientes ni cancelar/mover citas.
- El tenant (`ownerUserId`) sale del estado del servidor y el teléfono del cliente, de `from` (autenticado por la firma
  HMAC); llegan por `ToolContext`, nunca como parámetros que el modelo pueda inventar.
- La memoria del chat usa el prefijo `wa:` y no se cruza con la del asistente del administrador.
- El log de la respuesta (`LoggingCustomerReplyAdapter`) imprime el texto que se le enviaría al cliente; es temporal y
  se reemplaza por el envío real. El texto entrante nunca se loguea.

### Defensas del procesamiento (Parte C del plan)

`HandleCustomerMessageService.handle` se ejecuta **fuera del request** (`@Async`, executor `whatsappTaskExecutor` de
`WhatsAppAsyncConfig`: 2–4 hilos, cola de 100) y el webhook responde 200 a Meta de inmediato; si la cola se llena, el
envío se rechaza y se registra (el webhook sigue respondiendo 200). Orden de las defensas dentro del handler:

1. **Remitente válido**: sin `phoneNumberId` o `from` se ignora.
2. **Idempotencia** (`ProcessedMessagePort`): el `messageId` se registra de forma atómica; un reintento de Meta se
   ignora sin responder. Máx. 10 000 ids, retención 24 h.
3. **Límite por teléfono** (`CustomerRateLimitPort`): 10 mensajes por minuto (ventana fija). El primero por encima del
   límite recibe un aviso; el resto se descarta en silencio para no amplificar un abuso.
4. **Un turno a la vez por conversación**: locks por clave (64 franjas) evitan que dos mensajes seguidos del mismo
   cliente intercalen estado y memoria del chat.

Todo el estado de las defensas (conversación, ids vistos, contadores) está en memoria: se pierde al reiniciar y no se
comparte entre instancias; con más de una instancia hay que moverlo a Redis o a tablas con clave única.

Limitaciones conocidas del demo: no hay tope diario de gasto de IA por teléfono ni global; el `from` de WhatsApp
(p. ej. `5215512345678`) puede no coincidir con el formato en que el administrador guardó el teléfono del cliente.

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
