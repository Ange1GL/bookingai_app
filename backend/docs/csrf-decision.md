# Decisión: mantener CSRF (`csrf.spa()`) junto con `SameSite=Lax`

**Fecha:** 2026-09-14
**Estado:** Decidido — se mantiene tal como está configurado hoy.

---

## Contexto

El login/register/refresh no devuelven el JWT en el body de la respuesta: el `access_token` y el `refresh_token` viajan únicamente como cookies `HttpOnly` (`AuthCookieFactory.java:34-52`, escritas en `AuthController.java:87-88`). Esto es necesario para que el JS del frontend nunca pueda leer el token (mitiga XSS), pero tiene la contrapartida clásica: el navegador reenvía las cookies automáticamente en cualquier request al backend, incluidas las originadas por sitios ajenos — el vector clásico de CSRF.

Por eso `SecurityConfig.java:60` habilita `.csrf(csrf -> csrf.spa())`, que arma un `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN`, legible por JS) + `CsrfCookieFilter.java` para forzar su emisión en cada response. El cliente debe leer esa cookie y reenviar su valor en el header `X-XSRF-TOKEN` en cada request que muta estado.

## Pregunta

Ya usamos `security.cookie.same-site: Lax` (`application.yaml:60`). `SameSite=Lax` ya bloquea que un sitio cross-origin arrastre las cookies en requests automáticos (POST cross-site vía formulario, fetch, `<img>`, etc.). Dado que el único cliente real es la SPA Angular propia, ¿el CSRF completo (`csrf.spa()` + cookie `XSRF-TOKEN` + `CsrfCookieFilter`) es una capa redundante que se podría quitar para simplificar?

## Análisis

1. **`SameSite=Lax` no es protección completa.** Bloquea la mayoría de submissions automáticos cross-site, pero sigue permitiendo que la cookie viaje en navegación top-level (un link/redirect que dispare un GET) y en algunos flujos de terceros. No cubre el caso en que, más adelante, el atributo se tenga que relajar a `SameSite=None` — necesario si frontend y backend terminan sirviéndose desde dominios completamente distintos en producción (hoy en dev ya son puertos distintos: `localhost:4200` vs `localhost:8082`, lo que obligó a `allow-credentials: true` en CORS, `application.yaml:87`).
2. **Hoy no hay same-origin real, solo same-site.** Si en producción el backend termina en un dominio distinto al del frontend (subdominio de API, por ejemplo), `SameSite=Strict` podría no ser viable y CSRF vuelve a ser la única defensa activa.
3. **Referencia externa comparable.** El proyecto público [`dentalpin/dentalpin` (PR #394)](https://github.com/dentalpin/dentalpin/pull/394) implementa el mismo patrón exacto: cookies `HttpOnly` para access/refresh + cookie de doble-submit legible por JS + `SameSite=Lax`. Su propia documentación es explícita: mantienen el CSRF de doble-submit **a propósito**, tratando `SameSite=Lax` como una capa de defensa adicional y no como protección suficiente por sí sola. Mismo dilema, misma conclusión.
4. **Costo de mantenerlo es bajo.** El único costo real es el `GET` previo que el cliente debe hacer para obtener `XSRF-TOKEN` antes del primer request mutante (la SPA ya lo resuelve en el arranque de la app; el único punto de fricción es al probar manualmente con herramientas como Postman, que no copian automáticamente una cookie a un header custom — ver nota abajo).

## Decisión

**Se mantiene `csrf.spa()` en `qa` y en cualquier entorno real (sin perfil, o `qa`).** No se elimina ni se debilita a `SameSite=Strict`-only ahí. El razonamiento queda documentado aquí para que no se vuelva a cuestionar o parchear sin contexto (evitar el patrón de iteración orgánica sin documentar que ya se ve en los comentarios de `SecurityConfig.java:47-59` y `JwtConfig.java:61-63`).

### Actualización — perfil `dev` (2026-09-14)

Se agregó `security.csrf.enabled` (`CsrfProperties.java`, leído en `SecurityConfig.java`) para poder **desactivar CSRF únicamente en el perfil `dev`**, junto con `security.cookie.secure: false` en ese mismo perfil. Ambos flags viven en archivos de perfil separados: [`application-dev.yaml`](../src/main/resources/application-dev.yaml) y [`application-qa.yaml`](../src/main/resources/application-qa.yaml) (la base `application.yaml` ya no define `security.cookie.secure` ni `security.csrf.enabled` — son específicos de cada perfil). `dev` es además el **perfil activo por defecto** (`application.yaml:8-9`), así que hay que sobreescribirlo explícitamente (`SPRING_PROFILES_ACTIVE=qa`) para levantar el backend con el comportamiento de un entorno real.

Como fail-safe adicional, `CookieProperties.secure` (Java) tiene default `true` — si algún día se activa un tercer perfil sin `application-<perfil>.yaml` propio, las cookies siguen saliendo `Secure` en vez de caer silenciosamente al default de Java (`false`).

El objetivo de todo esto es exclusivamente eliminar la fricción de probar el login y endpoints protegidos con Postman en una máquina local (ver [postman-testing-guide.md](postman-testing-guide.md)) — **no cambia la decisión de arriba para `qa`/producción**, que sigue teniendo CSRF completo y cookies `Secure=true`. El perfil `dev` nunca debe activarse fuera de una máquina de desarrollo local.

## Nota sobre fricción al probar (Postman)

La causa de que el login "no deje" probarse cómodamente en Postman con el perfil `qa` es este mismo flujo de CSRF de dos pasos: hace falta un `GET` previo (p. ej. a `/api/v1/auth/login`) para que el backend emita `XSRF-TOKEN`, y luego copiar ese valor al header `X-XSRF-TOKEN` del `POST` de login — Postman reenvía cookies automáticamente pero no copia una cookie a un header por sí solo. A eso se suma que `security.cookie.secure: true` está fijo en ese modo, y sobre `http://localhost` los clientes tipo navegador/Postman no reenvían cookies `Secure` en peticiones no-HTTPS. Para desarrollo local, usa el perfil `dev` (ver arriba) en vez de repetir este flujo manual cada vez.
