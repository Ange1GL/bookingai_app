package com.github.angellariosacosta.bookingapp.infrastructure.security.filter;

import java.io.IOException;
import java.time.Instant;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.angellariosacosta.bookingapp.infrastructure.adapter.in.rest.dto.ErrorResponse;
import com.github.angellariosacosta.bookingapp.infrastructure.config.RateLimitProperties;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String JSON_CONTENT_TYPE = "application/json;charset=UTF-8";
    private static final String RATE_LIMIT_MESSAGE = "Demasiadas peticiones, intenta de nuevo más tarde";
    private static final String REMAINING_HEADER = "X-RateLimit-Remaining";

    private final RateLimitProperties rateLimitProperties;
    private final Cache<String, Bucket> bucketCache;
    private final JsonMapper jsonMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // OPTIONS (preflight CORS) nunca debe contar contra el límite: el navegador lo dispara solo,
        // sin que el frontend lo pida, y bloquearlo rompería CORS para clientes cross-origin.
        return !rateLimitProperties.isEnabled() || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Token bucket, un balde por IP: bucketCache.get busca el Bucket de esa IP o lo crea (newBucket)
        // si es la primera vez que se ve. Cada IP tiene su propio cupo independiente del resto.
        Bucket bucket = bucketCache.get(request.getRemoteAddr(), key -> newBucket());
        // Intenta consumir 1 token del balde. Esta línea ES el algoritmo: si hay tokens, resta uno y
        // deja pasar (isConsumed=true); si el balde está vacío, no resta nada y rechaza. ConsumptionProbe
        // además informa cuántos tokens quedan y cuánto falta para el próximo refill.
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(REMAINING_HEADER, String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        // Balde vacío: nanosToWaitForRefill es lo que falta para que exista al menos 1 token nuevo,
        // dado el refill "greedy" (ver newBucket) — se convierte a segundos para el header Retry-After.
        long retryAfterSeconds = Math.max(1, Math.ceilDiv(probe.getNanosToWaitForRefill(), 1_000_000_000L));
        log.warn("Rate limit excedido para ip={} en {} {}",
                request.getRemoteAddr(), request.getMethod(), request.getRequestURI());

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
    }

    // Configuración del token bucket para una IP nueva. Bucket4j sin ProxyManager: cada IP tiene su
    // propio Bucket local en memoria (bucketCache), suficiente para una sola instancia. Si la app
    // escala horizontalmente, esto deja de compartir el límite entre instancias y haría falta un
    // ProxyManager distribuido (ej. Redis).
    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        // capacity: tamaño máximo del balde — cuántas requests puede hacer de golpe
                        // una IP con el balde lleno antes de que la empiecen a rechazar.
                        .capacity(rateLimitProperties.getCapacity())
                        // refillGreedy(capacity, window): en vez de recargar los "capacity" tokens de
                        // golpe al terminar la ventana (fixed window, permite ráfaga doble en el borde),
                        // los reparte de forma continua tan pronto como el tiempo lo permite — ej. con
                        // capacity=60 y window=1m, agrega ~1 token cada segundo. Esto es lo que hace que
                        // sea "token bucket" y no un contador simple por ventana.
                        .refillGreedy(rateLimitProperties.getCapacity(), rateLimitProperties.getWindow()))
                .build();
    }
}
