# Plan: dictado por voz en el Asistente de AI (backend + frontend)

**Estado:** implementado (tope ajustado a 30 s; cuota 40/día por usuario + 1500/mes global). Pendiente: pasos manuales de AWS
**Rama:** `feature_ai_voice`
**Modelo STT:** `gpt-4o-mini-transcribe` (OpenAI, el modelo ligero: ~$0.003 USD/min)
**API key:** variable de entorno existente `API_OPENAI_KEY`
**Documentación consultada (Context7):** Spring AI `v2.0.0-M6` (tu versión del BOM), sección *OpenAI Transcriptions*

---

## 0. Qué se construye

**Modo dictado**, igual que el botón 🎤 del chat de ChatGPT, Gemini o Claude:

1. El usuario toca 🎤 y habla (máximo **20 s**, con corte automático tras **2 s de silencio**).
2. El navegador graba con `MediaRecorder` y sube el audio al backend.
3. El backend valida, revisa la cuota y transcribe con `gpt-4o-mini-transcribe`.
4. El texto aparece **en la caja del chat, editable**. El usuario lo revisa y pulsa Enviar.
5. El flujo de `/api/v1/agent/chat` con DeepSeek **no cambia**.

No incluye respuesta hablada, streaming ni modo conversacional en tiempo real; quedan para después del MVP.

---

## 1. Análisis: ¿cuánto tiempo hace falta para reservar una cita?

### Información mínima de una reserva
Cliente, servicio, día y hora. Opcional: notas.

### Velocidad de habla
Al dictarle a una máquina se habla más despacio que en una conversación: **~2 a 2.5 palabras/s**, con pausas.

| Caso real | Ejemplo | Palabras | Duración estimada |
|---|---|---|---|
| Mínimo | "Agenda a Juan mañana a las cinco" | 6 | **~3 s** |
| Completo | "Agéndame a Juan Pérez mañana a las cinco de la tarde para corte y barba" | 15 | **~6–7 s** |
| Con titubeos | "Eh… ponme una cita para la señora Martínez, este, el jueves que viene como a las once, es para tinte" | ~20 + pausas | **~10–12 s** |
| Compuesto | "Mueve la cita de Luis del viernes al sábado a las diez y cancela la de Pedro de hoy" | 19 | **~9 s** |
| Con corrección | "…a las cuatro, no, mejor a las cinco" | +6 | **+2–3 s** |
| Respuesta a confirmación | "Sí, confírmala" | 2 | **~1–2 s** |

### Conclusión
- **Más del 95 % de los mensajes cabe en 15 s.**
- **Un tope de 20 s** cubre los casos compuestos y con titubeos y deja margen.
- 20 s son unas 40–50 palabras, es decir **250–300 caracteres**, muy por debajo de tu límite de 500 en `ChatRequest`. **No hay que tocar ese límite.**
- 30 s no aportan nada para reservar citas; solo suben el peor caso de costo un 50 %.

### Dónde se ahorra de verdad
OpenAI cobra **por la duración real del audio**, no por el tope. El tope solo limita el peor caso. El ahorro real viene de **no mandar audio inútil**:

| Palanca | Ahorro |
|---|---|
| **Corte automático tras 2 s de silencio** (detección de voz con `AnalyserNode`) | Evita pagar silencios cuando el usuario olvida detener la grabación. Es la palanca más importante |
| **Descartar grabaciones de menos de 0.8 s** (toques accidentales) | No se sube nada; costo 0 |
| **Botón Cancelar** (✕ o Esc) | No se sube nada; costo 0 |
| **Modelo `gpt-4o-mini-transcribe`** | La mitad del precio de `gpt-4o-transcribe` y `whisper-1` |
| **`response-format: text` y `language: es`** | Respuesta mínima; menos latencia |
| **Sin reintentos automáticos** del backend ni del frontend | Nunca se paga dos veces un mismo audio sin que el usuario lo pida |
| **Cuota diaria por usuario** (60 transcripciones/día) | Limita el peor caso y el abuso |
| **Tope de 20 s y tamaño máximo en el backend** | El cliente no puede saltárselo |

### Costos estimados (gpt-4o-mini-transcribe ≈ $0.003/min ≈ $0.00005/s)

| Escenario | Cálculo | Costo |
|---|---|---|
| Un mensaje típico (7 s) | 7 × 0.00005 | **$0.00035** |
| Usuario normal (15 mensajes/día × 7 s × 22 días) | 2,310 s ≈ 38.5 min | **~$0.12 / mes** |
| Peor caso con cuota (60/día × 20 s × 30 días) | 600 min | **~$1.80 / mes** |
| 100 usuarios normales | 100 × 0.12 | **~$12 / mes** |

Conviene revisar el precio vigente en la página de OpenAI antes de lanzar.

---

## 2. Backend

### 2.1 Dependencia (`backend/pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```
Su versión la gestiona el BOM `spring-ai-bom 2.0.0-M6`, el mismo que usa DeepSeek.

### 2.2 Configuración (`application.yaml`)
Según la documentación de M6, el auto-configure se activa por tipo de modelo con `spring.ai.model.*`. Se **activa solo la transcripción** de OpenAI; el chat sigue con DeepSeek y el resto queda apagado. Así no se crean beans ni llamadas innecesarias.

```yaml
spring:
  ai:
    model:
      chat: deepseek                 # ya existe
      audio:
        transcription: openai        # solo STT con OpenAI
        speech: none
      embedding: none
      image: none
      moderation: none
    openai:
      api-key: ${API_OPENAI_KEY}     # sin valor por defecto: si falta, la app no arranca
      audio:
        transcription:
          options:
            model: gpt-4o-mini-transcribe
            language: es
            response-format: text
            temperature: 0
  servlet:
    multipart:
      max-file-size: 1MB
      max-request-size: 1MB

assistant:
  voice:
    max-duration-seconds: 20
    max-audio-bytes: 1048576          # 1 MB: Safari (AAC) pesa más que opus; 20 s de opus ≈ 60 KB
    min-audio-bytes: 2048             # descarta blobs vacíos o corruptos
    daily-limit: 60                   # transcripciones por usuario y día (zona del negocio)
    allowed-mime-types: audio/webm, audio/ogg, audio/mp4
    # Contexto para mejorar nombres y vocabulario del dominio. Son pocos tokens de texto.
    prompt: "Asistente de citas de una barbería: agendar, mover o cancelar citas; nombres de clientes, servicios, días y horas."
```

- **Propiedades a verificar en la implementación:** los nombres exactos de `spring.ai.model.embedding/image/moderation/audio.speech` (`none`). También que el starter de OpenAI no levante un `ChatModel` que compita con el de DeepSeek; tu `AiConfig` ya construye `ChatClient` y lo voy a revisar.
- **Timeout hacia OpenAI:** unos 15 s, para quedar por debajo de los 30 s de API Gateway. Lo configuro con la propiedad del cliente si M6 la expone; si no, con el `RestClient` del adaptador.

### 2.3 Hexagonal: piezas nuevas

**Puertos y casos de uso (`application/`)**
- `port/out/SpeechToTextPort`: `String transcribe(VoiceAudio audio)`.
- `port/out/VoiceUsageRepositoryPort`:
  - `boolean tryConsume(Long userId, LocalDate day, int dailyLimit)`, que es **atómico**;
  - `void refund(Long userId, LocalDate day)`.
- `command/TranscribeVoiceCommand`: `userId`, `byte[] audio`, `mimeType`.
- `result/TranscriptionResult`: `text`, `remainingToday`.
- `service/TranscribeVoiceService` (caso de uso):
  1. Valida tamaño mínimo y máximo y el MIME contra la lista blanca → `InvalidVoiceAudioException`.
  2. Llama a `tryConsume(userId, hoyEnZonaDelNegocio, limit)`. Si es `false` → `VoiceQuotaExceededException`.
  3. Llama a `speechToTextPort.transcribe(...)`.
  4. Si el proveedor falla → `refund(...)` y `SpeechToTextUnavailableException`. **No hay reintento automático.**
  5. Si el texto está vacío (solo hubo silencio) → `EmptyTranscriptionException`, devuelta como 422 con "No se entendió el audio".
  6. Recorta el texto a 500 caracteres, el límite del chat, y devuelve `TranscriptionResult`.
- La fecha usa tu `Clock` y `BusinessTimeProperties.timezone`, para que la cuota se reinicie a medianoche del negocio y no en UTC.

**Adaptadores (`infrastructure/`)**
- `adapter/out/ai/OpenAiSpeechToTextAdapter implements SpeechToTextPort`:
  - Usa el `TranscriptionModel` autoconfigurado con `OpenAiAudioTranscriptionOptions` (el prompt sale de la configuración).
  - **Detalle importante:** envuelve los bytes en un `ByteArrayResource` que **sobrescribe `getFilename()`** (`voice.webm` / `voice.mp4` / `voice.ogg`). OpenAI detecta el formato por la extensión, y sin nombre de archivo rechaza el audio.
  - Traduce las excepciones del cliente de OpenAI a `SpeechToTextUnavailableException`, con un log sin datos del usuario.
- `adapter/out/persistence/...`:
  - `VoiceUsageEntity` con PK compuesta `(user_id, usage_date)`;
  - repositorio JPA y adaptador del puerto, siguiendo la nomenclatura actual.
  - `tryConsume` en **una sola sentencia nativa**, para que dos peticiones simultáneas no se salten el límite:
    ```sql
    INSERT INTO voice_usage (user_id, usage_date, request_count) VALUES (:userId, :day, 1)
    ON CONFLICT (user_id, usage_date)
    DO UPDATE SET request_count = voice_usage.request_count + 1
    WHERE voice_usage.request_count < :limit
    ```
    Una fila afectada significa que se consumió; cero filas, que se agotó la cuota.
  - `refund`: `UPDATE ... SET request_count = request_count - 1 WHERE ... AND request_count > 0`.
- `adapter/in/rest/controller/AgentController`, endpoint nuevo:
  - `POST /api/v1/agent/transcriptions`, `multipart/form-data`, parte `audio` (`MultipartFile`).
  - Respuesta 200: `{ "text": "agenda a Juan mañana a las 5", "remainingToday": 57 }` (`TranscriptionResponse`).
  - Usa `@CurrentUserId`, igual que `/chat`.
- `GlobalExceptionHandler`: mapeos nuevos con el formato de error actual.

  | Excepción | HTTP | Mensaje |
  |---|---|---|
  | `InvalidVoiceAudioException` | 400 | "El audio no es válido" |
  | `MaxUploadSizeExceededException` | 413 | "El audio es demasiado largo" |
  | `EmptyTranscriptionException` | 422 | "No se entendió el audio, intenta de nuevo" |
  | `VoiceQuotaExceededException` | 429 | "Alcanzaste el límite de mensajes de voz de hoy" |
  | `SpeechToTextUnavailableException` | 502 | "El dictado no está disponible en este momento" |

- `VoiceProperties` (`@ConfigurationProperties("assistant.voice")`) validado al arrancar, como tus otros `*Properties`.

### 2.4 Base de datos (Flyway)
`V19__create_voice_usage.sql`:
```sql
CREATE TABLE voice_usage (
    user_id       BIGINT  NOT NULL REFERENCES users(id),   -- confirmar el nombre real de la tabla de usuarios
    usage_date    DATE    NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0 CHECK (request_count >= 0),
    PRIMARY KEY (user_id, usage_date)
);
```
Una fila por usuario activo y por día. Es muy poco espacio; la limpieza de filas antiguas queda opcional y fuera del MVP.

### 2.5 Seguridad y privacidad
- El endpoint queda **autenticado** (JWT en cookie) y con **CSRF**, como `/chat`. El `RateLimitFilter` por IP ya lo cubre.
- **El audio no se guarda**: se procesa en memoria y se descarta.
- **No se escribe en logs ni el texto transcrito ni el audio**; solo `userId`, tamaño, MIME, latencia y resultado.
- La API key solo vive en el backend (`API_OPENAI_KEY`); nunca llega al navegador.

### 2.6 Tests (backend)
- **`TranscribeVoiceServiceTest`** (Mockito):
  - éxito;
  - MIME inválido, demasiado pequeño o demasiado grande → no consume cuota y `verifyNoInteractions(speechToTextPort)`;
  - cuota agotada → no llama al proveedor;
  - el proveedor falla → `refund` y la excepción;
  - texto vacío;
  - texto recortado a 500 caracteres;
  - la fecha usa la zona del negocio (con `Clock` fijo).
- **`OpenAiSpeechToTextAdapterTest`**: `TranscriptionModel` simulado. Comprueba con un captor que el `Resource` lleva el nombre con la extensión correcta y que se pasa el prompt.
- **Test del adaptador de persistencia** (`@DataJpaTest` o equivalente, si el proyecto ya tiene infraestructura de BD para tests): el límite atómico y `refund`.
- **`AgentController`** (`@WebMvcTest`): multipart OK → 200; sin parte `audio` → 400; mapeos 413, 429 y 502.

---

## 3. Frontend (`feature/assistant`)

### 3.1 Constantes y modelos
- `models/voice.const.ts`: `VOICE_MAX_SECONDS = 20`, `SILENCE_AUTO_STOP_MS = 2000`, `MIN_RECORDING_MS = 800`, `AUDIO_BITS_PER_SECOND = 24000`, `PREFERRED_MIME_TYPES = ['audio/webm;codecs=opus', 'audio/ogg;codecs=opus', 'audio/mp4']`.
  Va junto a `quick-actions.const.ts`; respeta la regla de "no constants folder".
- `models/voice.model.ts`:
  - `TranscriptionResponseDto { text: string; remainingToday: number }`;
  - `VoiceState = 'idle' | 'requesting-permission' | 'recording' | 'transcribing'`;
  - `VoiceError = 'not-supported' | 'permission-denied' | 'too-short' | 'quota' | 'not-understood' | 'unavailable'`.

### 3.2 `service/voice-recorder.service.ts`
Lo provee el componente del botón, así cada instancia tiene su ciclo de vida.
- `isSupported()`: `navigator.mediaDevices?.getUserMedia && 'MediaRecorder' in window`.
- `start()`:
  - `getUserMedia({ audio: { echoCancellation: true, noiseSuppression: true, channelCount: 1 } })`;
  - elige el MIME con `MediaRecorder.isTypeSupported` en el orden de `PREFERRED_MIME_TYPES` (Safari en iOS cae en `audio/mp4`);
  - crea el `MediaRecorder` con `audioBitsPerSecond`.
- **Tope:** un temporizador hace `stop()` automático a los `VOICE_MAX_SECONDS`.
- **Detección de silencio:** `AudioContext` + `AnalyserNode`, calcula el RMS en cada frame.
  - Si el RMS queda bajo un umbral durante `SILENCE_AUTO_STOP_MS`, **después de haber detectado voz al menos una vez**, se hace `stop()` automático.
  - El mismo RMS alimenta la señal `level`, que dibuja la onda.
- Señales: `state`, `elapsedSeconds`, `level`.
- `stop()` devuelve `Promise<{ blob, mimeType, durationMs } | null>`: `null` si dura menos de `MIN_RECORDING_MS`.
- `cancel()`: descarta todo, sin subir nada.
- **Limpieza siempre** (en stop, cancel, error y `ngOnDestroy`): `track.stop()` y `audioContext.close()`. Así el indicador rojo del micrófono del navegador se apaga.

### 3.3 `AgentService.transcribe(blob, mimeType)`
- Crea un `FormData` con `append('audio', blob, 'voice.' + extensión)`.
- Hace POST a `${environment.apiBaseUrl}/api/v1/agent/transcriptions` con `SKIP_LOADER`, porque el compositor muestra su propio estado.
- **No fija `Content-Type` a mano**, para que el navegador ponga el `boundary`. `credentials` y `csrf` se aplican igual por los interceptores existentes.
- Devuelve `Observable<TranscriptionResponseDto>`. Los códigos 400, 413, 422, 429 y 502 se traducen a `VoiceError`.

### 3.4 Componente nuevo `components/voice-input/` (`voice-input.component.{ts,html}`)
`OnPush`, signals y `templateUrl`, según las convenciones.

**Inputs y outputs:**
- input `disabled` (mientras el chat envía);
- output `transcribed: string`;
- output `recordingChange: boolean`, para que el compositor oculte el textarea mientras graba.

**Estados de la interfaz (al estilo de ChatGPT):**
- **idle:** botón 🎤 (`pi pi-microphone`) junto a Enviar. Se oculta si no hay soporte.
- **recording:** barra que reemplaza al textarea:
  - ✕ cancelar;
  - onda de 5 a 7 barras animadas con `level()`;
  - contador `0:07 / 0:20`, que **se pone en rojo en los últimos 5 s**;
  - ✓ terminar.
  - Esc cancela.
- **transcribing:** spinner con "Transcribiendo…"; los botones quedan deshabilitados.
- **error:** mensaje breve en línea bajo el compositor (`role="alert"`). Por ejemplo, "Permite el micrófono en la configuración del navegador", "Muy corto, mantén pulsado más tiempo" o "Límite de voz de hoy alcanzado".
  - Para "No disponible" se muestra un botón **Reintentar**, que vuelve a subir **el mismo blob** sin regrabar. Ese reintento solo ocurre si el usuario lo pulsa.

**Accesibilidad:** `aria-label` en cada botón, `aria-live="polite"` para los cambios de estado y foco devuelto al textarea al terminar.

### 3.5 `chat-composer`
- Incluye `<app-voice-input>`.
- Con `(transcribed)`, hace `patchValue` del texto: lo **agrega** a lo que ya hubiera, con un espacio, y lo recorta a `MESSAGE_MAX_LENGTH`. Después enfoca el textarea con el cursor al final.
- **No envía automáticamente.** El usuario revisa y pulsa Enviar, por seguridad con nombres, fechas y horas.
- Mientras se graba (`recordingChange`), oculta el textarea y el botón de enviar.

### 3.6 Tests (Vitest)
- **`voice-recorder.service.spec.ts`** (con `MediaRecorder`, `getUserMedia` y `AudioContext` simulados):
  - elige el MIME soportado;
  - corta solo a los 20 s;
  - corta por silencio después de haber detectado voz;
  - descarta grabaciones de menos de 0.8 s;
  - `cancel` no produce blob;
  - siempre libera los tracks;
  - permiso denegado → `permission-denied`.
- **`agent.service.spec.ts`** (ampliado): URL, `FormData` con nombre y extensión, `SKIP_LOADER`, y el mapeo de los códigos de error.
- **`voice-input.component.spec.ts`**: transiciones de estado, emisión de `transcribed`, "Reintentar" reutiliza el blob, se oculta sin soporte.
- **`chat-composer`**: agrega el texto transcrito, lo recorta a 500 y no envía solo.

---

## 4. Infraestructura (AWS)
- **`.aws/task-definition-backend.json`:** nuevo secret `API_OPENAI_KEY` → `arn:aws:ssm:us-east-1:707256068332:parameter/API_OPENAI_KEY`.
- **Pasos manuales tuyos**, antes del deploy:
  1. `aws ssm put-parameter --region us-east-1 --name API_OPENAI_KEY --type SecureString --value "<key>"`
  2. Dar `ssm:GetParameters` sobre ese ARN al `ecsTaskExecutionRole`.
  3. **Límite de gasto mensual** en el panel de OpenAI (por ejemplo, $10) y alerta por correo.
- **API Gateway HTTP API:** acepta hasta 10 MB de payload y 30 s de timeout. El audio pesa como mucho 1 MB y la transcripción tarda entre 1 y 3 s.
- **CloudFront:** el comportamiento de `/api/*` ya reenvía POST; hay que verificar en QA que `multipart/form-data` pase con el body completo.
- **El micrófono exige HTTPS**, que CloudFront ya da. En `localhost` también funciona.

---

## 5. Documentación (en los mismos commits)
| Archivo | Cambio |
|---|---|
| `backend/docs/voice-transcription.md` (nuevo) | Endpoint con ejemplos de request y response, errores, cuota, configuración, privacidad, costos y cómo cambiar de modelo |
| `docs/assistant-chat.md` | Quitar "Voz pospuesta"; documentar el dictado, sus estados, límites y tests |
| `CLAUDE.md` (raíz) | Agregar las líneas de los docs nuevos y la feature `assistant` con dictado |
| `backend/README.md` | `API_OPENAI_KEY` en variables de entorno; enlace al doc nuevo |

---

## 6. Orden de trabajo y commits
1. `feat(agent): add voice transcription endpoint with daily quota`: backend completo, migración, tests y docs del backend.
2. `feat(assistant): add voice dictation to chat composer`: frontend completo, tests y docs del frontend.
3. `ci(backend): inject api openai key into ecs task`: task definition.

Después de cada paso: `./mvnw test` y `npx ng test --watch=false`. **Prueba manual final:** levantar el backend con tu `.env` y `npm start`, grabar en Chrome y, si puedes, en un iPhone.

**Estimación:** backend 2–3 días, frontend 2–3 días, más ½ día de documentación e infraestructura.

---

## 7. Decisiones para aprobar
1. **¿Tope de 20 s?** Es mi recomendación, por el análisis de la sección 1. La alternativa son 30 s.
2. **¿Cuota de 60 transcripciones por usuario y día?** El peor caso son ~$1.80 al mes por usuario.
3. **¿El texto transcrito se queda en la caja para revisarlo** (recomendado), o se envía solo?
4. **¿Se devuelve la cuota cuando OpenAI falla?** Recomiendo que sí.
5. **¿Endpoint `POST /api/v1/agent/transcriptions`** dentro de `AgentController`? Lo recomiendo, porque comparte autenticación y dominio con el chat.
