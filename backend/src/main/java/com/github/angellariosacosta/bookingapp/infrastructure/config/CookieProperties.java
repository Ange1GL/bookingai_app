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
    private boolean secure;
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
