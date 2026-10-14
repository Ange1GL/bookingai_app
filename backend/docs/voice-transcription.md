# Dictado por voz del asistente (transcripción)

El usuario graba hasta **30 s** en el chat del asistente; el backend transcribe el audio con OpenAI
**`gpt-4o-mini-transcribe`** (el modelo ligero) y devuelve el texto. El frontend lo deja en la caja del
chat para que el usuario lo revise antes de enviarlo a `/api/v1/agent/chat` (DeepSeek, sin cambios).

## Endpoint

`POST /api/v1/agent/transcriptions` — autenticado (cookie JWT) y con CSRF, igual que `/agent/chat`.

Request: `multipart/form-data` con la parte `audio` (archivo grabado por `MediaRecorder`).

```bash
curl -X POST http://localhost:8082/api/v1/agent/transcriptions \
  -b cookies.txt \
  -F "audio=@voice.webm;type=audio/webm;codecs=opus"
```

Respuesta `200`:

```json
{ "text": "Agenda a Juan mañana a las cinco para corte y barba", "remainingToday": 37 }
```

`text` se recorta a 500 caracteres (límite de `ChatRequest.message`). `remainingToday` = dictados que le
quedan hoy al usuario.

| Código | Cuándo | Mensaje |
|---|---|---|
| 400 | Falta la parte `audio`, formato no soportado, firma del archivo inválida, < 1 KB o > 512 KB | "Formato de audio no soportado", "El audio es demasiado corto", ... |
| 413 | Supera el tope de `spring.servlet.multipart` (1 MB) | "El archivo supera el tamaño máximo permitido" |
| 422 | El audio no tenía voz (texto vacío) | "No se entendió el audio, intenta de nuevo" |
| 429 | Cuota diaria del usuario o tope mensual global agotados | "Alcanzaste el límite de dictados por voz de hoy" / "...de este mes" |
| 502 | OpenAI falló o no respondió en 15 s | "El dictado no está disponible en este momento" |

Formatos aceptados (MIME + magic bytes): `audio/webm` (Chrome, Edge, Firefox, Android), `audio/ogg`
(Firefox) y `audio/mp4` (Safari/iOS). Los parámetros como `;codecs=opus` se ignoran.

## Arquitectura (hexagonal)

| Pieza | Rol |
|---|---|
| `AgentController#transcribe` | Recibe el multipart y arma `TranscribeVoiceCommand` |
| `TranscribeVoiceService` (`TranscribeVoiceUseCase`) | Valida, consume cuota, transcribe, recorta |
| `VoiceTranscriptionPolicy` | Límites; la construye `VoiceConfig` desde `VoiceProperties` (`assistant.voice.*`) |
| `VoiceAudioFormat` (dominio) | MIME soportados, extensión y firma de cada formato |
| `SpeechToTextPort` → `OpenAiSpeechToTextAdapter` | Llama al `TranscriptionModel` de Spring AI |
| `VoiceUsageRepositoryPort` → `JdbcVoiceUsageRepositoryAdapter` | Contadores atómicos en PostgreSQL |

El adaptador nombra el archivo `voice.<ext>`: OpenAI detecta el formato por la extensión y rechaza un
archivo sin nombre. Cambiar de proveedor = otro adaptador de `SpeechToTextPort`.

## Control de gasto

El objetivo es que el gasto con OpenAI quede acotado aunque todos los usuarios agoten su cuota.

1. **Validación antes de gastar:** formato, firma y tamaño se revisan antes de consumir cuota o llamar al
   proveedor.
2. **Cuota diaria por usuario:** `assistant.voice.daily-limit-per-user` (40), por día de negocio
   (`app.timezone`, no UTC).
3. **Tope mensual global:** `assistant.voice.monthly-limit` (1500 dictados entre todos los usuarios).
   Peor caso: `1500 × 30 s = 750 min × 0.003 USD ≈ 2.25 USD/mes`.
4. **Atomicidad:** cada consumo es un `INSERT ... ON CONFLICT DO UPDATE ... WHERE request_count < :limit`;
   peticiones simultáneas no pueden pasarse del límite.
5. **Sin reintentos:** `spring.ai.openai.audio.transcription.max-retries: 0` (el default de Spring AI es 3 y
   cada reintento podría cobrarse). El usuario decide si reintenta.
6. **Timeout de 15 s** (`...transcription.timeout`), por debajo de los 30 s de API Gateway.

Reembolsos:

| Situación | Cuota diaria | Tope mensual |
|---|---|---|
| Se agota el tope mensual | Se devuelve | — |
| OpenAI falla (502) | Se devuelve (el usuario puede reintentar) | **No** se devuelve: un timeout puede llegar después de que OpenAI ya cobró |
| Audio sin voz (422) | **No** se devuelve: OpenAI ya procesó el audio | No se devuelve |

El frontend agrega otra capa (ver `docs/assistant-chat.md`): no sube grabaciones canceladas, de menos de
0.8 s o en las que no detectó voz, y corta sola tras 2 s de silencio.

**Garantía final:** en OpenAI usa créditos prepagados **con la recarga automática desactivada**; así el
gasto nunca puede superar el saldo, pase lo que pase en la app.

## Base de datos (`V19__create_voice_usage.sql`)

- `voice_usage_daily (user_id, usage_date, request_count)`: PK `(user_id, usage_date)`, FK a `user_app`.
- `voice_usage_monthly (usage_month, request_count)`: `usage_month` = día 1 del mes.

Una fila por usuario activo y día; no requiere limpieza en el MVP.

## Configuración

```yaml
spring:
  servlet.multipart: { max-file-size: 1MB, max-request-size: 1MB }
  ai:
    model:
      chat: deepseek
      audio: { transcription: openai, speech: none }
      embedding: none     # el starter de OpenAI activa todo por defecto (matchIfMissing=true)
      image: none
      moderation: none
    openai:
      api-key: ${API_OPENAI_KEY}      # sin default: si falta, la app no arranca
      audio.transcription:
        model: gpt-4o-mini-transcribe
        language: es
        temperature: 0
        prompt: "Dictado para un asistente de citas de una barbería: ..."
        max-retries: 0
        timeout: 15s
assistant.voice:
  min-audio-bytes: 1024
  max-audio-bytes: 524288
  daily-limit-per-user: 40
  monthly-limit: 1500
```

`API_OPENAI_KEY`: en dev desde `.env`; en QA desde Parameter Store (SecureString), referenciada en
`.aws/task-definition-backend.json`. El rol `ecsTaskExecutionRole` necesita `ssm:GetParameters` sobre
`arn:aws:ssm:us-east-1:707256068332:parameter/API_OPENAI_KEY`.

Para cambiar de modelo basta con `spring.ai.openai.audio.transcription.model` (p. ej. `gpt-4o-transcribe`,
de mejor calidad y el doble de precio).

## Privacidad

- El audio **no se guarda**: se procesa en memoria y se descarta.
- No se registran en logs ni el audio ni el texto transcrito; el adaptador solo registra el tipo de error
  del proveedor.
- La API key vive solo en el backend.
