# `SecurityConfig` — guía de la configuración de seguridad

Este documento explica, en detalle y con contexto pedagógico, qué hace `SecurityConfig.java` y por qué está construido así. No sustituye al código, lo complementa: cada sección referencia la línea real del archivo.

---

## 1. Qué hace esta clase a nivel general

`SecurityConfig` es una clase `@Configuration` de Spring: no contiene lógica de negocio, solo **declara beans** que Spring usa para montar la seguridad HTTP de toda la aplicación. Su responsabilidad es una sola: construir el `SecurityFilterChain`, que es literalmente la cadena de filtros que Spring Security intercala en cada request HTTP entrante para decidir:

- si la request necesita autenticación,
- cómo se extrae y valida esa autenticación (aquí: un JWT, no sesión ni login por formulario),
- qué pasa si la autenticación falla o falta,
- qué orígenes/métodos/headers puede usar un cliente externo (CORS).

Es la pieza central que convierte esta API en una **API stateless protegida por JWT**: no hay `HttpSession` en el servidor, no hay cookies de sesión de Spring Security, cada request se autentica por sí misma a partir de un token.

Además de `securityFilterChain`, esta clase expone otros beans auxiliares que necesita el resto de la app (encoder de contraseñas, `AuthenticationManager`, configuración CORS) — se explican uno por uno más abajo.

---

## 2. Las anotaciones de clase

```java
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {
```

- **`@Configuration`**: le dice a Spring que esta clase declara beans (`@Bean`) que deben entrar al contenedor de IoC. Es el equivalente moderno a un XML de configuración de Spring clásico.
- **`@RequiredArgsConstructor`** (Lombok): genera un constructor con todos los campos `final` (`corsProperties`, `securityEntryPoint`, `jwtAuthenticationFilter`). Es el mecanismo de **inyección por constructor** obligatorio en este proyecto (ver `CLAUDE.md`): nada de `@Autowired` en campos.
- **`@EnableConfigurationProperties(CorsProperties.class)`**: activa el binding de `CorsProperties` (una clase `@ConfigurationProperties` que lee valores del `application.yaml`, p. ej. orígenes permitidos) y la registra como bean para poder inyectarla aquí.

---

## 3. El bean `securityFilterChain` — paso a paso

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .exceptionHandling(ex ->
                    ex.authenticationEntryPoint(securityEntryPoint)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/auth/**").permitAll()
                    .anyRequest().authenticated()
            );

    return http.build();
}
```

`HttpSecurity` es un builder: cada método configura un aspecto y devuelve el mismo builder para encadenar (`DSL` fluido). Al final, `http.build()` produce el `SecurityFilterChain` real, que Spring Boot registra automáticamente en el servlet container.

Eslabón por eslabón:

| Línea | Qué hace |
|---|---|
| `.httpBasic(disable)` | Desactiva el login por autenticación básica HTTP (usuario/contraseña en el header `Authorization: Basic ...`). No lo usamos, la autenticación es 100% vía JWT. |
| `.formLogin(disable)` | Desactiva la página de login por formulario que Spring Security genera por defecto. Esta es una API REST, no hay vistas server-side. |
| `.sessionManagement(STATELESS)` | Le dice a Spring Security que **nunca** cree ni use `HttpSession` para guardar el contexto de autenticación. Cada request debe traer su propia prueba de identidad (el JWT). Esto es la base de una arquitectura stateless/escalable horizontalmente. |
| `.cors(...)` | Activa el soporte CORS de Spring Security usando la `CorsConfigurationSource` definida más abajo (sección 7). |
| `.exceptionHandling(...)` | Registra `securityEntryPoint` como el `AuthenticationEntryPoint` (sección 8): qué responder cuando una request no autenticada intenta acceder a algo protegido. |
| `.addFilterBefore(...)` | Inserta `jwtAuthenticationFilter` en la cadena, justo **antes** de `UsernamePasswordAuthenticationFilter` (sección 9). |
| `.authorizeHttpRequests(...)` | Define las reglas de autorización: `/api/v1/auth/**` (login, registro) es público; **todo lo demás requiere autenticación**. |

### 3.1 ¿Por qué se deshabilita CSRF?

```java
.csrf(AbstractHttpConfigurer::disable)
```

CSRF (*Cross-Site Request Forgery*) es un ataque que abusa de que el navegador **adjunta automáticamente las cookies de sesión** en cualquier request, aunque esa request la haya disparado un sitio malicioso distinto. La defensa clásica de Spring Security (un token CSRF que el formulario debe reenviar) tiene sentido cuando:

- la autenticación vive en una **cookie de sesión** gestionada por el servidor (`HttpSession`), y
- el cliente es un navegador con formularios/JS que confía implícitamente en esa cookie.

En esta app **ninguna de las dos condiciones aplica de la forma clásica**:

1. La sesión es `STATELESS` (sección 3, fila `sessionManagement`) — no existe `HttpSession` que un atacante pueda "montar" en su ataque.
2. La autenticación viaja como JWT, ya sea en el header `Authorization: Bearer <token>` (que un sitio atacante **no puede** adjuntar automáticamente — no es una cookie) o, según `JwtAuthenticationFilter`, opcionalmente en una cookie propia. En el caso del header, CSRF deja de ser un vector válido porque el navegador no reenvía headers custom entre orígenes por su cuenta.

**Matiz honesto:** el filtro sí acepta el token también desde cookie (`CookieProperties`, ver sección 9). Si el JWT viaja en cookie, la protección real contra CSRF pasa a depender de los atributos de esa cookie (`SameSite=Strict`/`Lax`, `HttpOnly`, `Secure`), no de esta línea. Deshabilitar CSRF aquí es correcto para el modelo "token de portador stateless", pero no es una exención automática de pensar en CSRF si se cambia la estrategia de transporte del token.

### 3.2 El `FilterRegistrationBean<JwtAuthenticationFilter>` con `setEnabled(false)`

```java
@Bean
public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
}
```

Esto resuelve un problema de **doble registro**. `JwtAuthenticationFilter` está anotado con `@Component` (para poder inyectarle sus propias dependencias vía constructor), lo que significa que Spring Boot, por defecto, **también** lo registraría automáticamente como un filtro de servlet genérico (aplicado a *todas* las requests, sin control de orden respecto a la cadena de Spring Security). Eso duplicaría la ejecución del filtro: una vez por el auto-registro de Spring Boot, y otra vez dentro de la cadena de Spring Security vía `.addFilterBefore(...)` (sección 3, última fila).

Este bean neutraliza ese auto-registro (`setEnabled(false)` = "no lo registres como filtro de servlet suelto"), dejando `JwtAuthenticationFilter` activo **únicamente** dentro del `SecurityFilterChain`, en la posición exacta que se define explícitamente con `addFilterBefore`. Es un patrón conocido para filtros que son a la vez `@Component` y parte de una cadena de Spring Security.

---

## 4. `passwordEncoder()` y el factor 12 de BCrypt

```java
private static final int BCRYPT_STRENGTH = 12;
...
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
}
```

BCrypt es un algoritmo de hashing de contraseñas diseñado para ser **deliberadamente lento**, con un coste ajustable — esto lo hace resistente a ataques de fuerza bruta/rainbow tables, a diferencia de algoritmos rápidos como SHA-256 (pensados para velocidad, no para proteger contraseñas).

El **"factor de coste" (`strength`)** es el exponente de un número de iteraciones internas: un valor de `12` significa `2^12 = 4096` rondas de hashing por cada contraseña. A mayor valor:

- más difícil es para un atacante probar contraseñas por fuerza bruta (cada intento cuesta más tiempo de CPU),
- pero también más lento es cada login/registro legítimo (~250 ms con factor 12 en hardware típico).

`12` es el valor por defecto recomendado por la propia librería de Spring Security como buen punto de equilibrio seguridad/latencia; subirlo (13, 14...) duplica el costo computacional por cada punto. Se centraliza en la constante `BCRYPT_STRENGTH` (en vez de escribir `new BCryptPasswordEncoder(12)` directamente) para no dejar un "número mágico" suelto y documentar la intención en un solo lugar.

Este bean se inyecta luego en la capa de aplicación (p. ej. en un `PasswordHasher` de dominio/aplicación) para no acoplar esa capa al algoritmo concreto — la app solo sabe que existe un `PasswordEncoder`, no que es BCrypt.

---

## 5. `authenticationManager()` — por qué se expone como bean

```java
@Bean
public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
}
```

Spring Security construye un `AuthenticationManager` internamente, pero **no lo publica como bean inyectable por defecto**. Si algún servicio de aplicación necesita autenticar explícitamente unas credenciales (típicamente el caso de uso de *login*: recibir email+password y validar contra el `UserDetailsService`), necesita poder inyectar un `AuthenticationManager`.

Este método toma el `AuthenticationConfiguration` (una clase de infraestructura interna de Spring que ya sabe cómo ensamblar el `AuthenticationManager` con los `AuthenticationProvider`s registrados — incluyendo el `DaoAuthenticationProvider` que usa nuestro `UserDetailsService` + `PasswordEncoder`) y simplemente **expone ese manager como bean**, para poder hacer `@RequiredArgsConstructor` con `AuthenticationManager authenticationManager` en cualquier servicio de aplicación.

---

## 6. `corsConfigurationSource()`

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
    configuration.setAllowedMethods(corsProperties.getAllowedMethods());
    configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
    configuration.setExposedHeaders(corsProperties.getExposedHeaders());
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());
    configuration.setMaxAge(corsProperties.getMaxAge());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

No hardcodea nada: toda la política CORS (qué orígenes, métodos, headers están permitidos, si se permiten credenciales, cuánto tiempo cachea el navegador el preflight) viene de `CorsProperties`, que a su vez se lee de `application.yaml`. Esto permite tener políticas CORS distintas por entorno (dev permisivo, prod restringido a los dominios del frontend) sin tocar código. Se aplica a todas las rutas (`"/**"`).

---

## 7. ¿Qué es un `AuthenticationEntryPoint`?

Un `AuthenticationEntryPoint` es el "punto de entrada" que Spring Security invoca cuando una request **no autenticada** intenta acceder a un recurso que requiere autenticación, o cuando el proceso de autenticación falla. Por defecto, Spring Security suele redirigir a una página de login HTML — algo sin sentido en una API REST.

Aquí, `SecurityEntryPoint` (`infrastructure/security/entrypoint/SecurityEntryPoint.java`) implementa esa interfaz y, en vez de redirigir, devuelve una respuesta **JSON 401 consistente** con el resto de la API:

```java
ErrorResponse body = new ErrorResponse(
        Instant.now().toString(),
        HttpStatus.UNAUTHORIZED.value(),
        HttpStatus.UNAUTHORIZED.getReasonPhrase(),
        MESSAGE // "Autenticación requerida"
);
```

Se registra en `securityFilterChain` vía `.exceptionHandling(ex -> ex.authenticationEntryPoint(securityEntryPoint))`, y además `JwtAuthenticationFilter` lo llama **directamente** (`securityEntryPoint.commence(...)`) cuando detecta que un token es inválido o expiró, para reutilizar el mismo formato de error en ambos casos: "no mandaste token" y "mandaste un token pero es inválido" terminan en la misma respuesta 401 uniforme.

---

## 8. `JwtAuthenticationFilter` — dónde encaja

`JwtAuthenticationFilter` (`infrastructure/security/filter/JwtAuthenticationFilter.java`) extiende `OncePerRequestFilter` (garantiza que se ejecute una sola vez por request, incluso con forwards/includes internos). En cada request:

1. Busca un token: primero en el header `Authorization: Bearer <token>`, si no está, en una cookie (nombre configurado por `CookieProperties`).
2. Si no hay token, deja pasar la request tal cual (`filterChain.doFilter`) — la decisión de si esa ruta requiere autenticación la toma después `authorizeHttpRequests`, no este filtro.
3. Si hay token, lo valida con `TokenService`, obtiene el email del payload, carga el `UserDetails` vía `UserDetailsService`, y puebla `SecurityContextHolder` con un `UsernamePasswordAuthenticationToken` ya autenticado.
4. Si algo falla (token inválido, expirado, etc.), limpia el contexto de seguridad y delega en `securityEntryPoint.commence(...)` (sección 7) para devolver el 401 JSON.

Se inserta con `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`: es decir, **antes** del filtro que Spring Security usaría para un login por formulario tradicional. Como no usamos `formLogin`, ese filtro de referencia (`UsernamePasswordAuthenticationFilter`) casi no hace nada por sí mismo aquí — se usa solo como "marcador de posición" en la cadena para indicar el orden relativo donde debe ir la validación de JWT.

---

## 9. ¿Dónde vive todo esto respecto a Spring MVC / `DispatcherServlet`?

Este es el punto que más suele confundir: **Spring Security no es parte de Spring MVC**. Son dos capas distintas que se ejecutan en momentos distintos del ciclo de vida de una request HTTP.

### El flujo completo de una request

```
Cliente HTTP
   │
   ▼
Servlet Container (Tomcat embebido)
   │
   ▼
FilterChain de Servlet  ◄── Spring Security vive AQUÍ
   │   (DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain)
   │
   │   1. Filtro CORS
   │   2. JwtAuthenticationFilter   ← nuestro filtro
   │   3. ExceptionTranslationFilter (usa SecurityEntryPoint si algo falla)
   │   4. AuthorizationFilter (evalúa authorizeHttpRequests)
   │
   ▼
DispatcherServlet  ◄── Spring MVC empieza AQUÍ
   │
   ▼
HandlerMapping (encuentra qué @RestController/@GetMapping matchea la URL)
   │
   ▼
@RestController → capa application → domain → infrastructure.persistence
```

- **Servlet Filters** (donde vive Spring Security) son una capa estándar de la especificación Servlet (`jakarta.servlet.Filter`), **anterior y externa** a Spring MVC. Spring Boot los conecta a través de un `DelegatingFilterProxy` que Spring Security registra automáticamente en el servlet container; internamente ese proxy delega en un `FilterChainProxy`, que ejecuta la cadena de filtros que nosotros configuramos en `securityFilterChain` (CORS, JWT, manejo de excepciones de auth, autorización).
- **`DispatcherServlet`** es, en cambio, el corazón de Spring MVC: es *él mismo* un `Servlet` (implementa la interfaz `jakarta.servlet.Servlet`, no `Filter`), y es el único punto de entrada al mundo de `@Controller`/`@RestController`, `HandlerMapping`, `HandlerAdapter`, resolución de vistas/JSON, etc.
- Como los *filters* se ejecutan **antes** que cualquier *servlet* (incluido el `DispatcherServlet`) para esa misma request, cualquier request rechazada por seguridad (falta de token, token inválido, ruta no autorizada) **nunca llega a un `@RestController`**. El corte ocurre en la capa de filtros: `SecurityEntryPoint` escribe directamente en el `HttpServletResponse` y la cadena termina ahí — no existe todavía ningún `HandlerMapping` ni lógica MVC involucrada en esa respuesta 401.

En resumen: `DispatcherServlet` (Spring MVC) responde *qué controlador* atiende una URL válida; `SecurityFilterChain` (Spring Security, esta clase) decide *si esa request llega siquiera a intentarlo*.

---

## 10. Resumen de beans que expone esta clase

| Bean | Propósito |
|---|---|
| `securityFilterChain(HttpSecurity)` | Cadena de filtros de seguridad: CORS, stateless, JWT, autorización por ruta. |
| `jwtFilterRegistration(JwtAuthenticationFilter)` | Evita que Spring Boot registre `JwtAuthenticationFilter` dos veces (por ser `@Component` además de estar en la cadena). |
| `corsConfigurationSource()` | Política CORS leída de `CorsProperties`/`application.yaml`, aplicada a `/**`. |
| `passwordEncoder()` | `BCryptPasswordEncoder` con factor de coste 12, usado para hashear/verificar contraseñas. |
| `authenticationManager(AuthenticationConfiguration)` | Publica el `AuthenticationManager` interno de Spring como bean inyectable, para casos de uso de login. |
