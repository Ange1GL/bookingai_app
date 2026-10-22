# Plan: créditos diarios de IA por usuario (backend + frontend)

**Estado:** propuesto, pendiente de aprobación (no se ha editado código)
**Rama:** `feature_managment_credits_ai`
**Proveedor afectado:** DeepSeek (`deepseek-flash`) vía Spring AI 2.0.0-M6
**Patrón de referencia:** cuota de voz (`TranscribeVoiceService` + `JdbcVoiceUsageRepositoryAdapter`, `V19__create_voice_usage.sql`)

---

## 0. Qué se construye

Cada usuario tiene un **presupuesto diario de tokens** de DeepSeek para el Asistente de AI. Cada mensaje al asistente gasta tokens reales (los que reporta DeepSeek). Cuando se agota:

- el backend responde `429` y **no llama** a DeepSeek;
- el frontend muestra "usaste X de Y, se recarga a las 12:00 a. m. (en 5 h 12 min), inténtalo después" y ofrece **crear la cita manualmente**.

Además de la cuota por usuario hay un **tope global diario** que protege tus ~$2 de saldo aunque todos los usuarios agoten su cuota (igual que `monthly-limit` en voz).

## 1. Análisis del producto (para decidir si vale la pena la IA)

AglaCita agenda citas **conversando**: el dueño escribe/dicta al asistente (web) y los clientes finales reservan por WhatsApp. La IA no es decorativa: es la propuesta de valor. Pero para validar el MVP lo que importa es **medir si la usan**, por eso este plan también guarda el consumo por usuario y día (`request_count` + `tokens_used`): con eso verás cuántos mensajes hacen, cuánto cuesta cada uno y si el asistente se usa más que la UI manual. Si casi nadie lo usa, podrás recortar presupuesto con datos.

Lo que más tokens gasta (y por qué importa el tope por mensaje):
1. El **system prompt** de `AiConfig` (~400 tokens) se reenvía en **cada** llamada.
2. El **historial** (`MessageWindowChatMemory`, ventana por defecto de 20 mensajes) se reenvía completo en cada turno.
3. Cada **tool call** (buscar cliente, catálogo, fecha/hora) es una llamada extra al modelo con todo el contexto. Una cita nueva típica = 4–6 llamadas internas.

Por eso un "mensaje" del usuario no cuesta lo mismo siempre: medir **tokens reales** es más justo y más seguro que contar mensajes.

## 2. Decisiones (con mi recomendación)

| # | Tema | Recomendación | Alternativa |
|---|---|---|---|
| D1 | Unidad de cuota | **Tokens totales** (prompt + completion) reportados por DeepSeek | Contar mensajes (más simple, pero un mensaje con 6 tool calls cuesta 6× uno simple) |
| D2 | Cuándo se recarga | **Día calendario en `app.timezone`** (medianoche de Ciudad de México), igual que voz | Ventana móvil de 24 h desde el primer uso (más compleja: hay que guardar el timestamp de inicio) |
| D3 | Cuándo se cobra | **Pre-chequeo** (`usados < límite`) y **cobro real después** de la respuesta | Reservar un estimado antes (más exacto, pero bloquea al usuario con poco saldo) |
| D4 | Si DeepSeek falla | **No se cobra** (no hay uso conocido) | Cobrar un mínimo |
| D5 | Qué cuenta | Solo `/agent/chat` (dueño en la web) en la fase 1. WhatsApp en fase 3 | Incluir WhatsApp desde ya |
| D6 | `/agent/raw-chat` | **Cobrarlo o desactivarlo fuera de `dev`**. Hoy es un hueco: evade cualquier límite (ya estaba "pendiente de decidir" en `docs/assistant-chat.md`) | Dejarlo (anula la protección) |
| D7 | Voz | **Independiente** (usa OpenAI, otro presupuesto). La UI de créditos solo habla de IA de texto | Unificar en un solo medidor |

Sobrecosto aceptado en D3: si dos mensajes del mismo usuario van en paralelo, ambos pasan el pre-chequeo y el gasto puede exceder el límite por **un mensaje**. Está acotado por `max-tokens` por respuesta (sección 3.4) y es irrelevante frente al tope global.

### Valores iniciales sugeridos (configurables en `application.yaml`)

```yaml
assistant:
  credits:
    enabled: true
    daily-token-limit-per-user: 60000   # ≈ 10–15 mensajes con tools; ajustar con datos reales
    global-daily-token-limit: 400000    # tope duro de gasto; ≈ 6–7 usuarios activos/día
    max-output-tokens: 600              # tope por respuesta del modelo
```

**Cómo calibrar con tus $2:** `tokens_totales_disponibles ≈ saldo_USD / precio_por_millón × 1_000_000`. Revisa el precio vigente de `deepseek-flash` en la página de DeepSeek (no lo fijo aquí) y reparte entre los días que quieras cubrir: `global-daily-token-limit ≈ tokens_totales / días_de_prueba`. Mientras no recargues, el saldo prepago de DeepSeek ya es el techo absoluto (mismo principio que en OpenAI: sin recarga automática).

## 3. BACKEND

### 3.1 Migración — `V28__create_ai_usage.sql`

```sql
create table ai_usage_daily (
    user_id bigint not null,
    usage_date date not null,
    tokens_used bigint not null default 0 check (tokens_used >= 0),
    request_count integer not null default 0 check (request_count >= 0),
    primary key (user_id, usage_date),
    constraint fk_ai_usage_daily_user foreign key (user_id) references user_app
);

create table ai_usage_global_daily (
    usage_date date primary key,
    tokens_used bigint not null default 0 check (tokens_used >= 0)
);
```

(Se verifica primero que V28 siga libre.) Sin entidades JPA: SQL nativo con `JdbcClient`, igual que voz.

### 3.2 Dominio

- `domain/exception/AiQuotaExceededException` (mensajes: usuario → "Agotaste tus créditos de IA de hoy"; global → "El asistente alcanzó su límite de hoy").
- `domain/model/AiCredits` (record puro): `dailyLimit`, `used`, `remaining()`, `resetsAt` (`Instant`/`OffsetDateTime`). La regla "agotado" vive aquí.

### 3.3 Aplicación

- `port/out/AiUsageRepositoryPort`: `int/long usedToday(userId, day)`, `long globalUsedToday(day)`, `void addUsage(userId, day, tokens)` (upsert que suma a ambas tablas en una transacción corta), `boolean hasRemaining(...)` si conviene.
- `port/out/AssistantChatPort`: `AssistantReply chat(userId, sessionId, message)` → `AssistantReply(text, tokensUsed)`. La implementación Spring AI (infra) sustituye la lógica que hoy está en `AgentController`.
- `port/in/ChatWithAssistantUseCase` + `service/ChatWithAssistantService`:
  1. pre-chequeo usuario y global → `AiQuotaExceededException`;
  2. llama `AssistantChatPort`;
  3. `addUsage(tokensUsed)`;
  4. devuelve `ChatWithAssistantResult(reply, AiCredits)`.
  Sin `@Transactional` alrededor de la llamada HTTP (misma razón que `TranscribeVoiceService`); solo el `addUsage` es transaccional.
- `port/in/GetAiCreditsUseCase` + `service/GetAiCreditsService` → `AiCredits`.
- `AiCreditsPolicy` (record) construida desde `AiCreditsProperties` en `AiCreditsConfig`, igual que `VoiceTranscriptionPolicy`/`VoiceConfig`.
- `Clock` inyectado + `app.timezone` para el día de negocio y `resetsAt` (inicio del día siguiente).

### 3.4 Infraestructura

- `adapter/out/persistence/adapter/JdbcAiUsageRepositoryAdapter` — `INSERT ... ON CONFLICT DO UPDATE SET tokens_used = tokens_used + :tokens` (usuario y global).
- `adapter/out/ai/SpringAiAssistantChatAdapter` — usa `chatClient.prompt()...call().chatResponse()` en lugar de `.content()` y lee `response.getMetadata().getUsage().getTotalTokens()`. Mantiene `ChatMemory.CONVERSATION_ID = userId:sessionId`.
- **Riesgo técnico a verificar primero (spike de 30 min):** con tool calling, Spring AI puede devolver el `usage` solo de la **última** llamada interna y no la suma de todas. Si pasa, subestimaríamos el gasto. Verificación: loguear el usage de un mensaje con tools y comparar contra el panel de uso de DeepSeek; si no suma, se acumula con un `CallAdvisor`/`ChatModel` wrapper que sume el `Usage` de cada llamada al modelo. **No avanzar al resto del backend hasta confirmarlo.**
- `max-output-tokens` → `spring.ai.deepseek.chat.options.max-tokens` (acota el peor caso por llamada). Considerar también reducir la ventana de memoria (`MessageWindowChatMemory.builder().maxMessages(...)`), que es la palanca más grande de ahorro.
- `AgentController`: delega en `ChatWithAssistantUseCase`; nuevo `GET /api/v1/agent/credits`; `raw-chat` según D6. `AssistantRestMapper` arma los DTOs (regla: nada de construir records inline en el controller).
- `GlobalExceptionHandler`: `@ExceptionHandler(AiQuotaExceededException)` → `429` con `ErrorResponse.of`.
- WhatsApp (fase 3): `HandleCustomerMessageService` ya tiene rate limit; sumar el gasto a la cuota del **dueño** una vez elegida la empresa (`CompanySelection.ownerUserId`). Antes de elegir negocio no hay dueño: se queda con el rate limit actual y el tope global.

### 3.5 Contrato de API

`GET /api/v1/agent/credits` → `200`:

```json
{ "dailyLimit": 60000, "used": 25400, "remaining": 34600, "percentUsed": 42,
  "resetsAt": "2026-10-23T00:00:00-06:00" }
```

`POST /api/v1/agent/chat` → `200` (se agrega `credits`, el resto igual):

```json
{ "reply": "Listo, agendé...", "credits": { "dailyLimit": 60000, "used": 28100, "remaining": 31900, "percentUsed": 47, "resetsAt": "..." } }
```

`429` en `/chat` cuando está agotado (cuerpo `ErrorResponse` estándar). El frontend, al recibir 429, consulta `/credits` para saber `resetsAt` — así no se cambia el contrato de `ErrorResponse`.

### 3.6 Pruebas backend

- `ChatWithAssistantServiceTest`: agotado no llama al proveedor; cobra tokens reales; proveedor falla → no cobra; límite global.
- `AiCreditsPolicy`/`AiCredits`: `remaining`, `resetsAt` cruzando medianoche y zona horaria (con `Clock` fijo).
- Adaptador JDBC contra PostgreSQL (como los tests de persistencia existentes): upsert suma, concurrencia no pierde cuenta.
- `GlobalExceptionHandler` → 429.

### 3.7 Documentación (obligatoria por tus reglas)

`backend/docs/ai-credits.md` (nuevo, calcado de `voice-transcription.md`), `backend/AGENTS.md`/`README` (nueva sección de config), variables en `.env.example` si aplica.

## 4. FRONTEND (`feature/assistant`)

Sigue `frontend/AGENTS.md`: `inject()`, signals, OnPush, control flow nativo, Tailwind + PrimeNG, sin `any`.

### 4.1 Modelos y servicio

- `models/credits.model.ts`: `CreditsDto` (`dailyLimit`, `used`, `remaining`, `percentUsed`, `resetsAt: string`) y `CreditsLevel = 'ok' | 'low' | 'exhausted'`.
- `models/credits.const.ts`: `LOW_CREDITS_THRESHOLD_PERCENT = 80`, `TOKENS_PER_CREDIT = 1000` (la UI habla de "créditos", no de tokens crudos), `CREDITS_REFRESH_MS`.
- `AgentService.credits()` → `GET /agent/credits`; `chat()` pasa a devolver `{ reply, credits }`. Mantiene `SKIP_LOADER`/`SKIP_ERROR_TOAST`.

### 4.2 Estado — `ChatStore`

- `credits` (signal solo lectura) + `creditsLevel` (`computed`) + `exhausted` (`computed`).
- Carga inicial al abrir la página; se actualiza con el `credits` de cada respuesta de `/chat`.
- Si `/chat` responde **429**: el mensaje queda en estado de error **sin ofrecer "Reintentar"** (igual que voz con 4xx), se consulta `/credits` y `exhausted` pasa a `true`.
- `TimeUntilResetPipe`/`computed` + un `interval` (cada 30–60 s, limpiado con `takeUntilDestroyed`) para la cuenta regresiva; al llegar a `resetsAt` vuelve a pedir `/credits`.

### 4.3 Componentes

- **`credits-meter`** (nuevo, presentación): barra `p-progressbar` + texto *"Usaste 25 de 60 créditos hoy · Se recarga a las 12:00 a. m. (en 5 h 12 min)"*. Colores: verde < 80 %, ámbar ≥ 80 %, rojo agotado. Con `role="progressbar"` y `aria-valuenow` (accesibilidad). Se muestra en la cabecera del asistente (versión compacta en móvil: solo barra + "34 restantes").
- **Estado agotado** en `assistant-page` / `chat-composer`: la caja de texto y el micrófono pasan a `disabled`, y aparece un aviso `role="alert"`: *"Agotaste tus créditos de hoy. Se recargan a las 12:00 a. m. Puedes volver mañana o crear la cita manualmente"* + botón **Crear la cita manualmente** (reutiliza `createManually()` y `appointment-links.ts`).
- **Aviso de pocos créditos** (≥ 80 %): línea discreta en el medidor, sin bloquear.
- El medidor es independiente del contador de dictados de `voice-input` (D7).

### 4.4 Pruebas frontend (Vitest)

- `agent.service`: `credits()` y nuevo shape de `chat()`.
- `chat.store`: actualiza créditos tras responder, 429 → agotado sin reintento, `creditsLevel`.
- `credits-meter`: textos y niveles (ok/low/exhausted), cuenta regresiva con timers falsos.
- `chat-composer`/`assistant-page`: deshabilitado y aviso al agotarse; botón manual navega.

### 4.5 Documentación

Actualizar `docs/assistant-chat.md` (elementos de UI, estados, estructura, pruebas) y `docs/frontend-structure.md` si cambia la estructura.

## 5. Orden de ejecución y commits

| Fase | Contenido | Commit |
|---|---|---|
| 0 | Spike: confirmar cómo reporta `usage` Spring AI con tool calls | — |
| 1 | Backend: migración, dominio, puertos/servicio, adaptadores, endpoint `/credits`, 429, tests, docs | `feat(ai): add daily token credits per user` |
| 2 | Frontend: modelos, servicio, store, `credits-meter`, estado agotado, tests, docs | `feat(assistant): show daily ai credits and exhausted state` |
| 3 (opcional) | Descontar WhatsApp al dueño; cerrar `raw-chat` (D6 puede entrar en fase 1) | `feat(whatsapp): charge ai credits to business owner` |

Fase 1 se puede probar sola con `curl`; fase 2 depende del contrato de la sección 3.5.

## 6. Fuera de alcance

Recarga automática del saldo en DeepSeek (la haces tú manualmente), cobro/planes de pago, créditos por rol (admin ilimitado), panel de administración de consumo, streaming de respuestas.
