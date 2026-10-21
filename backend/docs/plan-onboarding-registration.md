# Plan: registro por solicitud (onboarding) con aprobación del administrador y entrega manual de credenciales

> **Estado:** la Fase 1 (entrega manual) y la recuperación de contraseña por el admin ya están implementadas, ver
> [manual-onboarding-and-password-recovery.md](manual-onboarding-and-password-recovery.md). Las solicitudes de
> onboarding (Fase 2) y la limpieza (Fase 3) siguen pendientes.

## Contexto y hallazgos

- `POST /auth/register` está **comentado** en `AuthController` (el registro abierto ya está apagado), pero siguen
  vivos `RegisterUserCase`, `RegisterUserService` y `RegisterRequest`, y el frontend tiene
  `register-form` / `register-page` / `register-request.dto`. Ese código crea el usuario **activo y con contraseña
  elegida por el visitante**: es justo lo que se quiere dejar de hacer.
- Ya existe casi todo lo necesario para el alta por el administrador: `POST /api/v1/users` crea el usuario **sin
  contraseña**, emite un código de un solo uso (OTP, hasheado en `one_time_code`) y el usuario lo canjea en
  `POST /auth/create-password` (ver `otp-password-creation.md`).
- Lo único que depende de Meta es **la entrega** del código (`OneTimeCodeNotifierPort` →
  `WhatsAppOneTimeCodeNotifierAdapter`). Hoy, sin Meta configurado, cae a `LoggingOneTimeCodeNotifierAdapter` y
  `codeSent:false`; el código solo se devuelve en el perfil `dev`.

## Mi opinión sobre tu propuesta

1. **Solicitud + aprobación manual: sí.** Con pocos clientes y contacto personal por WhatsApp es lo correcto y
   cuesta casi nada. Además filtra spam y te deja conocer a cada cliente antes de darle acceso.
2. **La solicitud no debe ser un usuario "pendiente".** Recomiendo una tabla propia `onboarding_request` (sin
   contraseña, sin rol, sin sesión). Un `status` dentro de `user_app` mezclaría "persona que pidió acceso" con
   "cuenta real", ensuciaría los únicos de `username`/`email` (alguien podría "reservar" un nombre) y todo el código
   de autenticación tendría que acordarse de filtrar pendientes. Al aprobar, se **crea el usuario** reutilizando el
   alta existente.
3. **No vuelvas a contraseñas temporales; conserva el OTP y solo cambia cómo se entrega.** Pasarle una contraseña por
   WhatsApp es justo lo que la V25 eliminó. Con el flujo actual tú recibes un código de un solo uso, se lo mandas tú
   por WhatsApp y el cliente **elige su propia contraseña** (tú nunca la conoces). Mismo canal manual, mucho más
   seguro, y cuando Meta verifique tu negocio se activa el envío automático sin reescribir nada.
4. **El TTL de 30 min no sirve con entrega manual** (tú estás en el celular, el cliente tarda). Con modo manual:
   72 h, configurable.
5. **Olvidé mi contraseña: sin autoservicio por ahora.** Un correo/WhatsApp automático cuesta y ya dijiste que no.
   El flujo manual queda: el cliente te escribe → tú pulsas `reset-password` (ya existe) → te devuelve un código
   nuevo → se lo mandas. Es mejor, además, que "mandar por correo": **solo envía el código al teléfono registrado**
   (o verifica de otra forma que es él) para que nadie suplante a un cliente pidiéndote un reinicio. El autoservicio
   (`PASSWORD_RESET` ya existe en el catálogo) queda para cuando haya canal de envío.
6. **Que tú crees usuarios directamente: ya se puede** (`POST /api/v1/users`) y se mantiene. Útil para clientes que
   conoces en persona y no necesitan llenar solicitud.
7. **Aviso de solicitudes nuevas:** sin correo ni WhatsApp automático, solo hay lista (`status=PENDING`) más un log.
   Para tu volumen basta; más adelante una pantalla de administración con contador.

## Flujo objetivo

```text
Visitante                    Backend                               Admin (tú)
  │ POST /onboarding-requests  │ guarda solicitud PENDING (público,
  │ {name,email,phone,         │ con rate limit, respuesta genérica 202)
  │  username,businessName}    │
  │ ◄── 202 ────────────────── │
  │                            │ ◄── GET /onboarding-requests?status=PENDING
  │                            │ ◄── POST /onboarding-requests/{id}/approve
  │                            │ crea user_app (sin contraseña, rol USER) + OTP
  │                            │ ── respuesta: {user, oneTimeCode, whatsappLink} ─► tú
  │ ◄── (tú por WhatsApp: usuario + código + link de /auth/create-password)
  │ POST /auth/create-password {username, code, newPassword} → sesión abierta
```

Rechazo: `POST /onboarding-requests/{id}/reject` con motivo opcional; no envía nada (se lo dices tú).

## Cambios

### Fase 1 — Entrega manual del código (la prioridad que mencionas; sirve ya para `POST /users`)
- Propiedad `otp.delivery` = `manual` | `whatsapp` (por defecto `manual`). Selecciona el `OneTimeCodeNotifierPort`
  (el adaptador de WhatsApp se conserva intacto, apagado hasta que Meta verifique el negocio).
- En modo `manual`, `IssuedCodeResult` **siempre** devuelve `oneTimeCode` al admin que lo creó/reinició (hoy solo en
  dev) más un `whatsappLink` (`https://wa.me/<phone>?text=<mensaje con usuario, código y enlace>`) para abrirlo y
  enviarlo con un clic. El código en BD sigue siendo solo hash y solo se muestra en esa respuesta; si lo pierdes,
  `reset-password` genera otro. Nunca se loguea.
- `security.otp.ttl-minutes` pasa a depender del modo (manual: 4320 = 72 h). Misma regla de 5 intentos.
- Migración: ninguna (no cambia el esquema).

### Fase 2 — Solicitudes de onboarding
- Migración nueva `V<n>__create_onboarding_request.sql`: catálogo `onboarding_request_status`
  (1 PENDING, 2 APPROVED, 3 REJECTED) + tabla `onboarding_request` (id, `username`, `email`, `name`, `phone`,
  `business_name`, `status_id` FK, `reviewed_by` FK a `user_app` null, `reviewed_at`, `rejection_reason`,
  `created_user_id` FK null, `created_at`). FKs/uniques con nombre, patrón doble campo (AGENTS.md). Índice único
  parcial para no tener dos PENDING con el mismo email.
- Dominio: `OnboardingRequest`, enum `OnboardingRequestStatus` (espejo del catálogo), excepciones de dominio
  (`OnboardingRequestNotFound`, `OnboardingRequestAlreadyReviewed`) con su `@ExceptionHandler`.
- Aplicación: puertos `SubmitOnboardingRequestUseCase`, `ListOnboardingRequestsUseCase`,
  `ApproveOnboardingRequestUseCase` (llama a `OneTimeCodeIssuer.issueForNewUser` + `OneTimeCodeDelivery`, o sea el
  mismo alta de Fase 1), `RejectOnboardingRequestUseCase`; servicios `@Service` transaccionales; puerto de salida
  `OnboardingRequestRepositoryPort`.
- Infraestructura: entidad/repositorio/adapter/mapper de persistencia y `OnboardingRequestController`:
  - `POST /api/v1/onboarding-requests` — **público** (agregar a `permitAll` y al `RateLimitFilter` con cupo bajo).
    Siempre 202 con cuerpo genérico, exista o no el usuario/email (no permite enumerar cuentas). Validación de
    entradas en el borde.
  - `GET /api/v1/onboarding-requests` (filtro `status`, paginado) — `USER_READ`.
  - `POST /{id}/approve` y `POST /{id}/reject` — `USER_WRITE`. Aprobar falla con el 409 normal si el
    `username`/`email` ya existen (entonces rechazas o corriges).
  - Se reutilizan `USER_READ`/`USER_WRITE`: no hacen falta permisos nuevos ni migración de permisos.
- Tests: servicios (aprobar crea usuario sin contraseña con rol USER, doble aprobación, rechazo, duplicados),
  controller/seguridad (público vs 403), y un flujo completo de integración como `CreatePasswordFlowTest`.

### Fase 3 — Limpieza y frontend
- Eliminar código muerto del registro con contraseña: `RegisterUserCase`, `RegisterUserService`, `RegisterRequest`,
  el bloque comentado de `AuthController` y su import.
- Frontend: reemplazar `register-form`/`register-page` por **"Solicitar acceso"** (nombre, correo, teléfono,
  usuario, nombre del negocio) con mensaje de confirmación genérico; borrar `register-request.dto`; en el login
  agregar "¿Olvidaste tu contraseña? Escríbele al administrador".
- Pantalla de administración (lista de solicitudes con Aprobar/Rechazar y botón de WhatsApp): **fuera de alcance**
  de este plan; mientras tanto se opera por API (Postman/curl). Proponerla como siguiente paso.

### Fase 4 — Documentación (mismo commit que cada fase)
- Actualizar `otp-password-creation.md` (entrega manual, TTL por modo), `admin-users-roles.md` (endpoints nuevos),
  `whatsapp-plantilla-otp.md` (anotar que el envío automático queda opcional), `CLAUDE.md`/`README.md` si listan
  endpoints o variables, y un `docs/onboarding-requests.md` nuevo con ejemplos request/response.

## Orden de commits sugerido

1. `feat(auth): add manual one-time code delivery mode` (Fase 1)
2. `feat(onboarding): add access requests approved by an admin` (Fase 2)
3. `refactor(auth)!: remove password-based self registration` + `feat(ui): replace register screen with access request` (Fase 3)

La Fase 1 sola ya cubre tu prioridad inmediata: crear usuarios tú y pasarles el acceso por WhatsApp a mano.

## Riesgos / puntos a vigilar

- **Endpoint público nuevo:** spam. Mitigación: rate limit por IP, límites de longitud, sin efectos secundarios
  hasta que tú aprobes, respuesta uniforme.
- **El código aparece en la respuesta del admin:** solo se devuelve con `USER_WRITE` y por HTTPS; recordar no
  pegarlo en logs de Postman compartidos. Caduca y es de un solo uso.
- **Suplantación en "olvidé mi contraseña" manual:** enviar siempre al teléfono registrado.
- **Breaking change menor:** `POST /auth/register` ya estaba deshabilitado, así que no hay consumidores; el
  frontend de registro se sustituye.

## Decisiones que necesito de ti

1. ¿De acuerdo con la **tabla `onboarding_request` separada** (recomendado) en lugar de un `status` en `user_app`?
2. ¿Empiezo por la **Fase 1** sola (rápida, desbloquea tu caso de hoy) y luego las demás, o todo de una vez?
3. ¿Qué datos debe pedir la solicitud? Propuesta: nombre, correo, teléfono, usuario y nombre del negocio.
