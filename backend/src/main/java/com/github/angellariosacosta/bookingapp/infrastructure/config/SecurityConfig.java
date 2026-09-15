package com.github.angellariosacosta.bookingapp.infrastructure.config;

import com.github.angellariosacosta.bookingapp.infrastructure.security.entrypoint.SecurityEntryPoint;
import com.github.angellariosacosta.bookingapp.infrastructure.security.filter.CsrfCookieFilter;
import com.github.angellariosacosta.bookingapp.infrastructure.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    // Factor de coste de BCrypt: 2^12 iteraciones de hash (~250 ms). Subir aumenta seguridad pero penaliza latencia de login.
    private static final int BCRYPT_STRENGTH = 12;

    private final CorsProperties corsProperties;
    private final CsrfProperties csrfProperties;
    private final SecurityEntryPoint securityEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilita CSRF (antes estaba deshabilitado) porque el login va a pasar a viajar en cookie
                // en vez de header Authorization: con cookie, el navegador la reenvía solo porque existe,
                // sin que el request lo pida explícitamente, y ahí es donde aparece el riesgo de CSRF.
                // csrf.spa() (atajo de Spring Security 7.0 pensado justo para este caso) arma un
                // CookieCsrfTokenRepository (cookie "XSRF-TOKEN" legible por JavaScript) + un
                // CsrfTokenRequestHandler que compara el valor CRUDO de esa cookie contra el header
                // X-XSRF-TOKEN. El handler que Spring usa por defecto (XorCsrfTokenRequestAttributeHandler)
                // espera el valor enmascarado (XOR), no el valor crudo que expone la cookie — con ese
                // default, ningún cliente (Angular, Postman, curl) que simplemente copie la cookie al
                // header podía pasar la validación, así que bloqueaba el 100% de los POST. spa() usa el
                // handler correcto para este patrón. Si el header no coincide, la petición se rechaza —
                // eso es lo que bloquea a un sitio atacante: puede hacer que el navegador reenvíe la
                // cookie, pero no puede leer su valor para ponerlo en el header.
                // security.csrf.enabled queda en false solo en el perfil "dev" (ver application.yaml) para
                // poder probar la API con Postman sin el paso manual de copiar XSRF-TOKEN; en "qa" y en
                // cualquier otro perfil se mantiene en true. Ver docs/csrf-decision.md.
                .csrf(csrf -> {
                    if (csrfProperties.isEnabled()) {
                        csrf.spa();
                    } else {
                        csrf.disable();
                    }
                })
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
                        .requestMatchers("/api/v1/auth/**", "/error").permitAll()
                        .anyRequest().authenticated()
                );

        if (csrfProperties.isEnabled()) {
            http.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class);
        }

        return http.build();
    }

    // Expone la configuración CORS leída de CorsProperties (application.yaml) y la aplica a todas las rutas.
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

    // Encoder de contraseñas con BCrypt. Se inyecta en PasswordHasher para no acoplar la infra al algoritmo concreto.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    // Expone el AuthenticationManager de Spring como bean para poder inyectarlo en los servicios de autenticación.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

}
