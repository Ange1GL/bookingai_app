package com.github.angellariosacosta.bookingapp.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.client-ip")
@Getter
@Setter
public class ClientIpProperties {
    /**
     * Number of trusted proxies that append to X-Forwarded-For between the client and this app
     * (0 = connect directly, use the socket address). Behind CloudFront + API Gateway it is 1.
     */
    private int trustedProxyHops = 0;
}
