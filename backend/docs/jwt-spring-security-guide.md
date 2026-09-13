# Guía: JWT + Spring Security en Spring Boot (RS256)

Implementación completa de autenticación stateless con JWT firmado con RSA (RS256), usando `spring-boot-starter-security` y `spring-boot-starter-oauth2-resource-server`.

---

## 1. Dependencias (`pom.xml`)

```xml
<!-- Seguridad base -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- OAuth2 Resource Server: trae JwtEncoder, JwtDecoder, NimbusJwtDecoder -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Nimbus para manejo de JWK/RSA -->
<dependency>
    <groupId>com.nimbusds</groupId>
    <artifactId>nimbus-jose-jwt</artifactId>
    <version>10.0.2</version>
</dependency>
```

---

## 2. Generar par de claves RSA

```bash
# Clave privada (2048 bits)
openssl genrsa -out private.pem 2048

# Clave pública derivada
openssl rsa -in private.pem -pubout -out public.pem
```

Coloca ambos archivos en `src/main/resources/certs/` y añade esa carpeta a `.gitignore`.

---

## 3. Configuración (`application.yaml`)

```yaml
security:
  jwt:
    private-key: classpath:certs/private.pem
    public-key: classpath:certs/public.pem
    expiration: 60        # minutos para el access token
  cookie:
    name: auth_token
    http-only: true
    secure: true
    max-age: 3600
    same-site: Strict
```

---

## 4. Clases a crear — Orden de implementación

### 4.1 Dominio: modelo de usuario limpio

```java
// domain/model/User.java  — POJO puro, sin anotaciones Spring/JPA
@Getter @Setter @Builder
public class User {
    private Long id;
    private String email;
    private String name;
    private String password;
    private boolean active;
    private Set<Role> roles;
}
```

### 4.2 Puertos de salida

```java
// application/port/out/LoadUserByEmailPort.java
public interface LoadUserByEmailPort {
    User loadByEmail(String email);
}

// application/port/out/TokenService.java
public interface TokenService {
    String generateToken(Long userId, String email, List<String> roles);
    String generateRefreshToken(Long userId);
    String getEmail(String token);
    String getSubject(String token);
    void validateToken(String token);
}

// application/port/out/PasswordHasher.java
public interface PasswordHasher {
    String hash(String raw);
    boolean matches(String raw, String encoded);
}
```

### 4.3 `JwtConfig` — beans de encoder/decoder

```java
@Configuration
public class JwtConfig {

    @Value("${security.jwt.private-key}")
    private RSAPrivateKey privateKey;

    @Value("${security.jwt.public-key}")
    private RSAPublicKey publicKey;

    @Bean
    JWKSet jwkSet() {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new JWKSet(rsaKey);
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSet jwkSet) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(jwkSet));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }
}
```

### 4.4 `JwtTokenService` — implementación de `TokenService`

```java
// infrastructure/security/filter/JwtTokenService.java
@Service
@RequiredArgsConstructor
public class JwtTokenService implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    @Value("${security.jwt.expiration}")
    private long expirationMinutes;

    @Override
    public String generateToken(Long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .claim("user_id", userId)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    @Override
    public String getEmail(String token) {
        return jwtDecoder.decode(token).getClaimAsString("email");
    }

    @Override
    public void validateToken(String token) {
        try {
            jwtDecoder.decode(token);
        } catch (JwtValidationException ex) {
            throw new JwtAuthenticationException(JwtErrorCode.TOKEN_EXPIRED);
        } catch (BadJwtException ex) {
            throw new JwtAuthenticationException(JwtErrorCode.TOKEN_INVALID);
        }
    }
    // ... getSubject, generateRefreshToken similares
}
```

### 4.5 `CustomUserDetails` — wrapper de `UserDetails`

```java
// infrastructure/config/CustomUserDetails.java
// NO es @Component — se instancia con new CustomUserDetails(user)
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
            role.permissions().forEach(p ->
                    authorities.add(new SimpleGrantedAuthority(p.name()))
            );
        });
        return authorities;
    }

    @Override public String getPassword() { return user.getPassword(); }
    @Override public String getUsername() { return user.getEmail(); }
    @Override public boolean isEnabled()  { return user.isActive(); }
    // isAccountNonExpired, isAccountNonLocked, isCredentialsNonExpired → true
}
```

### 4.6 `CustomUserDetailsService` — implementa `UserDetailsService`

```java
// infrastructure/config/CustomUserDetailsService.java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final LoadUserByEmailPort loadUserByEmailPort;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            User user = loadUserByEmailPort.loadByEmail(email);
            return new CustomUserDetails(user);
        } catch (Exception ex) {
            throw new UsernameNotFoundException("User not found: " + email, ex);
        }
    }
}
```

### 4.7 `JwtAuthenticationFilter` — filtro por request

```java
// infrastructure/security/filter/JwtAuthenticationFilter.java
// Extiende OncePerRequestFilter — NO es @Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final List<String> publicEndpointPatterns;
    private final String authCookieName;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // constructor explícito (no @RequiredArgsConstructor — no es bean directo)

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return publicEndpointPatterns.stream()
                .anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(...) {
        // 1. Extrae token de header Authorization: Bearer o cookie
        // 2. tokenService.validateToken(token)
        // 3. email = tokenService.getEmail(token)
        // 4. userDetails = userDetailsService.loadUserByUsername(email)
        // 5. SecurityContextHolder.getContext().setAuthentication(
        //        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()))
    }
}
```

### 4.8 `SecurityConfig` — cadena de filtros

```java
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final CorsProperties corsProperties;
    private final CookieProperties cookieProperties;

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(
                tokenService,
                userDetailsService,
                List.of("/api/v1/auth/**"),
                cookieProperties.getName()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

---

## 5. Flujo completo

```
Registro
  POST /api/v1/auth/register  {email, password, name}
  → RegisterUserService: hashea password, asigna rol USER, guarda User
  → TokenService.generateToken(id, email, ["USER"])
  → Devuelve {token, refreshToken}

Login
  POST /api/v1/auth/login  {email, password}
  → AuthenticateUserService: carga usuario, BCrypt.matches(), comprueba active
  → TokenService.generateToken(...)
  → Devuelve {token, refreshToken}

Request autenticado
  GET /api/v1/customers
  Authorization: Bearer <token>
  → JwtAuthenticationFilter:
      1. Extrae token del header
      2. tokenService.validateToken(token)   ← firma RS256, expiración
      3. email = tokenService.getEmail(token)
      4. userDetails = CustomUserDetailsService.loadUserByUsername(email)
      5. Setea UsernamePasswordAuthenticationToken en SecurityContextHolder
  → Spring Security verifica .anyRequest().authenticated() → pasa
  → Controller ejecuta

Refresh
  POST /api/v1/auth/refresh  {refreshToken}
  → RefreshTokenUseCase: validateToken, getSubject (userId), UserRepository.findById
  → generateToken(id, email, roles)
  → Devuelve nuevo {token}
```

---

## 6. Errores comunes

| Error | Causa | Solución |
|-------|-------|---------|
| `UnsatisfiedDependencyException` en `CustomUserDetails` | `@Component` en una clase que recibe un POJO | Quitar `@Component`; instanciar con `new CustomUserDetails(user)` |
| Spring usa `InMemoryUserDetailsManager` | No hay bean `UserDetailsService` | Crear `CustomUserDetailsService implements UserDetailsService @Service` |
| Filtro JWT no se ejecuta | `JwtAuthenticationFilter` no registrado | Exponer como `@Bean` en `SecurityConfig` y añadir `.addFilterBefore(...)` |
| `403` en endpoints públicos | Falta `.requestMatchers("/api/v1/auth/**").permitAll()` | Añadir la regla explícita antes de `.anyRequest().authenticated()` |
| `JwtValidationException` al validar | Token expirado o firma incorrecta | Verificar `security.jwt.expiration` y que la clave pública coincide con la privada |
