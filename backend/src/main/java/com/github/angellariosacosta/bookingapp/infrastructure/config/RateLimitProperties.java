package com.github.angellariosacosta.bookingapp.infrastructure.config;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.rate-limit")
@Getter
@Setter
public class RateLimitProperties {
    private boolean enabled = true;
    private long capacity;
    private Duration window;
    private CacheSettings cache = new CacheSettings();

    @Getter
    @Setter
    public static class CacheSettings {
        private Duration expireAfterAccess;
        private long maximumSize;
    }
}
