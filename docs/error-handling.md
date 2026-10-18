# Manejo de errores (frontend)

Referencia: `GlobalExceptionHandler` del backend, que responde siempre con `ErrorResponse { timestamp, status, error, message }` (`ApiErrorDto` en el frontend).

## Piezas (`src/app/core`)
- `interceptor/error.interceptor.ts`: captura todo `HttpErrorResponse` no recuperado por `refreshInterceptor` y llama a `ErrorHandlerService.notify`. Se omite en `/auth/refresh` y con `SKIP_ERROR_TOAST` (cuando el componente muestra el error inline).
- `service/error-handler.service.ts`: única lógica de mensajes.
  - `extractMessage`: status `0` → sin conexión; `body.message` si existe; si no (HTML de proxy/CloudFront, cuerpo vacío) mensaje por status (400, 401, 403, 404, 409, 413, 429, 5xx).
  - `notify`: toast `warn` para 4xx y `error` para 0/5xx; ignora un toast idéntico dentro de 2 s (varias peticiones fallando a la vez).
  - `notifyUnexpected`: errores no HTTP; detalle a consola, mensaje genérico al usuario.
- `service/global-error-handler.ts`: `ErrorHandler` de Angular; atrapa errores de ejecución no capturados e ignora `HttpErrorResponse` (ya notificados por el interceptor).

## Convenciones
- Los componentes no muestran toasts de errores HTTP: basta con `error: () => undefined`.
- Si un formulario muestra el error inline, enviar la petición con `context: new HttpContext().set(SKIP_ERROR_TOAST, true)` y leer el texto con `ErrorHandlerService.extractMessage`.
- Un mensaje nuevo del backend no requiere cambios en el frontend; solo hay que añadir un fallback si aparece un status nuevo.
