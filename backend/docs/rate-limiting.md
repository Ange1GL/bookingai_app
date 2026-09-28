# Rate limiting — guía de `RateLimitFilter`

Este documento explica qué problema resuelve el rate limiting, cómo está implementado con Bucket4j + Caffeine, y por qué está posicionado donde está en la cadena de filtros. Complementa a `docs/security-config.md` (que explica `SecurityConfig` en general) y `docs/jwt-authentication-filter.md`.

---

## 1. Qué problema resuelve

Sin rate limiting, cualquier cliente (o atacante) puede golpear la API tantas veces por segundo como su ancho de banda le permita. Esto es especialmente peligroso en `/api/v1/auth/login`: es un endpoint público (`permitAll()`), no requiere JWT, así que es el blanco natural de un ataque de fuerza bruta (probar contraseñas o usernames a repetición). También protege endpoints autenticados contra un cliente (o token comprometido) que castiga la base de datos con requests repetidos.

La estrategia: limitar cuántas requests puede hacer una misma IP en una ventana de tiempo, y responder `429 Too Many Requests` cuando se excede — con el mismo formato de error JSON que ya usa el resto de la API (`ErrorResponse`), no un string armado a mano.

---

## 2. El algoritmo: Token Bucket (con refill "greedy")

`RateLimitFilter` usa [Bucket4j](https://bucket4j.com/) (`com.bucket4j:bucket4j_jdk17-core`), que implementa **token bucket**: cada IP tiene un balde con capacidad máxima de tokens (`security.rate-limit.capacity`, 60 por defecto). Cada request consume un token; si el balde está vacío, se rechaza.

Los tokens se recargan de forma continua con `refillGreedy(capacity, window)` — reparte la recarga tan pronto como el tiempo lo permite, en vez de esperar a que termine toda la ventana para recargar todo de golpe. Esto evita el efecto "doble ráfaga" del clásico *fixed window counter* (60 requests al final de un minuto + 60 al inicio del siguiente = 120 en un instante).

No usamos un `ProxyManager` distribuido de Bucket4j (Redis, Hazelcast, etc.): el `pom.xml` solo trae el core (`bucket4j_jdk17-core`), sin el módulo `bucket4j-caffeine`. Cada instancia de la app lleva su propio conteo en memoria — correcto para una sola instancia; si la app llegara a escalar horizontalmente, el límite dejaría de compartirse entre instancias y haría falta migrar a un backend distribuido.

---

## 3. La cache de Caffeine — por qué no hay fuga de memoria

```java
@Bean
public Cache<String, Bucket> rateLimitBucketCache(RateLimitProperties rateLimitProperties) {
    return Caffeine.newBuilder()
            .expireAfterAccess(rateLimitProperties.getCache().getExpireAfterAccess())
            .maximumSize(rateLimitProperties.getCache().getMaximumSize())
            .build();
}
```

Cada IP nueva crea una entrada `IP -> Bucket` en esta cache (`bucketCache.get(ip, key -> newBucket())`). Sin límites, una IP nueva por request (rotación de IPs, IPv6, o directamente spoofing) haría crecer la cache indefinidamente. Dos guardas:

- **`expireAfterAccess`** (5 minutos por defecto, configurable): libera el bucket de una IP que lleva un rato sin volver a pedir nada. Debe ser `>=` que `window` — si fuera menor, se liberaría un bucket que todavía debería recordar consumo dentro de su propia ventana.
- **`maximumSize`** (10 000 entradas por defecto): techo duro. Aunque llueva tráfico de IPs distintas más rápido de lo que `expireAfterAccess` puede liberar, Caffeine empieza a evictar las entradas menos usadas antes de crecer sin límite.

Estos valores viven en `RateLimitProperties` (`@ConfigurationProperties(prefix = "security.rate-limit")`), no como constantes en el filtro — mismo patrón que `CsrfProperties`/`CookieProperties`, permite ajustarlos por perfil sin tocar código.

---

## 4. Dónde vive en la cadena de filtros

```java
.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

`RateLimitFilter` corre **antes** que `JwtAuthenticationFilter`. Dos razones:

1. **Costo**: decodificar el JWT y consultar la revocación en BD/cache (`AccessTokenRevocationRepositoryPort.existsByJti`) no es gratis. Si una IP ya agotó su cupo, no tiene sentido pagar ese costo antes de rechazarla.
2. **Cobertura de `/login`**: `JwtAuthenticationFilter` dejaría pasar sin hacer nada una request a `/api/v1/auth/login` (no trae token, no hay nada que decodificar) — el filtro de rate limiting corre para *todas* las rutas, así que `/login` también queda protegido contra fuerza bruta, no solo los endpoints autenticados.

**¿Por qué no un `@Order(Ordered.HIGHEST_PRECEDENCE)` a nivel de contenedor (antes de todo, incluido CORS/CSRF)?** Este proyecto no usa `@Order` en ningún filtro (ver `docs/security-config.md`, sección 3.2) — todo el ordering se resuelve con `.addFilterBefore/After` dentro de `securityFilterChain()`, y ese es el único mecanismo que se sigue aquí también. Además, posicionarlo antes del `CorsFilter` real de Spring Security tendría un efecto secundario indeseado: una respuesta 429 saldría **sin headers CORS**, y el navegador la reportaría como error de red/CORS en vez de un 429 legible por el frontend. Con la posición actual (después de `CorsFilter`/`CsrfFilter`, que ya corren antes de `UsernamePasswordAuthenticationFilter` por defecto en Spring Security), el 429 ya lleva los headers CORS aplicados.

Efecto secundario aceptado: una request rechazada por CSRF (token `X-XSRF-TOKEN` inválido/ausente) nunca llega al rate limiter, así que no consume presupuesto de su bucket. No es un problema real — ese rechazo de CSRF ya es barato (no toca BD ni decodifica nada).

`RateLimitFilter` es `@Component` (necesita inyectar `RateLimitProperties`, la cache y `JsonMapper` por constructor) y por eso también necesita su propio `FilterRegistrationBean<RateLimitFilter>` con `setEnabled(false)` — mismo motivo exacto que `jwtFilterRegistration` (ver sección 3.2 de `docs/security-config.md`): sin él, Spring Boot lo registraría también a nivel de contenedor y correría dos veces por request.

### 4.1 Por qué el `Cache<String, Bucket>` vive en `RateLimitConfig`, no en `SecurityConfig`

```java
@Configuration
public class RateLimitConfig {

    @Bean
    public Cache<String, Bucket> rateLimitBucketCache(RateLimitProperties rateLimitProperties) { ... }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) { ... }
}
```

La primera versión de esto declaraba ambos beans directamente en `SecurityConfig`, junto a `jwtFilterRegistration`. Eso rompía el arranque: `SecurityConfig` depende de `RateLimitFilter` (campo final), y `RateLimitFilter` depende del `Cache<String, Bucket>` — si ese `Cache` es un `@Bean` de **instancia** dentro de la propia `SecurityConfig`, Spring necesita una instancia de `SecurityConfig` ya construida para invocar el método, pero construir esa instancia requiere primero a `RateLimitFilter`, que requiere el `Cache`, que requiere... `BeanCurrentlyInCreationException` al arrancar.

El primer arreglo fue declarar el método como `static` (un `@Bean` estático no necesita una instancia de la clase que lo declara para poder invocarse). Funciona, pero es frágil: es el único método `static` de toda la clase, y nada impide que alguien lo "normalice" quitándole el `static` sin entender por qué estaba ahí, reintroduciendo el ciclo. La solución definitiva es sacarlo a una clase (`RateLimitConfig`) que **no** depende de `RateLimitFilter` — el ciclo deja de ser posible estructuralmente, sin depender de que nadie recuerde una regla no escrita.

`SecurityConfig` conserva únicamente el campo `rateLimitFilter` y la línea `.addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)` en `securityFilterChain()` — eso sí tiene que vivir ahí, es donde se define la cadena de filtros.

---

## 5. El formato de la respuesta 429

```java
ErrorResponse body = new ErrorResponse(
        Instant.now().toString(),
        HttpStatus.TOO_MANY_REQUESTS.value(),
        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
        RATE_LIMIT_MESSAGE
);

response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
response.setContentType(JSON_CONTENT_TYPE);
jsonMapper.writeValue(response.getWriter(), body);
```

Mismo patrón que `SecurityEntryPoint.commence(...)` para el 401: se construye el `ErrorResponse` compartido (`infrastructure/adapter/in/rest/dto/ErrorResponse.java`) y se escribe manualmente con `JsonMapper` (Jackson 3, `tools.jackson.databind.json.JsonMapper`) — nada de strings JSON armados a mano. `RateLimitFilter` corre fuera del dispatch de Spring MVC (es un filtro, no un controller), así que no puede apoyarse en `GlobalExceptionHandler`/`@RestControllerAdvice`: esa pieza solo atrapa excepciones lanzadas dentro de un `@RestController`. Por el mismo motivo tampoco se agregó un caso `RATE_LIMIT_EXCEEDED` a `AuthError` — ningún servicio de aplicación va a lanzar esa excepción, sería un caso muerto en el enum.

Headers adicionales en la respuesta:

- **`Retry-After`** (estándar HTTP): segundos que el cliente debería esperar antes de reintentar. Se calcula desde `probe.getNanosToWaitForRefill()`.
- **`X-RateLimit-Remaining`** (convención de facto, no hay RFC — la misma que usan GitHub/Stripe/Twitter): en respuestas exitosas, cuántos tokens le quedan a esa IP en el balde actual.

Ambos headers están en `cors.exposed-headers` (`application.yaml`) — sin eso, el navegador los recibe pero el JS del frontend no puede leerlos en una respuesta cross-origin (`Access-Control-Expose-Headers` es obligatorio para headers no estándar).

---

## 6. Configuración (`application.yaml`)

```yaml
security:
  rate-limit:
    enabled: true
    capacity: 60        # peticiones permitidas por ventana, por IP
    window: 1m           # ventana de refill del bucket
    cache:
      expire-after-access: 5m   # debe ser >= window
      maximum-size: 10000
```

`security.rate-limit.enabled` permite apagar el filtro completo (útil para un perfil de test/dev si los límites molestan durante pruebas manuales repetidas) sin tocar código — se revisa en `RateLimitFilter.shouldNotFilter(...)`.

---

## 7. Caveat conocido: IP real detrás de un proxy

`RateLimitFilter` usa `request.getRemoteAddr()` para identificar al cliente — la misma fuente que ya usa `AuthController`/`AuthenticateUserUseCase` para el bloqueo de cuentas. Si la app llega a correr detrás de un reverse proxy o load balancer, este método devuelve la IP del proxy, no la del cliente real, a menos que se configure `server.forward-headers-strategy` y el proxy mande `X-Forwarded-For`/`Forwarded` de forma confiable. Fuera de alcance de la implementación actual — documentado aquí para no perderlo de vista si se cambia la topología de despliegue.
