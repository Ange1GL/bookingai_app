# Alta manual de usuarios y recuperación de contraseña por el administrador

Mientras Meta no verifique el negocio no se pueden enviar códigos por WhatsApp. El flujo actual es **manual**: el
administrador da de alta al cliente, copia un mensaje y se lo pasa por su propio chat. Reemplaza el envío automático
descrito en [otp-password-creation.md](otp-password-creation.md), que se conserva apagado (`security.otp.delivery=whatsapp`).

## 1. Alta de usuario

`POST /api/v1/users` (`USER_WRITE`)

```http
{ "username": "maria.lopez", "name": "María", "lastName": "López Pérez",
  "email": "maria@example.com", "phone": "55 1234 5678", "roleIds": [1] }

201 Created
{ "user": { "id": 7, "username": "maria.lopez", "name": "María", "lastName": "López Pérez",
            "email": "maria@example.com", "phone": "525512345678", "active": true, "passwordSet": false, ... },
  "codeSent": false,
  "oneTimeCode": "483920",
  "shareMessage": "usuario: maria.lopez\ncodigoAcceso: 483920" }
```

- `name` y `lastName` (apellidos) son obligatorios; `email` es **opcional** (`null` o vacío = sin correo; hay un único
  por correo solo si se informa); `phone` es obligatorio.
- `shareMessage` es el texto para copiar y pegar en el chat del cliente (dos líneas, `usuario:` y `codigoAcceso:`).
  El frontend solo necesita un botón "Copiar" sobre ese campo.
- El cliente canjea el código en `POST /api/v1/auth/create-password` y elige su propia contraseña; el administrador
  nunca la conoce. El código vale 72 h (`security.otp.ttl-minutes`), es de un solo uso y se guarda solo su hash:
  si se pierde, se genera otro con `POST /api/v1/users/{id}/reset-password`.

### Formato del teléfono

Se guarda en formato internacional **solo con dígitos y con prefijo de país** (como lo usa WhatsApp):

| Entrada | Se guarda |
|---|---|
| `55 1234 5678` (10 dígitos, sin `+`) | `525512345678` (se antepone 52, México) |
| `+52 55 1234 5678` | `525512345678` |
| `+1 415 555 2671` | `14155552671` (con `+` se respeta el país) |
| `525512345678` (más de 10 dígitos) | tal cual |

Así el sistema no se limita a México: para otro país basta escribirlo con `+` y su prefijo. La migración V26
antepone `52` a los teléfonos de 10 dígitos que ya existían. El prefijo por defecto es
`PhoneNumbers.DEFAULT_COUNTRY_CODE`.

## 2. Recuperación de contraseña (atendida por el administrador)

```text
Usuario                         Backend                                   Administrador
  │ POST /auth/password-recovery  │
  │ { "username": "maria.lopez" } │ si existe, está activo y tiene teléfono → guarda solicitud PENDIENTE
  │ ◄─ 202 (mismo cuerpo siempre) │                                            │
  │                               │ ◄── GET /password-recovery-requests ───────┤ ve la bandeja
  │                               │ ◄── POST /users/{id}/reset-password ───────┤ genera código nuevo
  │ ◄──────────── tú le pasas "usuario / codigoAcceso" por tu chat ────────────┤
  │ POST /auth/create-password …  │ (la solicitud queda atendida al emitir el código)
```

- `POST /api/v1/auth/password-recovery` (**público**): cuerpo `{ "username": "..." }`. Responde **siempre 202** con
  el mismo mensaje, exista o no el usuario (no revela qué cuentas existen). A lo mucho una solicitud pendiente por
  usuario (repetirla no duplica).
- `GET /api/v1/password-recovery-requests` (`USER_READ`): solicitudes pendientes, de la más antigua a la más
  reciente, con `userId`, `username`, nombre, `phone` y `requestedAt`.
- Atender = `POST /api/v1/users/{id}/reset-password` (ya existente): borra la contraseña actual, cierra sus sesiones,
  limpia el bloqueo por intentos y devuelve `oneTimeCode`/`shareMessage`. Marca la solicitud como atendida.
- Recomendación operativa: envía el código **al teléfono registrado** del usuario para que nadie te suplante pidiendo
  un reinicio.
- Tabla: `password_recovery_request` (migración V26); índice único parcial `uk_password_recovery_request_pending_per_user`.

## 3. Configuración

| Variable | Propiedad | Descripción |
|---|---|---|
| `OTP_DELIVERY` | `security.otp.delivery` | `manual` (defecto): el código se devuelve al admin. `whatsapp`: lo envía Meta |
| `OTP_TTL_MINUTES` | `security.otp.ttl-minutes` | 4320 (72 h). Con `whatsapp` conviene 30 |

## 4. Pendiente (no incluido)

- Registro abierto con solicitud de acceso aprobada por el admin (`onboarding_request`, ver
  [plan-onboarding-registration.md](plan-onboarding-registration.md)); el código muerto de `RegisterUserService`
  sigue sin eliminarse.
- Pantallas de administración en el frontend (botón "Copiar", bandeja de recuperaciones).
