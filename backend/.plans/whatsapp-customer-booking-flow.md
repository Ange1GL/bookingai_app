# Plan: Company + agente de WhatsApp para clientes (demo)

## Hallazgo clave (condiciona todo el diseño)
`BookingTools` obtiene el tenant con `currentUserPort.getCurrentUserId()`, que lee el `SecurityContext` (JWT del admin).
En WhatsApp **no hay JWT**: quien escribe es un cliente final. Si reutilizamos `BookingTools`, falla (`authentication == null`)
y, peor, expone herramientas de administrador (lista negra, cancelar/mover citas de cualquiera, buscar clientes).
=> El flujo de clientes necesita **su propio set de tools** (`CustomerBookingTools`) y su propio `ChatClient`;
el `userId` del tenant sale del **estado de la conversación**, nunca del modelo ni del texto del cliente.

## Parte A — Company (dominio + persistencia)
1. `Company` (dominio): `id`, `name`, `userId` (dueño). Datos mínimos.
2. Migración `V20__create_company.sql`: tabla `company(id, name, user_id FK, created_at)`,
   `UNIQUE(user_id)` (1 compañía por usuario en el demo), índice por `lower(name)` para búsqueda.
3. Puertos/casos de uso (hexagonal, siguiendo el estilo existente):
   - `CreateCompanyUseCase` + `CreateCompanyCommand(name, userId)`; valida nombre no vacío y que el usuario no tenga ya compañía.
   - `FindCompanyUseCase`: `searchByName(String)` (parcial, case-insensitive) para el bot.
   - `CompanyRepositoryPort` (out) + adapter JPA + entity + mapper.
4. REST admin: `POST /api/v1/companies` (autenticado, `@CurrentUserId` asigna el dueño) y `GET /api/v1/companies/me`.
5. Docs: `backend/docs/company.md` y línea en CLAUDE.md.

## Parte B — Flujo del cliente por WhatsApp (demo mínimo)
**Endpoint:** no se crea otro. Se reutiliza `POST /api/v1/webhooks/whatsapp` (Meta solo permite una URL por app);
`ReceiveWhatsAppEventService` pasa de "solo log" a delegar en un nuevo caso de uso `HandleCustomerMessageUseCase`.
Se mantiene el log actual (metadatos, teléfono enmascarado).

**Estado de la conversación** (`WhatsAppConversationState`, en memoria para el demo, clave = teléfono del cliente):
`AWAITING_COMPANY` → `COMPANY_SELECTED(companyId, ownerUserId)`. Se guarda en servidor; el LLM no puede cambiarlo salvo vía tool.

**Flujo 1 – elegir negocio**
- Saludo → "Bienvenido a la app de Aglacitas, ingresa el nombre del negocio donde quieres agendar".
- Tool `findCompanyByName(name)`: devuelve coincidencias; si no hay, el bot responde que no está registrado y sugiere similares.
- Tool `selectCompany(companyId)`: fija el estado (valida que el id exista) → pasa a Flujo 2.

**Flujo 2 – reservar**
- Tools con `ownerUserId` tomado del estado (no parámetro del modelo):
  `listServices()`, `getAvailability/getCurrentDateTime`, `bookMyAppointment(startTime, endTime, priceCatalogId)`.
- El cliente se identifica **por el número de WhatsApp** (`from`, verificado por la firma HMAC): se reusa `BookAppointmentUseCase`
  (busca/crea cliente por teléfono bajo ese tenant y respeta lista negra). Si es nuevo, el bot pide solo el nombre.
- No se exponen: lista negra, buscar otros clientes, cancelar/mover citas ajenas (en esta fase solo reservar; cancelar "mis citas" después).

**IA**
- `AiConfig`: nuevo bean `customerChatClient` con `CUSTOMER_SYSTEM_PROMPT` (bienvenida, elegir negocio, reservar) y `defaultTools(customerBookingTools)`.
- `ChatMemory` con `CONVERSATION_ID = "wa:" + phoneNumberId + ":" + from`.
- El system prompt del admin no cambia.

**Envío de la respuesta**
- `WhatsAppSenderPort` + adapter HTTP a `https://graph.facebook.com/v{N}/{phone_number_id}/messages`
  con `SYSTEM_USER_EMPLOYEE_META` (nueva propiedad `whatsapp.cloud.system-user-token`, sin default, validada con `@NotBlank`).
- Si el token no está, el demo debe poder correr en modo "solo log de la respuesta" (flag) para probar sin enviar.

## Parte C — Seguridad mínima del demo (se amplía después)
- Firma HMAC ya validada (autentica a Meta y por tanto el `from`).
- `ownerUserId` nunca viene del modelo ni del texto; solo del estado de servidor.
- Idempotencia por `messageId` (Meta reintenta): cache en memoria de ids procesados.
- Procesar de forma asíncrona y responder 200 rápido a Meta (si el LLM tarda, Meta reintenta).
- Rate limit por teléfono (cuota simple) para no quemar créditos de IA.
- No loguear texto ni teléfono completo; token de Meta nunca en logs.
- Pendiente para después: persistir estado/memoria, ventana de 24 h de WhatsApp (plantillas fuera de ventana), spam, borrar datos.

## Orden de implementación sugerido (commits separados)
1. Company: migración + dominio + puertos + adapter + REST + tests + docs.
2. Estado de conversación + `HandleCustomerMessageUseCase` + `CustomerBookingTools` + `customerChatClient` (respuesta solo en log).
3. `WhatsAppSenderPort` + `SYSTEM_USER_EMPLOYEE_META` + docs.
4. Hardening (idempotencia, asíncrono, rate limit).

## Decisiones que necesito de ti
1. ¿Una compañía por usuario (UNIQUE user_id) en el demo, o varias?
2. ¿Aprobado reutilizar el mismo webhook (recomendado) en lugar de un endpoint nuevo?
3. ¿Estado de conversación en memoria (se pierde al reiniciar) o ya tabla en BD?
4. ¿Los clientes solo reservan en esta fase, o también ver/cancelar sus citas?
5. ¿Empezamos por la Parte A o por la B con una compañía sembrada por migración?
