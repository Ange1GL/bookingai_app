# Asistente de AI (`feature/assistant`)

Chat, pensado para móvil, que conversa con el agente del backend (`/api/v1/agent`) para agendar, mover y cancelar citas. Se puede escribir o **dictar por voz**. En el home y la navegación se llama **"Asistente de AI"**.

## Elementos de la interfaz
- **Cabecera** con degradado, avatar, nombre, estado "En línea" y botón *Nueva conversación* (limpia el historial y crea otra sesión).
- **Pantalla de bienvenida** con avatar grande, saludo y 4 tarjetas de acción rápida (Agendar cita, Citas de hoy, Mover cita, Cancelar cita) y aviso para revisar los datos antes de confirmar.
- **Mensajes**: burbujas violeta (usuario) / gris (asistente) con avatar del asistente, hora y marca de enviado; separador "Hoy"; indicador de "escribiendo…"; si falla el envío, botón *Reintentar*.
- **Respuestas rápidas** sobre el campo de texto, con scroll horizontal: Sí, confirmar · No · Hoy · Mañana · Citas de hoy (el agente pide confirmación antes de cancelar o mover).
- **Campo de texto** que crece solo (máx. 500 caracteres, contador desde 400): Enter envía, Shift+Enter agrega línea; auto-scroll al último mensaje.
- **Botón de micrófono** (dictado por voz), junto a Enviar. Ver abajo.

## Dictado por voz
Como el botón 🎤 de ChatGPT/Gemini/Claude: graba, transcribe en el backend (`POST /api/v1/agent/transcriptions`, OpenAI `gpt-4o-mini-transcribe`; ver `backend/docs/voice-transcription.md`) y **deja el texto en la caja para revisarlo**; nunca se envía solo (nombres, fechas y horas deben poder corregirse).

**Flujo**
1. Toque en 🎤 → el navegador pide permiso del micrófono (requiere HTTPS o `localhost`).
2. Mientras graba, la caja de texto y Enviar se ocultan y aparece una barra: ✕ cancelar · medidor de volumen · contador `0:07 / 0:30` (rojo en los últimos 5 s) · ✓ terminar. `Escape` cancela.
3. Al terminar, el botón muestra un spinner; el texto transcrito se **agrega** a lo que ya hubiera escrito (recortado a 500 caracteres) y el foco vuelve a la caja.

**Control de gasto en el navegador** (cada segundo subido se paga; esto evita subir audio inútil)

| Regla | Constante (`models/voice.const.ts`) |
|---|---|
| Tope por grabación: 30 s, corte automático | `VOICE_MAX_SECONDS` |
| Corte automático tras 2 s de silencio después de hablar | `SILENCE_AUTO_STOP_MS` |
| Si no se detecta voz en 6 s, se descarta sin subir | `NO_SPEECH_TIMEOUT_MS`, `SPEECH_RMS_THRESHOLD` |
| Grabaciones de menos de 0.8 s se descartan (toques accidentales) | `MIN_RECORDING_MS` |
| Cancelar (✕ o Escape) no sube nada | — |
| Opus a 24 kbps (30 s ≈ 110 KB) | `AUDIO_BITS_PER_SECOND` |
| **Sin reintentos automáticos**: tras un error de red/5xx se ofrece *Reintentar* (vuelve a subir la misma grabación, sin regrabar); 4xx (cuota, audio inválido) no se reintenta | — |

El backend aplica además la cuota diaria por usuario y el tope mensual global. Cuando quedan 5 dictados o menos en el día, el micrófono muestra el número restante.

**Errores** (aviso en línea sobre el micrófono, `role="alert"`, sin toast global gracias a `SKIP_ERROR_TOAST`): permiso denegado, sin micrófono, muy corto, sin voz, cuota agotada (429), no se entendió (422), servicio no disponible (502, con *Reintentar*).

**Compatibilidad**: `MediaRecorder` + `getUserMedia`. Formato preferido `audio/webm;codecs=opus` (Chrome, Edge, Firefox, Android), luego `audio/ogg`, y `audio/mp4` en Safari/iOS. Sin soporte, el botón no se muestra y el chat sigue funcionando por texto. El análisis de volumen usa Web Audio (`AnalyserNode`); si no existe, solo aplica el tope de 30 s.

**Privacidad**: el audio solo viaja al backend (nunca directo al proveedor; la API key no llega al navegador) y no se guarda.

## Si el asistente no responde
Cuando el envío falla (error de red/5xx del agente, respuesta vacía o más de 45 s sin respuesta, `AGENT_REPLY_TIMEOUT_MS`), el mensaje queda marcado como fallido y debajo aparece, además de *Reintentar*, un aviso: "El asistente no respondió. Puedes reintentar, consultar con el administrador o agendar la cita tú mismo" con el botón **Crear la cita manualmente**. El botón navega a `/appointments?nueva=true` (`shared/navigation/appointment-links.ts`); el calendario abre el formulario de nueva cita y quita el parámetro de la URL. Las peticiones del chat usan `SKIP_ERROR_TOAST`: el error se muestra en línea, no como toast. Al reintentar con éxito, el aviso desaparece.

## Estructura
```
feature/assistant/
  assistant.routes.ts          # provee ChatStore a nivel de ruta
  models/{chat.model,composer-form.model,quick-actions.const,voice.const,voice.model}.ts
  service/{agent.service,chat.store,voice-recorder.service}.ts
  page/assistant-page/
  components/{assistant-avatar,message-bubble,quick-actions,suggestion-chips,chat-composer,voice-input}/
```
- `VoiceRecorderService`: graba con `MediaRecorder`, mide el volumen y resuelve `record()` con `recorded | cancelled | too-short | no-speech`; siempre libera el micrófono. Lo provee `voice-input` (una instancia por componente).
- `VoiceInputComponent`: botón, barra de grabación, subida (`AgentService.transcribe`) y errores; emite `transcribed` y `recordingChange`.
- `core/interceptor/error.interceptor.ts`: `SKIP_ERROR_TOAST` (contexto HTTP) para peticiones que muestran su error en línea.

## Backend (endurecimiento aplicado)
- `ChatRequest` validado: `sessionId` y `message` obligatorios, `message` ≤ 500 caracteres.
- La memoria de conversación se indexa por `userId:sessionId` (un `sessionId` ajeno no abre la conversación de otro usuario).
- Los fallos del modelo devuelven `502` con mensaje genérico.
- Lista negra: si el cliente está bloqueado el agente no agenda, avisa y pregunta si se quita o se mantiene; también puede bloquear/desbloquear con confirmación (ver `backend/docs/customer-blacklist.md`).
- Dictado: ver `backend/docs/voice-transcription.md`.
- Pendiente de decidir: restringir `/agent/raw-chat` fuera de `dev`.

## Pruebas
Specs (Vitest):
- `agent.service` (chat y subida multipart del audio).
- `chat.store` (envío, sesión única, bloqueo en vuelo, reintento, nueva conversación).
- `voice-recorder.service` (MIME y bitrate, corte por silencio, sin voz, tope de 30 s, muy corto, cancelar, liberar micrófono, permiso denegado).
- `voice-input` (sube solo grabaciones con voz, reintento manual con el mismo blob, sin reintento en 429, cuota baja, Escape).
- `chat-composer` (agrega el dictado sin enviarlo, respeta 500 caracteres, oculta la caja al grabar).
- `core/interceptor/error.interceptor` (`SKIP_ERROR_TOAST`).
