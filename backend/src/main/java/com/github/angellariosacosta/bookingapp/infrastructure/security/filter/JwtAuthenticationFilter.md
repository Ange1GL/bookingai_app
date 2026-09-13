# `JwtAuthenticationFilter`

Filtro de infraestructura (`infrastructure.security.filter`) que intercepta **cada** petición HTTP antes de que llegue a cualquier `@RestController`, para intentar autenticar al usuario a partir de un JWT.

---

## ¿Por qué extiende `OncePerRequestFilter` y qué son esos 3 parámetros de `doFilterInternal`?

```java
protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
) throws ServletException, IOException
```

Esto **no lo defines tú**: es el contrato que impone Spring (`OncePerRequestFilter`, que a su vez implementa `jakarta.servlet.Filter`). El contenedor de servlets (Tomcat embebido) invoca este método una vez por cada request que entra a la aplicación, y te entrega:

- `HttpServletRequest` → todo lo que llegó del cliente (headers, cookies, body, método, URI). Aquí es de donde sacas el token.
- `HttpServletResponse` → lo que vas a devolver. Lo usas solo en el camino de error, delegándolo a `securityEntryPoint`.
- `FilterChain` → el "siguiente eslabón" de la cadena de filtros de Servlet/Spring Security. Llamar `filterChain.doFilter(request, response)` significa *"yo terminé, que siga el siguiente filtro (y eventualmente el controller)"*. **No llamarlo** corta la petición ahí mismo — es justo lo que pasa en las ramas `catch`.

`OncePerRequestFilter` (en vez de implementar `Filter` directo) garantiza que esta lógica se ejecute **una sola vez por request**, incluso si hay `forward`/`include` internos, algo que Spring Security recomienda explícitamente para filtros de autenticación.

---

## Recorrido del método

### 1. Extracción del token (línea 45)

```java
Optional<String> tokenOpt = extractToken(request);

if (tokenOpt.isEmpty()) {
    filterChain.doFilter(request, response);
    return;
}
```

Si no hay token (ni header ni cookie), el filtro **no rechaza la petición aquí**: simplemente la deja pasar sin autenticar. La decisión de si esa ruta requiere autenticación la toma después `authorizeHttpRequests` en `SecurityConfig` (rutas `/api/v1/auth/**` → `permitAll()`, el resto → `authenticated()`). Esto es correcto: el filtro solo se encarga de *poblar* el contexto de seguridad si puede, no de autorizar.

### 2. Camino feliz (líneas 52-69)

```java
tokenService.validateToken(tokenOpt.get());
String email = tokenService.getEmail(tokenOpt.get());
UserDetails userDetails = userDetailsService.loadUserByUsername(email);

UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

SecurityContextHolder.getContext().setAuthentication(authentication);
filterChain.doFilter(request, response);
```

- Valida el JWT y extrae el email (el "subject" del token).
- Usa el `UserDetailsService` estándar de Spring Security para cargar el usuario real desde la base de datos (roles, permisos, etc.) — el token solo prueba *quién dice ser*, pero los authorities vigentes se leen en cada request, no del propio JWT. Esto es importante: si revocas un rol en la BD, el cambio aplica en el siguiente request sin esperar a que expire el token.
- El constructor de 3 argumentos de `UsernamePasswordAuthenticationToken` (principal, credentials, authorities) marca el token como **ya autenticado** (`isAuthenticated() = true`) — por eso `credentials` va en `null`: ya no se necesita la contraseña, el JWT ya cumplió ese rol.
- `WebAuthenticationDetailsSource().buildDetails(request)` adjunta metadatos de la request (IP remota, session id) al objeto `Authentication`, útil para auditoría/logs, no afecta la autenticación en sí.
- `SecurityContextHolder.getContext().setAuthentication(...)` es **el paso que realmente autentica** ante Spring Security: es un `ThreadLocal` que el resto del framework (`@PreAuthorize`, `authorizeHttpRequests`, `SecurityContextHolder.getContext().getAuthentication()` en el controller) consulta durante el resto del ciclo de vida de ese hilo/request.

### 3. Manejo de errores (líneas 71-84)

```java
catch (JwtAuthenticationException ex) {
    SecurityContextHolder.clearContext();
    securityEntryPoint.commence(request, response, ex);
}
catch (Exception ex) {
    SecurityContextHolder.clearContext();
    securityEntryPoint.commence(request, response, new BadCredentialsException(..., ex));
}
```

- Dos niveles: primero la excepción de dominio propia (token expirado, firma inválida, etc., con su `JwtErrorCode`), y luego un catch-all genérico (por ejemplo si `loadUserByUsername` lanza `UsernameNotFoundException`, o cualquier error inesperado).
- **Pro de seguridad**: en el catch genérico no se propaga el mensaje/stacktrace real al cliente — se envuelve en un `BadCredentialsException` con un mensaje fijo (`TOKEN_INVALID`). Esto evita fugas de información interna (nombres de excepciones, stacktraces) que un atacante podría usar para enumerar usuarios o inferir detalles de implementación. El detalle completo solo se manda a logs (`log.warn`).
- `SecurityContextHolder.clearContext()` antes de delegar al entry point es correcto: evita dejar un contexto de seguridad "a medias" si algo falló después de setear parcialmente el `Authentication`.
- Ninguna rama de error llama a `filterChain.doFilter(...)`: la petición **termina ahí**, `securityEntryPoint.commence(...)` ya escribe el 401 JSON y cierra la respuesta.

### 4. `extractToken` (líneas 87-102)

```java
private Optional<String> extractToken(HttpServletRequest request) {
    String bearer = request.getHeader("Authorization");
    if (bearer != null && bearer.startsWith("Bearer ")) {
        return Optional.of(bearer.substring(7));
    }

    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
        return Arrays.stream(cookies)
                .filter(cookie -> cookieProperties.getName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    return Optional.empty();
}
```

Prioridad: **header `Authorization: Bearer ...` primero, cookie como fallback**. `Optional` aquí evita null-checks anidados y hace explícito en la firma que "puede no haber token", que es el caso normal para rutas públicas.

---

## ¿Es necesaria la parte de `Cookie`?

Depende de quién consume la API:

- Si **solo** hay clientes tipo SPA/mobile que guardan el JWT en memoria/localStorage y siempre mandan `Authorization: Bearer`, la rama de cookie es código muerto — nadie la usa y es superficie extra sin beneficio.
- Si el frontend (Angular, según tu repo) necesita que el navegador **envíe el token automáticamente** sin que JS lo maneje (para mitigar robo de token vía XSS usando una cookie `HttpOnly`), entonces sí tiene sentido y es, de hecho, la práctica recomendada frente a guardar el JWT en `localStorage`.

### Pros de usar cookie para el JWT
- Si se crea con `httpOnly=true` (ver `CookieProperties`), JavaScript en el navegador **no puede leerla** → un XSS no puede robar el token directamente (mitiga, no elimina, el robo de sesión).
- El navegador la adjunta automáticamente; el frontend no tiene que gestionar headers manualmente.

### Contras / riesgos de seguridad
- **CSRF**: el mecanismo clásico de ataque CSRF explota justo esto — el navegador manda cookies automáticamente en peticiones cross-site. Aquí `SecurityConfig` tiene `.csrf(AbstractHttpConfigurer::disable)`. Deshabilitar CSRF es razonable si el único método de auth fuera el header `Authorization` (que un sitio malicioso no puede forzar a enviar), pero **al soportar también cookie, reintroduces la superficie de CSRF** que el `csrf().disable()` asumía que no existía. Mitigantes reales de esto son el atributo `SameSite` de la cookie (`Strict`/`Lax`) — está en `CookieProperties.sameSite`, pero **este filtro no lo puede verificar**, solo lee la cookie que ya llegó; el atributo se define donde se **crea** la cookie (en el login/refresh, fuera de este archivo).
- Este filtro **no valida** `Secure`/`HttpOnly`/`SameSite` al leer la cookie — no puede, esos son atributos que el navegador respeta al *enviar*, no algo verificable del lado servidor al leer `request.getCookies()`. La seguridad real de la cookie se decide 100% en el punto donde se construye (`Set-Cookie`), no aquí.
- Si `cookieProperties.getName()` coincide por casualidad con una cookie de otro propósito (ej. analytics) el filtro la tomaría igual — riesgo bajo, pero vale la pena que el nombre sea específico (algo como `bookingapp_jwt`, no `token`).

**Recomendación**: si el frontend web ya funciona bien con `Authorization: Bearer`, puedes considerar quitar el soporte de cookie para reducir superficie de ataque. Si lo necesitas por el tema de XSS/`httpOnly`, entonces sí es necesario, pero **debes** re-habilitar protección CSRF (o un patrón double-submit-cookie) mientras la cookie siga siendo un mecanismo válido de autenticación — actualmente no la tienes.

---

## Relación con CORS

Este archivo no configura CORS directamente (eso vive en `SecurityConfig.corsConfigurationSource()`), pero están acoplados en la práctica:

- Para que un navegador envíe la cookie del JWT en una petición **cross-origin** (frontend y backend en dominios/puertos distintos), se necesitan **dos** condiciones simultáneas: `Access-Control-Allow-Credentials: true` en el servidor (`corsProperties.isAllowCredentials()`) y un origen **explícito** en `Access-Control-Allow-Origin` (nunca `*` — el spec de CORS lo prohíbe cuando se permiten credenciales).
- Si `corsProperties` tiene orígenes permitidos demasiado amplios (ej. patrones muy laxos) **y** `allowCredentials=true`, cualquier sitio que entre en ese patrón podría hacer que el navegador de una víctima logueada mande peticiones autenticadas (vía cookie) a tu API — esto amplifica exactamente el riesgo de CSRF mencionado arriba.
- Si el flujo real es solo `Authorization: Bearer` (sin cookies), CORS mal configurado es menos crítico para CSRF porque un sitio malicioso no puede forzar el envío del header (necesitaría leer el token, que con SPA normalmente vive en memoria/storage, no accesible cross-origin).

En resumen: **cuantas más rutas de autenticación por cookie soportes, más estricta debe ser tu configuración CORS/CSRF.** Vale la pena revisar `CorsProperties` para confirmar que `allowedOrigins` no sea `*` mientras `allowCredentials=true`.

---

## Relación con Spring Security — ¿es necesario este filtro tal cual?

- Spring Security no trae "de fábrica" un filtro para JWT hecho a mano con un `TokenService` propio (sí trae uno si usas el *Resource Server* de OAuth2 con `spring-boot-starter-oauth2-resource-server`, que decodifica JWT estándar automáticamente). Como aquí el proyecto tiene su propia implementación de `TokenService`/`JwtAuthenticationException`, un filtro custom **es la forma correcta** de integrarlo: es el patrón estándar documentado por Spring Security para "JWT casero".
- El registro en `SecurityConfig`:
  ```java
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
  ```
  posiciona este filtro **antes** del filtro de login por formulario en la cadena de `SecurityFilterChain`. El orden importa: así el `SecurityContextHolder` ya queda poblado (o no) antes de que `authorizeHttpRequests`/`FilterSecurityInterceptor` decida si la request pasa o recibe 401/403.
- Detalle no obvio pero correcto en `SecurityConfig` (línea 36-40):
  ```java
  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
      FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
      registration.setEnabled(false);
      return registration;
  }
  ```
  Como `JwtAuthenticationFilter` está anotado `@Component`, Spring Boot lo registraría **automáticamente** como un filtro de servlet genérico (fuera de la cadena de Spring Security) además de agregarlo manualmente con `.addFilterBefore(...)`. Sin este `FilterRegistrationBean` con `setEnabled(false)`, el filtro se ejecutaría **dos veces por request** (una vez por el auto-registro de Boot, otra por Spring Security). Esto ya está bien resuelto en el proyecto — es un error común al anotar un `Filter` de Spring Security con `@Component`.
- `SessionCreationPolicy.STATELESS` (en `SecurityConfig`) confirma que este filtro es la **única** fuente de autenticación por request: no hay sesión HTTP que recuerde al usuario entre peticiones, por eso el JWT tiene que revalidarse y el `SecurityContextHolder` repoblarse en cada llamada.

---

## Resumen pros/contras del diseño actual

| Aspecto | Pro | Contra / a vigilar |
|---|---|---|
| `OncePerRequestFilter` + registro explícito | Evita doble ejecución, patrón estándar de Spring Security | — |
| Roles cargados desde `UserDetailsService` en cada request | Revocación de permisos es inmediata, no depende de que expire el JWT | Un query extra a BD por request (se puede cachear si hace falta) |
| Manejo de excepciones con `securityEntryPoint` | Respuesta 401 uniforme y sin fuga de detalles internos | — |
| Soporte de cookie como fallback | Permite `HttpOnly` (mitiga robo de token vía XSS) | Reintroduce superficie CSRF mientras `csrf().disable()` siga activo y no haya `SameSite` estricto verificado |
| CORS con `allowCredentials` | Necesario si el frontend depende de la cookie | Debe combinarse con orígenes explícitos, nunca wildcard |
