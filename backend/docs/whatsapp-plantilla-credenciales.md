# Plantilla de WhatsApp para enviar la contraseña temporal

Guía de lo que hay que hacer **en Meta** (no en el código) para que el backend pueda enviar la contraseña temporal
a un usuario nuevo. El código ya está listo (`WhatsAppCredentialNotifierAdapter`); falta la parte de cuenta,
número y plantilla. Contexto técnico: [admin-users-roles.md](admin-users-roles.md#configuración-de-whatsapp-para-credenciales).

> Meta cambia los nombres de menús y reglas con frecuencia. Los pasos son los vigentes al escribir esto; si algo no
> coincide, busca el equivalente en la documentación oficial de la *WhatsApp Business Platform / Cloud API*.

## 1. ¿Por qué hace falta una plantilla?

WhatsApp solo deja que el negocio escriba **libremente** a una persona durante las **24 horas siguientes a que esa
persona le escribió** (la "ventana de servicio al cliente"). Un usuario recién creado nunca nos ha escrito, así que
el primer mensaje debe ser una **plantilla de mensaje aprobada por Meta**. Si se intenta enviar texto libre fuera de
la ventana, Meta responde con el error `131047`.

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
| 8 | La **plantilla aprobada** (esta guía) | WhatsApp Manager → Plantillas de mensajes |

Notas sobre el número:

- El número debe poder recibir un código de verificación (SMS o llamada) y **no puede estar en uso a la vez en la app
  normal de WhatsApp / WhatsApp Business**, salvo que uses el modo de coexistencia que ofrezca Meta. Si ya usas ese
  número en el celular, decide primero si lo migras a la API o consigues otro número dedicado.
- Para pruebas, Meta da un **número de prueba** gratuito. Con él solo puedes enviar a un máximo de **5 destinatarios**
  que verifiques en el panel (si no, error `131030`). Sirve para validar el flujo, no para producción.
- La **verificación del negocio** (documentos de la empresa) no siempre bloquea la primera plantilla, pero sí sube
  los límites de mensajes y es necesaria para ciertas funciones. Conviene iniciarla pronto porque tarda días.

## 3. Obtener el `phone_number_id` y el ID de la cuenta (WABA)

1. App de Meta → **WhatsApp → Configuración de la API** (*API Setup*).
2. Selecciona el número remitente. Ahí aparecen:
   - **Identificador del número de teléfono** → es el `phone_number_id` (solo dígitos).
   - **Identificador de la cuenta de WhatsApp Business** → el `waba_id` (lo usarás para consultar plantillas).

El `phone_number_id` va en la variable `WHATSAPP_CREDENTIALS_PHONE_NUMBER_ID`.
No es el número de teléfono en sí.

## 4. Crear el token permanente

El token temporal del panel dura ~24 h. Para el backend necesitas uno permanente:

1. Business Settings → **Usuarios → Usuarios del sistema → Agregar** (rol *Administrador* o *Empleado*).
2. **Asignar activos** → tu app y tu cuenta de WhatsApp, con control total.
3. **Generar token** → elige la app y marca los permisos
   `whatsapp_business_messaging` y `whatsapp_business_management`. Elige que **no expire**.
4. Guárdalo como variable de entorno `SYSTEM_USER_EMPLOYEE_META` (la misma que ya usa el agente de clientes).

Nunca lo pegues en el código ni en el repositorio (ver `no-secrets-in-code`). En QA va en Parameter Store.

## 5. Crear la plantilla

WhatsApp Manager → **Plantillas de mensajes → Crear plantilla**.

| Campo | Valor recomendado |
|---|---|
| **Categoría** | **Utilidad** (*Utility*). Ver la nota de la sección 7 sobre *Autenticación* |
| **Nombre** | `credenciales_usuario` (minúsculas, números y guion bajo; es el valor de `WHATSAPP_CREDENTIALS_TEMPLATE_NAME`) |
| **Idioma** | **Español** (`es`) o **Español (México)** (`es_MX`). Ver el aviso de abajo |
| **Encabezado / pie / botones** | Ninguno (el backend solo envía los parámetros del cuerpo) |

**Cuerpo** (exactamente dos variables, en este orden):

```text
Hola, tu cuenta en BookingApp ya está lista.
Usuario: {{1}}
Contraseña temporal: {{2}}
Deberás cambiarla la primera vez que inicies sesión. No compartas esta contraseña con nadie.
```

- `{{1}}` = nombre de usuario, `{{2}}` = contraseña temporal. **El orden y la cantidad importan**: el backend manda
  `[username, contraseña]`.
- Meta pide **valores de ejemplo** para cada variable al enviar la plantilla a revisión; pon datos ficticios
  (`maria.lopez`, `Ab3dE6gH9jKm`), nunca una contraseña real.
- Reglas habituales del cuerpo: no puede empezar ni terminar con una variable, no debe haber variables seguidas, y el
  texto fijo debe ser suficiente respecto al número de variables.

> **Aviso sobre el idioma:** el código de idioma con el que se envía debe ser **idéntico** al de la plantilla.
> El backend usa `whatsapp.cloud.credentials-template-language` (por defecto `es`). Si creaste la plantilla como
> `es_MX`, cambia esa propiedad a `es_MX` (en `application.yaml`); si no coinciden, Meta responde `132001`
> ("la plantilla no existe en ese idioma").

Envía a revisión. Suele aprobarse en minutos, pero puede tardar hasta ~24 h. El estado pasa de *En revisión* a
*Aprobada* (o *Rechazada*, con el motivo).

## 6. Configurar el backend

Variables de entorno (o `.env` en local, **que no se commitea**):

```properties
SYSTEM_USER_EMPLOYEE_META=<token permanente>
WHATSAPP_CREDENTIALS_PHONE_NUMBER_ID=<phone_number_id>
WHATSAPP_CREDENTIALS_TEMPLATE_NAME=credenciales_usuario
```

Reinicia la app. En el log de arranque debe aparecer
`Envío de contraseñas temporales por plantilla de WhatsApp habilitado`. Si aparece
`WhatsApp de credenciales sin configurar`, falta alguna de las tres variables. A partir de ahí, crear un usuario o
usar `reset-password` responde `"credentialsSent": true` cuando Meta acepta el mensaje.

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
      "name": "credenciales_usuario",
      "language": { "code": "es" },
      "components": [{ "type": "body", "parameters": [
        { "type": "text", "text": "usuario.prueba" },
        { "type": "text", "text": "Clave-de-prueba-1" }
      ]}]
    }
  }'
```

Para ver el estado de la plantilla por API: `GET https://graph.facebook.com/v26.0/<WABA_ID>/message_templates?name=credenciales_usuario`.

## 8. Errores frecuentes

| Código | Significado | Qué hacer |
|---|---|---|
| `132001` | La plantilla no existe con ese nombre **e idioma** | Revisa nombre y `credentials-template-language`; la plantilla debe estar *Aprobada* |
| `132000` | Número de parámetros distinto al de la plantilla | La plantilla debe tener exactamente 2 variables de cuerpo |
| `131047` | Se intentó texto libre fuera de la ventana de 24 h | Solo plantillas; no uses el flujo de respuestas del agente para esto |
| `131030` | Destinatario no permitido (número de prueba) | Agrega y verifica el destino en el panel, o usa un número de producción |
| `131026` | Mensaje no entregable | El teléfono no tiene WhatsApp o está mal escrito (formato internacional, solo dígitos) |
| `190` / `401` | Token vencido o inválido | Genera un token permanente de usuario del sistema |
| `100` | Parámetro inválido | Revisa `phone_number_id` (debe ser numérico) y el cuerpo |

Si Meta rechaza el mensaje, el backend **no** revierte el usuario: queda creado, responde `credentialsSent: false`
y se reintenta con `POST /api/v1/admin/users/{id}/reset-password`. En el log solo aparece el código de error de Meta;
la contraseña nunca se escribe.

## 9. Posible rechazo de la plantilla y alternativas

Meta es estricto con las plantillas que contienen **credenciales**. Una plantilla de *Utilidad* con usuario y
contraseña puede ser rechazada o recategorizada. Si pasa, hay dos salidas:

1. **Pedir revisión** con otro texto (por ejemplo, no llamar "contraseña" a la variable en el texto de ejemplo y
   dejar claro que es temporal y de un solo uso).
2. **Usar la categoría *Autenticación*** (*Authentication*), pensada para códigos de un solo uso. Su cuerpo es fijo
   (lo define Meta), admite **un solo parámetro** (el código, hasta 15 caracteres; la contraseña temporal de 12
   caracteres cabe) y lleva un botón "Copiar código". Esa plantilla **no puede incluir el nombre de usuario**, así que
   habría que comunicarlo por otro medio (por ejemplo, que el usuario inicie sesión con su email o que el admin se lo
   diga). Además requiere **cambiar el adaptador**: este tipo exige un componente de botón con el mismo código, que
   hoy `WhatsAppCredentialNotifierAdapter` no envía. Si eliges este camino, pídelo y se implementa.

Mientras la plantilla no esté aprobada, el flujo sigue funcionando: se crea el usuario, la API responde
`credentialsSent: false` y, **solo en el perfil dev**, devuelve la contraseña temporal en la respuesta para poder
probar (`admin.users.expose-temporary-password`).

## 10. Resumen rápido

1. Tener Meta Business + app + WABA + número con método de pago.
2. Crear el token permanente de usuario del sistema.
3. Crear la plantilla `credenciales_usuario` (Utilidad, 2 variables) y esperar la aprobación.
4. Configurar `SYSTEM_USER_EMPLOYEE_META`, `WHATSAPP_CREDENTIALS_PHONE_NUMBER_ID` y
   `WHATSAPP_CREDENTIALS_TEMPLATE_NAME` (y el idioma si no es `es`).
5. Crear un usuario de prueba y comprobar `credentialsSent: true`.
