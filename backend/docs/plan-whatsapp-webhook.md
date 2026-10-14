# Plan — Webhook de WhatsApp Cloud API (Meta)

Estado: **pendiente de aprobación** (no se ha editado código).
Fuente: `docs_meta_api_webhook.md` (guía de Meta) + arquitectura actual del backend.

## 1. Alcance

**Fase 1 (este plan):** endpoint público que Meta puede verificar y que recibe eventos de forma segura (firma HMAC), responde 200 rápido y deja el evento listo para procesar.

**Fuera de alcance (Fase 2, plan aparte):** que el mensaje llegue al agente de IA y se responda por WhatsApp. Motivo: hoy `BookingTools` y el chat usan `CurrentUserPort` (usuario autenticado = tenant). El webhook no tiene usuario, así que antes hay que decidir cómo mapear `phone_number_id` / `wa_id` → tenant (probablemente una tabla `whatsapp_account` con migración Flyway) y cómo enviar respuestas (token de acceso de WhatsApp, cliente HTTP de salida).

## 2. Hallazgos de la arquitectura que afectan al diseño

| Tema | Situación actual | Consecuencia |
|---|---|---|
| `SecurityConfig` | `anyRequest().authenticated()`; permitAll solo para auth y `/actuator/health` | Añadir la ruta del webhook a `permitAll` |
| **CSRF** | `csrf.spa()` activo en `qa`/prod (solo `dev` lo desactiva) | **El POST de Meta daría 403** aunque esté en permitAll. Hay que añadir `ignoringRequestMatchers` para la ruta. En dev no se nota, en QA sí |
| `JwtAuthenticationFilter` | Sin token deja pasar la request | No requiere cambios |
| `RateLimitFilter` | 60 req/min por IP (IP real vía `trusted-proxy-hops=1`) | Meta envía desde pocas IPs; para una barbería es suficiente. Se deja activo (protege el GET de verificación). Si hubiera 429 de Meta, excluir el POST |
| CORS | Solo orígenes del frontend | Irrelevante (Meta no es navegador) |
| Prefijo de rutas | Todo vive bajo `/api/v1/...` | Usar `/api/v1/webhooks/whatsapp` (no `/webhook` como en la guía) para no depender de una ruta nueva en API Gateway/CloudFront |
| Configuración | `@ConfigurationProperties` + variables de entorno sin default; en QA vía Parameter Store (SecureString) | `@Value("${VERIFY_TOKEN}")` de la guía no se usa; se crea `WhatsAppProperties` |
| Arquitectura | Hexagonal: controller → port.in → service | Controller delgado; lógica en caso de uso; firma como componente de seguridad |

## 3. Endpoints

Base: `/api/v1/webhooks/whatsapp`

- `GET` — verificación. Params `hub.mode`, `hub.verify_token`, `hub.challenge`. Si `mode == subscribe` y el token coincide (comparación en tiempo constante) → `200 text/plain` con el challenge. Si no → `403`.
- `POST` — eventos. Header `X-Hub-Signature-256`. Valida HMAC-SHA256 sobre el **cuerpo crudo en bytes** con el App Secret. Firma ausente/inválida → `401` (sin detalle). Firma válida → parsea, delega al caso de uso y responde `200` vacío siempre (incluso si el payload es raro: un 5xx provoca reintentos de Meta durante días).

## 4. Cambios por archivo

Paquete base `com.github.angellariosacosta.bookingapp`.

### Nuevos
1. `infrastructure/config/WhatsAppProperties.java` — `@ConfigurationProperties("whatsapp.webhook")` `@Validated`: `verifyToken` y `appSecret` con `@NotBlank`, sin valor por defecto (la app no arranca si faltan, igual que `JWT_SECRET`).
2. `infrastructure/security/util/WebhookSignatureVerifier.java` — calcula HMAC-SHA256 con `byte[]`, compara con `MessageDigest.isEqual` sobre bytes, tolera header nulo/mal formado (devuelve `false`). Constantes para el prefijo `sha256=` y el algoritmo.
3. `infrastructure/adapter/in/rest/controller/WhatsAppWebhookController.java` — GET y POST como arriba. El POST recibe `@RequestBody byte[]` (no `String` ni objeto: el HMAC debe calcularse sobre los bytes exactos). Parsea con el `JsonMapper` (Jackson 3, `tools.jackson`) ya usado en el proyecto.
4. `infrastructure/adapter/in/rest/dto/WhatsAppWebhookPayload.java` — records anidados (`entry → changes → value → messages/statuses/contacts/metadata`) con `ignoreUnknown`, solo los campos que usamos.
5. `application/port/in/ReceiveWhatsAppEventUseCase.java` + `application/command/ReceiveWhatsAppEventCommand.java` (mensajes ya normalizados: `phoneNumberId`, `from`, `messageId`, `type`, `text`).
6. `application/service/ReceiveWhatsAppEventService.java` — Fase 1: registra el evento (log con `messageId`, `type` y teléfono enmascarado; **nunca el texto del cliente ni el cuerpo completo**) y descarta los `statuses`. Punto de enganche para la Fase 2.
7. Tests (ver §6).
8. `docs/whatsapp-webhook.md` — documentación operativa (variables, URL a pegar en Meta, configuración AWS, troubleshooting).

### Modificados
9. `infrastructure/config/SecurityConfig.java`
   - `permitAll`: añadir `"/api/v1/webhooks/whatsapp"` (comentario: autenticado por token de verificación y firma HMAC, no por JWT).
   - CSRF: dentro de `csrf.spa()` aplicar `csrf.ignoringRequestMatchers("/api/v1/webhooks/whatsapp")`. Constante compartida para la ruta (una sola definición, no string repetido).
10. `src/main/resources/application.yaml` — bloque `whatsapp.webhook.verify-token: ${WHATSAPP_VERIFY_TOKEN}` y `app-secret: ${WHATSAPP_APP_SECRET}` (sin defaults). Registrar la clase en `@EnableConfigurationProperties` o `@Component` según el patrón de `CsrfProperties`.
11. `.env` local (no se commitea) y `.env.example` si existe — añadir las dos variables con valores de prueba.
12. `CLAUDE.md` (raíz) — añadir `backend/docs/whatsapp-webhook.md` a la lista de docs y una línea de convención. `docs/security-config.md` — actualizar la lista de rutas públicas y la excepción CSRF.

## 5. Lo que tienes que configurar tú en AWS / Meta (no es código)

1. **Secretos**: crear en Parameter Store (SecureString) `WHATSAPP_VERIFY_TOKEN` (cadena inventada, larga y aleatoria) y `WHATSAPP_APP_SECRET` (Meta → Configuración de la app → Básica) e inyectarlos como `secrets` en la task definition de ECS.
2. **API Gateway**: que la ruta `/api/v1/webhooks/whatsapp` llegue al VPC Link con métodos **GET y POST** (si usas `ANY /api/{proxy+}` ya está cubierto).
3. **CloudFront** (el punto que más suele romper):
   - Comportamiento para `/api/*` con métodos permitidos GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE.
   - **Caché desactivada** (CachingDisabled) o política que **reenvíe query strings**; si no, `hub.challenge` no llega y la verificación falla o se cachea.
   - Origin request policy que **reenvíe el header `X-Hub-Signature-256`** y el body sin modificar (p. ej. `AllViewerExceptHostHeader`).
4. **Meta**: URL de callback `https://<tu-dominio-cloudfront>/api/v1/webhooks/whatsapp`, token de verificación igual al del paso 1, y suscribirse al campo `messages`.
5. Con la app de Meta en modo Desarrollo solo llegan eventos de números de prueba/administradores.

## 6. Pruebas

- Unitarias de `WebhookSignatureVerifier`: firma válida, inválida, header nulo, sin prefijo `sha256=`, cuerpo alterado en un byte.
- `@WebMvcTest` / MockMvc del controller **con CSRF activo y sin JWT** (reproduce QA):
  - GET con token correcto → 200 y body = challenge (text/plain).
  - GET con token incorrecto / mode distinto → 403.
  - POST sin firma / firma mala → 401.
  - POST con firma válida y payload de mensaje → 200 y caso de uso invocado una vez.
  - POST con firma válida y payload de `statuses` → 200 sin invocar mensajes.
  - POST con firma válida y JSON malformado → 200 (sin reintento de Meta).
- Comprobar que otras rutas siguen exigiendo autenticación (el matcher no se ensancha por accidente).
- Verificación manual local: `curl "http://localhost:8082/api/v1/webhooks/whatsapp?hub.mode=subscribe&hub.verify_token=<token>&hub.challenge=123"` → `123`. Luego mismo curl contra la URL pública de CloudFront.

## 7. Riesgos y decisiones abiertas

1. **Ruta**: asumí `/api/v1/webhooks/whatsapp` por el prefijo `/api` ya ruteado en tu infraestructura. Dime si prefieres otra.
2. **Idempotencia**: Meta reintenta y puede duplicar entregas; en Fase 1 no hay efectos secundarios, pero en Fase 2 habrá que deduplicar por `wamid` (tabla + migración).
3. **Procesamiento asíncrono**: el límite es responder en pocos segundos. La llamada al LLM en Fase 2 debe ir en un executor/cola, no dentro del request del webhook.
4. **Rate limit en memoria** por instancia: sin cambios respecto al resto de la API.
5. **Logs**: el cuerpo contiene teléfonos y texto de clientes (datos personales). No se loguea el cuerpo; si se agrega a `docs/privacy-policy` del frontend hay que mantener veraz el texto (`feature/landing/data/privacy-policy.ts`) cuando WhatsApp empiece a procesar datos en Fase 2.

## 8. Orden de implementación

1. `WhatsAppProperties` + yaml + `.env`.
2. `WebhookSignatureVerifier` + tests.
3. DTOs, command, port, service.
4. Controller.
5. `SecurityConfig` (permitAll + CSRF).
6. Tests de MockMvc.
7. Docs (`whatsapp-webhook.md`, `security-config.md`, `CLAUDE.md`).
8. Commit convencional: `feat(whatsapp): add meta webhook endpoint`.
