# Asistente de AI (`feature/assistant`)

Chat de texto, pensado para móvil, que conversa con el agente del backend (`/api/v1/agent`) para agendar, mover y cancelar citas. En el home y la navegación se llama **"Asistente de AI"**.

> **Voz pospuesta.** Se había construido entrada por voz (Web Speech API: `SpeechRecognition` + `speechSynthesis`) y se retiró por ahora. Notas por si se retoma: el reconocimiento de Chrome envía el audio a un servicio online (privacidad), requiere HTTPS/`localhost`, y Firefox/Safari tienen soporte limitado; `speechSynthesis` es local y gratis.

## Elementos de la interfaz
- **Cabecera** con degradado, avatar, nombre, estado "En línea" y botón *Nueva conversación* (limpia el historial y crea otra sesión).
- **Pantalla de bienvenida** con avatar grande, saludo y 4 tarjetas de acción rápida (Agendar cita, Citas de hoy, Mover cita, Cancelar cita) y aviso para revisar los datos antes de confirmar.
- **Mensajes**: burbujas violeta (usuario) / gris (asistente) con avatar del asistente, hora y marca de enviado; separador "Hoy"; indicador de "escribiendo…"; si falla el envío, botón *Reintentar*.
- **Respuestas rápidas** sobre el campo de texto, con scroll horizontal: Sí, confirmar · No · Hoy · Mañana · Citas de hoy (el agente pide confirmación antes de cancelar o mover).
- **Campo de texto** que crece solo (máx. 500 caracteres, contador desde 400): Enter envía, Shift+Enter agrega línea; auto-scroll al último mensaje.

## Estructura
```
feature/assistant/
  assistant.routes.ts          # provee ChatStore a nivel de ruta
  models/{chat.model,composer-form.model,quick-actions.const}.ts
  service/{agent.service,chat.store}.ts
  page/assistant-page/
  components/{assistant-avatar,message-bubble,quick-actions,suggestion-chips,chat-composer}/
```

## Backend (endurecimiento aplicado)
- `ChatRequest` validado: `sessionId` y `message` obligatorios, `message` ≤ 500 caracteres.
- La memoria de conversación se indexa por `userId:sessionId` (un `sessionId` ajeno no abre la conversación de otro usuario).
- Los fallos del modelo devuelven `502` con mensaje genérico.
- Lista negra: si el cliente está bloqueado el agente no agenda, avisa y pregunta si se quita o se mantiene; también puede bloquear/desbloquear con confirmación (ver `backend/docs/customer-blacklist.md`).
- Pendiente de decidir: restringir `/agent/raw-chat` fuera de `dev`.

## Pruebas
Specs (Vitest): `agent.service` y `chat.store` (envío, sesión única, bloqueo en vuelo, reintento, nueva conversación).
