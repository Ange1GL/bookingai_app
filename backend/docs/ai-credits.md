# Créditos de AI del asistente (límite diario de tokens)

Cada usuario tiene un presupuesto **diario de tokens** de DeepSeek para el chat del asistente
(`POST /api/v1/agent/chat`). Cada mensaje gasta los tokens reales que reporta el proveedor. Al agotarse, el
backend responde `429` sin llamar a DeepSeek. Hay además un **tope global diario** que acota el gasto total.

Los créditos se reinician a la medianoche del día de negocio (`app.timezone`, no UTC), igual que la cuota de voz.
La voz (OpenAI) tiene su propio presupuesto: ver `voice-transcription.md`.

## Endpoints

`GET /api/v1/agent/credits` — autenticado. Respuesta `200`:

```json
{ "dailyLimit": 60000, "used": 25400, "remaining": 34600, "percentUsed": 42,
  "resetsAt": "2026-10-23T00:00:00-06:00" }
```

`POST /api/v1/agent/chat` — la respuesta incluye el saldo actualizado:

```json
{ "reply": "Listo, agendé...", "credits": { "dailyLimit": 60000, "used": 28100, "remaining": 31900,
  "percentUsed": 47, "resetsAt": "2026-10-23T00:00:00-06:00" } }
```

| Código | Cuándo | Mensaje |
|---|---|---|
| 429 | El usuario agotó sus créditos de hoy | "Agotaste tus créditos de AI de hoy" |
| 429 | Se alcanzó el tope global diario | "El asistente alcanzó su límite de uso de hoy" |
| 502 | DeepSeek falló (no se cobra nada) | "El asistente no está disponible..." |

El `429` no lleva `resetsAt` (se mantiene el contrato de `ErrorResponse`): el cliente consulta `/credits`.

## Cómo se cobra

1. **Pre-chequeo** (`AiCreditsTracker.assertAvailable`): se exige saldo del usuario y del global. El costo real
   solo se conoce después de responder.
2. **Llamada** al modelo (`SpringAiAssistantChatAdapter`). El `usage` de la respuesta ya es **acumulado**
   entre todas las llamadas internas del tool calling (`DeepSeekChatModel` usa
   `UsageCalculator.getCumulativeUsage`), así que un mensaje con 5 tool calls se cobra completo.
3. **Cobro** (`record`): suma los tokens al contador del usuario y al global en una transacción.

Consecuencias aceptadas: el último mensaje puede pasarse del límite (dos mensajes en paralelo, o uno caro);
está acotado por `max-tokens` por respuesta. Si DeepSeek falla no se cobra. `raw-chat` solo existe en el perfil
`dev` (`RawAgentController`) porque no pasa por los créditos.

## Arquitectura

| Pieza | Rol |
|---|---|
| `AgentController` | `/chat`, `/credits`; delega en los casos de uso |
| `ChatWithAssistantService` | Pre-chequeo → llamada → cobro |
| `GetAiCreditsService` | Saldo del día |
| `AiCreditsTracker` | Reglas compartidas (día de negocio, `resetsAt`, límites) |
| `AiCreditsPolicy` | Límites; la construye `AiCreditsConfig` desde `AiCreditsProperties` |
| `AssistantChatPort` → `SpringAiAssistantChatAdapter` | Llama al `ChatClient` y devuelve texto + tokens |
| `AiUsageRepositoryPort` → `JdbcAiUsageRepositoryAdapter` | Contadores con upsert atómico |

## Base de datos (`V28__create_ai_usage.sql`)

- `ai_usage_daily (user_id, usage_date, tokens_used, request_count)`: PK `(user_id, usage_date)`, FK a `user_app`.
  `request_count` sirve para medir uso real durante la validación del MVP.
- `ai_usage_global_daily (usage_date, tokens_used)`.

## Configuración

```yaml
assistant:
  credits:
    daily-token-limit-per-user: 150000   # 150 créditos en la UI
    global-daily-token-limit: 400000
spring.ai.deepseek.chat.options.max-tokens: 600
```

Calibrar: `tokens disponibles ≈ saldo_USD / precio_por_millón × 1_000_000` (precio vigente en la página de
DeepSeek) y `global-daily-token-limit ≈ tokens disponibles / días de prueba`. El saldo prepago de DeepSeek sigue
siendo el techo absoluto: no activar recarga automática.

Precios de `deepseek-flash` (oct. 2026, por millón de tokens): entrada sin caché $0.15 (pico $0.30), entrada con
caché $0.003, salida $0.60 (pico $1.20). Pico = lun-vie 01-04 y 06-10 UTC. Estimación (sin medir): una cita completa
gasta ~13,000 tokens (4-6 llamadas internas que reenvían prompt + historial), ≈ $0.004 en el peor caso. Con 150,000
por usuario y 400,000 globales el gasto máximo es ≈ $0.06/usuario/día y ≈ $0.16/día en total (≈ $4.7/mes).
Reemplazar esta estimación con datos reales del log DEBUG.

Para ver cuánto cuesta un mensaje: nivel `DEBUG` de `SpringAiAssistantChatAdapter` loguea `userId` y tokens.
