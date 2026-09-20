package com.github.angellariosacosta.bookingapp.infrastructure.config;


import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.resolver.CurrentUserIdArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties corsProperties;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    public WebConfig(CorsProperties corsProperties, CurrentUserIdArgumentResolver currentUserIdArgumentResolver) {
        this.corsProperties = corsProperties;
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
    }

    // Configura CORS (Cross-Origin Resource Sharing) para toda la API.
    // Sin esto, el navegador bloquearía las respuestas cuando el frontend
    // (ej. https://miapp.com) llama a un backend servido desde otro origen
    // (ej. https://api.miapp.com), aunque el request en sí funcione.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // "/**" aplica esta política a TODAS las rutas de la API.
        registry.addMapping("/**")
                // Orígenes permitidos (esquema + host + puerto), ej. "http://localhost:5173"
                // en dev o el dominio real en producción. Se usa "OriginPatterns" (no
                // "allowedOrigins") porque soporta wildcards y es OBLIGATORIO cuando
                // allowCredentials(true) está activo: con "*" el navegador rechaza
                // la combinación por seguridad.
                .allowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
                // Verbos HTTP permitidos en requests cross-origin (GET, POST, PUT, DELETE...).
                // Cualquier método fuera de esta lista es rechazado en el preflight (OPTIONS)
                // antes de llegar a los controladores.
                .allowedMethods(corsProperties.getAllowedMethods().toArray(new String[0]))
                // Headers que el cliente puede enviar (ej. Authorization, Content-Type,
                // X-XSRF-TOKEN). Cualquier header fuera de esta lista también es
                // rechazado en el preflight.
                .allowedHeaders(corsProperties.getAllowedHeaders().toArray(new String[0]))
                // Permite que el navegador incluya cookies/credenciales (access_token,
                // refresh_token en cookies HttpOnly) en requests cross-origin, y que el
                // cliente JS pueda leer la respuesta. Sin esto, las cookies de auth
                // no viajarían aunque el resto de CORS esté bien configurado.
                .allowCredentials(true);
    }

    // Registra HandlerMethodArgumentResolver(s) personalizados: "traductores" que
    // Spring MVC consulta ANTES de invocar un método de controlador, para decidir
    // cómo construir cada uno de sus parámetros (igual que ya hace nativamente con
    // @RequestBody, @PathVariable, @RequestParam, etc.).
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // CurrentUserIdArgumentResolver intercepta cualquier parámetro anotado con
        // @CurrentUserId (tipo Long) y lo rellena automáticamente con el id del
        // usuario autenticado (extraído del SecurityContext/JWT vía CurrentUserPort),
        // en vez de que cada controlador tenga que sacarlo manualmente del
        // SecurityContextHolder. Ejemplo de uso en un controller:
        //   create(@CurrentUserId Long userId, @RequestBody CreateBookingRequest request)
        resolvers.add(currentUserIdArgumentResolver);
    }
}
