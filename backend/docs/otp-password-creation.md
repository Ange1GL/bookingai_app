# Creación de contraseña con código de un solo uso (OTP)

Reemplaza a la "contraseña temporal" de las fases 6 y 7 de [admin-users-roles.md](admin-users-roles.md). Un admin ya
no genera ni envía una contraseña: el sistema envía por WhatsApp un **código de un solo uso** y el propio usuario
**crea su contraseña** con él. Así ninguna contraseña viaja por un tercero y no existe el estado "contraseña
temporal / debe cambiarla".

## 1. Flujo

```text
Admin                       Backend                              WhatsApp           Usuario
  │ POST /admin/users        │                                      │                  │
  │ {username,email,phone,…} │ crea user_app (password = NULL)      │                  │
  │ ───────────────────────► │ emite OTP (hash en one_time_code)    │                  │
  │                          │ ── plantilla de autenticación ─────► │ ───────────────► │ recibe "483920"
  │ ◄─ {user, codeSent} ──── │                                      │                  │
  │ (le dice su username al usuario por otro medio)                                     │
  │                          │                                                         │
  │                          │ ◄── POST /auth/create-password {username, code, newPassword} ──┤
  │                          │ verifica OTP, define la contraseña, consume el OTP       │
  │                          │ ── cookies de sesión (queda con sesión abierta) ───────► │
```

- **Alta** (`POST /api/v1/admin/users`): el usuario nace **sin contraseña** (`user_app.password IS NULL`).
- **Reinicio / reenvío** (`POST /api/v1/admin/users/{id}/reset-password`): borra la contraseña actual, cierra sus
  sesiones, limpia un posible bloqueo por intentos y emite un código nuevo. Sirve también para reenviar el código si
  el WhatsApp falló o el usuario no lo usó a tiempo.
- **Crear contraseña** (`POST /api/v1/auth/create-password`, **público**): lo usa el propio usuario.
- Una cuenta sin contraseña **no puede iniciar sesión** (responde igual que credenciales inválidas, sin revelar nada).

## 2. Modelo de datos (normalizado)

El OTP no ensucia `user_app`: tiene su tabla y su catálogo, como `status_appointment` ↔ `StatusAppointment`.

```text
user_app (user_id PK, …, password NULL, password_changed_at, phone)
   ▲ 1
   │                                                       ┌─ one_time_code_purpose ─┐
   │ N                                                     │ purpose_id PK           │
one_time_code ─────────────────────────────────────────► N │ name UNIQUE             │
  one_time_code_id PK                                   1  │ 1 ACCOUNT_ACTIVATION    │
  user_id          FK fk_one_time_code_user                │ 2 PASSWORD_RESET        │
  purpose_id       FK fk_one_time_code_purpose             └─────────────────────────┘
  code_hash        (BCrypt del código, nunca el código)
  expires_at, failed_attempts, consumed_at, created_at
```

- **Un solo código vigente por usuario**: índice único parcial `uk_one_time_code_pending_per_user` sobre
  `user_id WHERE consumed_at IS NULL`. Emitir uno nuevo consume el anterior (queda como historial).
- **Se eliminó `user_app.must_change_password`**: era un dato derivado. "Debe crear su contraseña" ⇔ `password IS NULL`
  (la API lo expone como `passwordSet` en `UserResponse`).
- `OneTimeCodePurpose` (enum) refleja 1:1 el catálogo; al agregar un propósito, nueva migración con su `INSERT`.
- Migración: `V25__one_time_code_replaces_temporary_password.sql` (los usuarios que aún tenían contraseña temporal
  quedan sin contraseña; el admin les envía un código con `reset-password`).

## 3. Reglas de seguridad

| Aspecto | Decisión |
|---|---|
| Código | 6 dígitos, `SecureRandom`, se guarda **solo su hash BCrypt** |
| Vigencia | `security.otp.ttl-minutes` = **30** |
| Intentos | **5** fallos invalidan el código (hay que pedir otro). El conteo se persiste (`noRollbackFor`) |
| Un solo uso | Al canjearlo se consume; no sirve dos veces |
| Concurrencia | `SELECT … FOR UPDATE` sobre el código al canjear |
| Enumeración | Usuario inexistente / sin código / vencido / agotado / incorrecto → **el mismo 400** y se hace un BCrypt de relleno para igualar el tiempo |
| Política de contraseña | Se valida **antes** de tocar el código: una contraseña débil no gasta un intento |
| Usuario deshabilitado | No puede canjear códigos |
| Al canjear | Define la contraseña, revoca sus refresh tokens, limpia el bloqueo por intentos y abre sesión (cookies) |
| Logs | Nunca se loguean el código, la contraseña ni el cuerpo del request (`toString` ocultos) |
| WhatsApp | Plantilla de **autenticación**; el mensaje sale **después del commit**; si falla, `codeSent:false` y se reintenta |

## 4. API

### `POST /api/v1/auth/create-password` (público)

```http
POST /api/v1/auth/create-password
{ "username": "maria.lopez", "code": "483920", "newPassword": "Mi-contraseña-2026" }

200 OK  (+ cookies de acceso y refresh)
{ "userId": 7, "username": "maria.lopez", "email": "maria@example.com", "roles": ["USER"] }

400 Bad Request   { "message": "Código inválido o vencido", … }      ← cualquier causa
400 Bad Request   { "message": "Invalid field 'newPassword': …" }    ← política de contraseña
```

Política: 10–72 caracteres, al menos una letra y un dígito (`PasswordPolicy`). `code`: solo dígitos.

### Admin (`USER_WRITE`)

```http
POST /api/v1/admin/users
{ "username": "maria.lopez", "email": "maria@example.com", "name": "María López",
  "phone": "+52 155 1234 5678", "roleIds": [1] }

201 Created
{ "user": { "id": 7, …, "active": true, "passwordSet": false, "roles": [ … ] },
  "codeSent": true }
```

- `codeSent: false` ⇒ el usuario quedó creado pero Meta no aceptó el mensaje (o WhatsApp no está configurado).
- Solo en el perfil **dev**, y solo si `codeSent` es `false`, la respuesta incluye `"oneTimeCode": "483920"` para
  poder probar sin WhatsApp (`admin.users.expose-one-time-code`). Nunca en otro caso.
- `POST /api/v1/admin/users/{id}/reset-password` devuelve la misma forma. No se puede aplicar a uno mismo.

## 5. Lo que se eliminó

- `TemporaryPasswordGenerator`, `SecureRandomTemporaryPasswordGenerator` → `OneTimeCodeGenerator`.
- `PasswordChangeRequiredFilter` (403 `X-Error-Code`) y la cabecera expuesta por CORS: ya no hay estado
  "contraseña temporal" que hacer cumplir; sin contraseña no hay login.
- `must_change_password` (columna, entidad, dominio, `mustChangePassword` en las respuestas de login y de usuario).
- `POST /api/v1/auth/change-password` **se conserva**: es el cambio voluntario de contraseña de una sesión abierta.

## 6. Configuración

| Variable | Propiedad | Descripción |
|---|---|---|
| `SYSTEM_USER_EMPLOYEE_META` | `whatsapp.cloud.system-user-token` | Token permanente de Meta (ya existente) |
| `WHATSAPP_OTP_PHONE_NUMBER_ID` | `whatsapp.cloud.otp-phone-number-id` | `phone_number_id` emisor |
| `WHATSAPP_OTP_TEMPLATE_NAME` | `whatsapp.cloud.otp-template-name` | Plantilla de autenticación aprobada |
| — | `whatsapp.cloud.otp-template-language` | `es` por defecto |
| — | `security.otp.ttl-minutes` | 30 |
| — | `admin.users.expose-one-time-code` | `false`; `true` solo en `application-dev.yaml` |

Cómo conseguir la plantilla y el número en Meta: [whatsapp-plantilla-otp.md](whatsapp-plantilla-otp.md).

## 7. Código

- Dominio: `OneTimeCode` (reglas de vigencia/intentos/consumo), `OneTimeCodePurpose`, `InvalidOneTimeCodeException`.
- Aplicación: `OneTimeCodeIssuer` (parte transaccional: crea/invalida y emite), `OneTimeCodeDelivery` (envío),
  `CreateUserService`, `ResetUserPasswordService`, `CreatePasswordService` (canje), puertos `OneTimeCodeGenerator`,
  `OneTimeCodeNotifierPort`, `OneTimeCodeRepositoryPort`.
- Infraestructura: `OneTimeCodeEntity`/`OneTimeCodePurposeEntity`, `JpaOneTimeCodeRepositoryAdapter`,
  `WhatsAppOneTimeCodeNotifierAdapter` (+ `LoggingOneTimeCodeNotifierAdapter` sin configurar),
  `SecureRandomOneTimeCodeGenerator`, `WhatsAppOtpConfig`.
- Pendiente (no hecho): limpieza programada de filas vencidas de `one_time_code` (hoy se conservan como historial) y
  un "olvidé mi contraseña" de autoservicio (reutilizaría esta misma tabla con `PASSWORD_RESET`).
