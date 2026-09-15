package com.github.angellariosacosta.bookingapp.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.cookie")
@Getter
@Setter
public class CookieProperties {
    private boolean httpOnly;
    // Fail-safe: si un perfil sin application-{perfil}.yaml propio queda activo, se mantiene "secure" en vez
    // de caer al default de Java (false). Solo application-dev.yaml lo baja explícitamente a false.
    private boolean secure = true;
    private String sameSite;
    private CookieSettings access = new CookieSettings();
    private CookieSettings refresh = new CookieSettings();

    @Getter
    @Setter
    public static class CookieSettings {
        private String name;
        private String path;
        private long maxAge;
    }
}
