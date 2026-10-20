# Plantilla de WhatsApp para enviar el código de un solo uso (OTP)

Guía de lo que hay que hacer **en Meta** (no en el código) para que el backend pueda enviar el código con el que un
usuario nuevo **crea su contraseña**. El código ya está listo (`WhatsAppOneTimeCodeNotifierAdapter`); falta la parte de
cuenta, número y plantilla. Cómo funciona el flujo completo: [otp-password-creation.md](otp-password-creation.md).

> Meta cambia los nombres de menús y reglas con frecuencia. Los pasos son los vigentes al escribir esto; si algo no
> coincide, busca el equivalente en la documentación oficial de la *WhatsApp Business Platform / Cloud API*.

## 1. ¿Por qué hace falta una plantilla?

WhatsApp solo deja que el negocio escriba **libremente** a una persona durante las **24 horas siguientes a que esa
persona le escribió**. Un usuario recién creado nunca nos ha escrito, así que el primer mensaje debe ser una
**plantilla de mensaje aprobada por Meta**. Fuera de esa ventana, el texto libre falla con el error `131047`.

Para códigos de un solo uso Meta tiene una categoría propia: **Autenticación** (*Authentication*). Es la que usa este
proyecto: su contenido es fijo (lo define Meta), lleva **un solo parámetro —el código—** y un botón **"Copiar código"**.
Esto es justo lo que necesitamos y evita el rechazo que tendría una plantilla genérica con contraseñas.

## 2. Qué necesitas tener (checklist)

| # | Requisito | Dónde |
|---|---|---|
| 1 | Un **Portfolio comercial de Meta** (Meta Business) | business.facebook.com |
| 2 | Una **app de Meta** de tipo *Business* con el producto **WhatsApp** agregado | developers.facebook.com → Mis apps |
| 3 | Una **cuenta de WhatsApp Business (WABA)** vinculada a esa app | App → WhatsApp → Configuración de la API |
| 4 | Un **número de teléfono** registrado en la Cloud API, con su `phone_number_id` | misma pantalla |
| 5 | **Nombre para mostrar** del número aprobado | WhatsApp Manager → Números de teléfono |
| 6 | Un **método de pago** en la WABA (las plantillas enviadas por el negocio se cobran) | Business Settings / WhatsApp Manager → Facturación |
| 7 | Un **usuario del sistema** con **token permanente** | Business Settings → Usuarios → Usuarios del sistema |
| 8 | La **plantilla de autenticación aprobada** (sección 5) | WhatsApp Manager → Plantillas de mensajes |

Notas sobre el número:

- El número debe poder recibir un código de verificación (SMS o llamada) y **no puede estar en uso a la vez en la app
  normal de WhatsApp / WhatsApp Business**, salvo que uses el modo de coexistencia que ofrezca Meta. Si ya usas ese
  número en el celular, decide primero si lo migras a la API o consigues otro número dedicado.
- Para pruebas, Meta da un **número de prueba** gratuito. Con él solo puedes enviar a un máximo de **5 destinatarios**
  que verifiques en el panel (si no, error `131030`). Sirve para validar el flujo, no para producción.
- La **verificación del negocio** (documentos de la empresa) sube los límites de mensajes y es necesaria para ciertas
  funciones. Conviene iniciarla pronto porque tarda días.

## 3. Obtener el `phone_number_id` y el ID de la cuenta (WABA)

1. App de Meta → **WhatsApp → Configuración de la API** (*API Setup*).
2. Selecciona el número remitente. Ahí aparecen:
   - **Identificador del número de teléfono** → es el `phone_number_id` (solo dígitos).
   - **Identificador de la cuenta de WhatsApp Business** → el `waba_id` (lo usarás para consultar plantillas).

El `phone_number_id` va en la variable `WHATSAPP_OTP_PHONE_NUMBER_ID`. No es el número de teléfono en sí.

## 4. Crear el token permanente

El token temporal del panel dura ~24 h. Para el backend necesitas uno permanente:

1. Business Settings → **Usuarios → Usuarios del sistema → Agregar** (rol *Administrador* o *Empleado*).
2. **Asignar activos** → tu app y tu cuenta de WhatsApp, con control total.
3. **Generar token** → elige la app y marca los permisos
   `whatsapp_business_messaging` y `whatsapp_business_management`. Elige que **no expire**.
4. Guárdalo como variable de entorno `SYSTEM_USER_EMPLOYEE_META` (la misma que ya usa el agente de clientes).

Nunca lo pegues en el código ni en el repositorio. En QA va en Parameter Store.

## 5. Crear la plantilla de autenticación

WhatsApp Manager → **Plantillas de mensajes → Crear plantilla**.

| Campo | Valor |
|---|---|
| **Categoría** | **Autenticación** (*Authentication*) |
| **Nombre** | `codigo_verificacion` (minúsculas, números y guion bajo; es el valor de `WHATSAPP_OTP_TEMPLATE_NAME`) |
| **Idioma** | **Español** (`es`) o **Español (México)** (`es_MX`). Ver el aviso de abajo |
| **Tipo de código / botón** | **Copiar código** (*Copy code*) |
| **Recomendación de seguridad** | Activada ("No compartas este código") |
| **Vencimiento del código** | **30 minutos**, igual que `security.otp.ttl-minutes` (el texto "este código vence en N minutos" es informativo; el vencimiento real lo aplica el backend) |

El cuerpo lo genera Meta (algo como "*483920* es tu código de verificación. Por tu seguridad, no lo compartas.").
**No se puede escribir texto propio ni agregar el nombre de usuario**: por eso el usuario debe conocer su `username`
(se lo comunica el admin) y escribirlo junto con el código en la pantalla "Crear contraseña".

- El código tiene **un solo parámetro**; el backend lo manda dos veces (cuerpo y botón), como exige este tipo de
  plantilla. Nuestros códigos son de 6 dígitos (el máximo permitido por Meta es 15 caracteres).
- Envía la plantilla a revisión. Las de autenticación suelen aprobarse en minutos, pero puede tardar hasta ~24 h.
  El estado pasa de *En revisión* a *Aprobada* (o *Rechazada*, con el motivo).

> **Aviso sobre el idioma:** el código de idioma con el que se envía debe ser **idéntico** al de la plantilla.
> El backend usa `whatsapp.cloud.otp-template-language` (por defecto `es`). Si creaste la plantilla como `es_MX`,
> cambia esa propiedad a `es_MX` en `application.yaml`; si no coinciden, Meta responde `132001`.

## 6. Configurar el backend

Variables de entorno (o `.env` en local, **que no se commitea**):

```properties
SYSTEM_USER_EMPLOYEE_META=<token permanente>
WHATSAPP_OTP_PHONE_NUMBER_ID=<phone_number_id>
WHATSAPP_OTP_TEMPLATE_NAME=codigo_verificacion
```

Reinicia la app. En el log de arranque debe aparecer
`Envío de códigos de un solo uso por plantilla de WhatsApp habilitado`. Si aparece
`WhatsApp de códigos sin configurar`, falta alguna de las tres variables. A partir de ahí, crear un usuario o usar
`reset-password` responde `"codeSent": true` cuando Meta acepta el mensaje.

## 7. Probar antes de usar el backend (opcional)

Con `curl`, para comprobar que el token, el número y la plantilla funcionan (sustituye los marcadores):

```bash
curl -X POST "https://graph.facebook.com/v26.0/<PHONE_NUMBER_ID>/messages" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "messaging_product": "whatsapp",
    "to": "<TELEFONO_DESTINO_SOLO_DIGITOS>",
    "type": "template",
    "template": {
      "name": "codigo_verificacion",
      "language": { "code": "es" },
      "components": [
        { "type": "body", "parameters": [ { "type": "text", "text": "123456" } ] },
        { "type": "button", "sub_type": "url", "index": "0", "parameters": [ { "type": "text", "text": "123456" } ] }
      ]
    }
  }'
```

Para ver el estado de la plantilla por API: `GET https://graph.facebook.com/v26.0/<WABA_ID>/message_templates?name=codigo_verificacion`.

## 8. Errores frecuentes

| Código | Significado | Qué hacer |
|---|---|---|
| `132001` | La plantilla no existe con ese nombre **e idioma** | Revisa nombre y `otp-template-language`; la plantilla debe estar *Aprobada* |
| `132000` | Número de parámetros distinto al de la plantilla | Debe ser un parámetro en el cuerpo y uno en el botón |
| `131047` | Se intentó texto libre fuera de la ventana de 24 h | Solo plantillas; no uses el flujo de respuestas del agente para esto |
| `131030` | Destinatario no permitido (número de prueba) | Agrega y verifica el destino en el panel, o usa un número de producción |
| `131026` | Mensaje no entregable | El teléfono no tiene WhatsApp o está mal escrito (formato internacional, solo dígitos) |
| `190` / `401` | Token vencido o inválido | Genera un token permanente de usuario del sistema |
| `100` | Parámetro inválido | Revisa `phone_number_id` (debe ser numérico) y el cuerpo |

Si Meta rechaza el mensaje, el backend **no** revierte el usuario: queda creado (sin contraseña), responde
`codeSent: false` y se reintenta con `POST /api/v1/users/{id}/reset-password`. En el log solo aparece el código
de error de Meta; el OTP nunca se escribe.

## 9. Mientras la plantilla no esté aprobada

El flujo sigue funcionando: se crea el usuario, la API responde `codeSent: false` y, **solo en el perfil dev**,
devuelve el código en la respuesta (`oneTimeCode`) para poder probar la pantalla "Crear contraseña" sin WhatsApp
(`admin.users.expose-one-time-code`).

## 10. Resumen rápido

1. Tener Meta Business + app + WABA + número con método de pago.
2. Crear el token permanente de usuario del sistema.
3. Crear la plantilla `codigo_verificacion` (Autenticación, Copiar código, 30 min) y esperar la aprobación.
4. Configurar `SYSTEM_USER_EMPLOYEE_META`, `WHATSAPP_OTP_PHONE_NUMBER_ID` y `WHATSAPP_OTP_TEMPLATE_NAME`
   (y el idioma si no es `es`).
5. Crear un usuario de prueba y comprobar `codeSent: true`.
