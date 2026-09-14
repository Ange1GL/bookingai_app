# Decisión: mantener CSRF (`csrf.spa()`) junto con `SameSite=Lax`

**Fecha:** 2026-09-14
**Estado:** Decidido — se mantiene tal como está configurado hoy.

---

## Contexto

El login/register/refresh no devuelven el JWT en el body de la respuesta: el `access_token` y el `refresh_token` viajan únicamente como cookies `HttpOnly` (`AuthCookieFactory.java:34-52`, escritas en `AuthController.java:87-88`). Esto es necesario para que el JS del frontend nunca pueda leer el token (mitiga XSS), pero tiene la contrapartida clásica: el navegador reenvía las cookies automáticamente en cualquier request al backend, incluidas las originadas por sitios ajenos — el vector clásico de CSRF.

Por eso `SecurityConfig.java:60` habilita `.csrf(csrf -> csrf.spa())`, que arma un `CookieCsrfTokenRepository` (cookie `XSRF-TOKEN`, legible por JS) + `CsrfCookieFilter.java` para forzar su emisión en cada response. El cliente debe leer esa cookie y reenviar su valor en el header `X-XSRF-TOKEN` en cada request que muta estado.

## Pregunta

Ya usamos `security.cookie.same-site: Lax` (`application.yaml:55`). `SameSite=Lax` ya bloquea que un sitio cross-origin arrastre las cookies en requests automáticos (POST cross-site vía formulario, fetch, `<img>`, etc.). Dado que el único cliente real es la SPA Angular propia, ¿el CSRF completo (`csrf.spa()` + cookie `XSRF-TOKEN` + `CsrfCookieFilter`) es una capa redundante que se podría quitar para simplificar?

## Análisis

1. **`SameSite=Lax` no es protección completa.** Bloquea la mayoría de submissions automáticos cross-site, pero sigue permitiendo que la cookie viaje en navegación top-level (un link/redirect que dispare un GET) y en algunos flujos de terceros. No cubre el caso en que, más adelante, el atributo se tenga que relajar a `SameSite=None` — necesario si frontend y backend terminan sirviéndose desde dominios completamente distintos en producción (hoy en dev ya son puertos distintos: `localhost:4200` vs `localhost:8082`, lo que obligó a `allow-credentials: true` en CORS, `application.yaml:79`).
2. **Hoy no hay same-origin real, solo same-site.** Si en producción el backend termina en un dominio distinto al del frontend (subdominio de API, por ejemplo), `SameSite=Strict` podría no ser viable y CSRF vuelve a ser la única defensa activa.
3. **Referencia externa comparable.** El proyecto público [`dentalpin/dentalpin` (PR #394)](https://github.com/dentalpin/dentalpin/pull/394) implementa el mismo patrón exacto: cookies `HttpOnly` para access/refresh + cookie de doble-submit legible por JS + `SameSite=Lax`. Su propia documentación es explícita: mantienen el CSRF de doble-submit **a propósito**, tratando `SameSite=Lax` como una capa de defensa adicional y no como protección suficiente por sí sola. Mismo dilema, misma conclusión.
4. **Costo de mantenerlo es bajo.** El único costo real es el `GET` previo que el cliente debe hacer para obtener `XSRF-TOKEN` antes del primer request mutante (la SPA ya lo resuelve en el arranque de la app; el único punto de fricción es al probar manualmente con herramientas como Postman, que no copian automáticamente una cookie a un header custom — ver nota abajo).

## Decisión

**Se mantiene `csrf.spa()` sin cambios.** No se elimina ni se debilita a `SameSite=Strict`-only. El razonamiento queda documentado aquí para que no se vuelva a cuestionar o parchear sin contexto (evitar el patrón de iteración orgánica sin documentar que ya se ve en los comentarios de `SecurityConfig.java:47-59` y `JwtConfig.java:61-63`).

## Nota sobre fricción al probar (Postman)

La causa de que el login "no deje" probarse cómodamente en Postman es este mismo flujo de CSRF de dos pasos: hace falta un `GET` previo (p. ej. a `/api/v1/auth/login`) para que el backend emita `XSRF-TOKEN`, y luego copiar ese valor al header `X-XSRF-TOKEN` del `POST` de login — Postman reenvía cookies automáticamente pero no copia una cookie a un header por sí solo. A eso se suma que `security.cookie.secure: true` (`application.yaml:54`) está fijo sin diferenciar por entorno, y sobre `http://localhost` los clientes tipo navegador/Postman no reenvían cookies `Secure` en peticiones no-HTTPS. La solución concreta (perfil `dev` con `secure=false`, y/o una colección Postman con pre-request script) queda fuera del alcance de esta decisión — pendiente para una pasada futura si se pide explícitamente.
