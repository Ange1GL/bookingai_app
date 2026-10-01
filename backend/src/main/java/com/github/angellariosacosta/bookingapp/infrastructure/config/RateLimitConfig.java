package com.github.angellariosacosta.bookingapp.infrastructure.config;

import com.github.angellariosacosta.bookingapp.infrastructure.security.filter.RateLimitFilter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RateLimitConfig {

    // Cache local (una entrada por IP) que respalda los buckets de Bucket4j. expireAfterAccess libera
    // el bucket de una IP inactiva y maximumSize pone un techo duro de memoria — sin esto, una IP nueva
    // por request (spoofing, IPv6 rotativas, etc.) crecería la cache sin límite. Ver RateLimitProperties
    // y docs/rate-limiting.md.
    @Bean
    public Cache<String, Bucket> rateLimitBucketCache(RateLimitProperties rateLimitProperties) {
        return Caffeine.newBuilder()
                .expireAfterAccess(rateLimitProperties.getCache().getExpireAfterAccess())
                .maximumSize(rateLimitProperties.getCache().getMaximumSize())
                .build();
    }

    // Mismo motivo que jwtFilterRegistration (SecurityConfig): RateLimitFilter es @Component (necesita
    // inyectar sus dependencias) y a la vez se registra manualmente en la cadena de Spring Security
    // (SecurityConfig#securityFilterChain, vía addFilterBefore). Sin esto, Spring Boot lo registraría
    // también a nivel de contenedor y correría dos veces por request.
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
