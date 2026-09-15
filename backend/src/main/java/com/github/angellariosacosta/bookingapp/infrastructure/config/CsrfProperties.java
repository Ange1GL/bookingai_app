package com.github.angellariosacosta.bookingapp.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.csrf")
@Getter
@Setter
public class CsrfProperties {
    private boolean enabled = true;
}
