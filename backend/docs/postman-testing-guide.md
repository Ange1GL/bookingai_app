# Cómo probar el login y endpoints protegidos en Postman (cookies httpOnly + CSRF)

Esta guía explica por qué el flujo actual de autenticación (JWT en cookies `HttpOnly` + CSRF de doble-submit) requiere un paso extra al probar manualmente en Postman, y da los pasos exactos para hacerlo sin usar Pre-request/Test scripts. Complementa [csrf-decision.md](csrf-decision.md) (por qué se mantiene CSRF) y la referencia de implementación en [jwt-spring-security-guide.md](jwt-spring-security-guide.md).

---

## Por qué el `Path` de las cookies usa el prefijo `/api`

Todos los controladores REST del proyecto exponen sus rutas bajo `/api/v1/...` — no hay `server.servlet.context-path` global, cada uno define su propio `@RequestMapping`:

- [AuthController.java:31](../src/main/java/com/github/angellariosacosta/bookingapp/infrastructure/adapter/in/rest/controller/AuthController.java) → `/api/v1/auth`
- [AppointmentController.java:23](../src/main/java/com/github/angellariosacosta/bookingapp/infrastructure/adapter/in/rest/controller/AppointmentController.java) → `/api/v1/appointments`
- [CustomerController.java:20](../src/main/java/com/github/angellariosacosta/bookingapp/infrastructure/adapter/in/rest/controller/CustomerController.java) → `/api/v1/customers`
- [AgentController.java:21](../src/main/java/com/github/angellariosacosta/bookingapp/infrastructure/adapter/in/rest/controller/AgentController.java) → `/api/v1/agent`

El atributo `Path` de una cookie le dice al cliente (navegador o Postman) "solo reenvía esta cookie en requests cuya URL empiece con este prefijo". Configurado en `application.yaml:56-63` y aplicado en [AuthCookieFactory.java:34-42](../src/main/java/com/github/angellariosacosta/bookingapp/infrastructure/security/util/AuthCookieFactory.java):

| Cookie | Path | Razón |
|---|---|---|
| `access_token` | `/api` | Prefijo común mínimo que cubre **todos** los controladores reales del proyecto. Evita mandar el JWT en rutas que no son de la API (estáticos, `/error`, futuros endpoints de Actuator/docs). Cualquier controller nuevo bajo `/api/v1/...` queda cubierto automáticamente, sin tocar esta config. |
| `refresh_token` | `/api/v1/auth/refresh` | Acotado a un único endpoint, porque este token vive 30 días (vs 15 min del access token) — es más sensible, así que se minimiza su "blast radius" al mínimo necesario: solo el endpoint que realmente lo necesita. |

No se usó `Path=/` (raíz) a propósito: mandaría la cookie en cualquier request al dominio, ampliando la exposición sin ninguna ganancia.

---

## Por qué Postman no puede loguearte "de una"

El login no devuelve el JWT en el body — viaja solo en cookies `HttpOnly` (`access_token`, `refresh_token`), lo cual obliga a mantener CSRF de doble-submit (`csrf.spa()` en [SecurityConfig.java:60](../src/main/java/com/github/angellariosacosta/bookingapp/infrastructure/config/SecurityConfig.java), decisión documentada en [csrf-decision.md](csrf-decision.md)). Esto significa que **todo `POST`/`PUT`/`PATCH`/`DELETE`** exige un header `X-XSRF-TOKEN` que coincida con el valor de la cookie `XSRF-TOKEN` que el propio backend emite.

Postman reenvía cookies automáticamente (como un navegador), pero **no copia solo una cookie a un header custom** — eso hay que hacerlo a mano si no se usan scripts.

Dos cookies que vas a ver y que **no** se configuran en `application.yaml`, aunque lo parezca:

- `XSRF-TOKEN`: la genera el `CookieCsrfTokenRepository` interno de Spring Security (activado por `csrf.spa()`), no `CookieProperties`/`AuthCookieFactory`. Por diseño sale **siempre** `HttpOnly=false` (el frontend necesita leerla) y `Secure` se calcula según si el request llegó por HTTPS — sobre `http://localhost` siempre da `Secure=false`, aunque `application.yaml:54` tenga `secure: true` (esa propiedad solo aplica a `access_token`/`refresh_token`).

---

## Pasos en Postman (sin Pre-request/Test scripts)

1. **Warm-up CSRF**: manda un `GET http://localhost:8082/api/v1/auth/login`. Va a dar `405 Method Not Allowed` (no hay handler GET) — es esperado, ignóralo. Lo que importa es que los filtros de seguridad ya corrieron y la cookie `XSRF-TOKEN` llega en la respuesta antes de que Spring decida la ruta.
2. En la pestaña **Cookies** de esa respuesta, copia el **Value** completo de `XSRF-TOKEN`.
3. Ve al request `POST /api/v1/auth/login` → pestaña **Headers** (NO Cookies) → agrega:
   - Key: `X-XSRF-TOKEN`
   - Value: el valor que copiaste en el paso 2
4. Confirma que ese request tenga activo **"Send cookies automatically"** (pestaña Settings / engranaje del request), para que Postman mande también `Cookie: XSRF-TOKEN=...` solo.
5. Envía el login → `200 OK` con `Set-Cookie` para `access_token` y `refresh_token`.
6. Para endpoints protegidos con métodos mutantes (POST/PUT/PATCH/DELETE), repite el header `X-XSRF-TOKEN` con el **mismo valor** del paso 2 (el token CSRF no rota en cada request, se mantiene mientras la cookie `XSRF-TOKEN` viva). Los GET no necesitan este header.

### Error común: crear `X-XSRF-TOKEN` como cookie en vez de header

Si en el modal "Manage Cookies" agregas manualmente una cookie llamada `X-XSRF-TOKEN`, **no funciona** — Spring Security busca un header HTTP con ese nombre, no una cookie. El resultado es `403 Forbidden` porque el header nunca llegó y el valor de esa cookie extra no coincide con `XSRF-TOKEN`. La corrección es siempre el paso 3 de arriba: header, no cookie.

---

## Referencia: cómo lo resuelve `scripts/test-auth-flow.mjs`

El script en [`scripts/test-auth-flow.mjs`](../scripts/test-auth-flow.mjs) automatiza exactamente estos mismos pasos (líneas 58-78): hace el GET de warm-up, lee `XSRF-TOKEN` de su propio cookie-jar en memoria, y lo copia al header `X-XSRF-TOKEN` del POST de login. Es el mismo mecanismo, solo que a mano en Postman en vez de en código.
