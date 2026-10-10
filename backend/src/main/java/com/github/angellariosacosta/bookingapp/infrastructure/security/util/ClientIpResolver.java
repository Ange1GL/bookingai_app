package com.github.angellariosacosta.bookingapp.infrastructure.security.util;

import com.github.angellariosacosta.bookingapp.infrastructure.config.ClientIpProperties;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the real client IP behind trusted proxies (CloudFront, API Gateway).
 * Each trusted proxy appends the address it saw to X-Forwarded-For, so the entries added by
 * infrastructure we trust are the LAST ones; anything before them is client-controlled and spoofable.
 * Counting from the right by the configured hops avoids trusting forged leading entries.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String ENTRY_SEPARATOR = ",";

    private final ClientIpProperties properties;

    public String resolve(HttpServletRequest request) {
        int hops = properties.getTrustedProxyHops();
        String header = request.getHeader(FORWARDED_FOR_HEADER);
        if (hops <= 0 || header == null || header.isBlank()) {
            return request.getRemoteAddr();
        }
        String[] entries = header.split(ENTRY_SEPARATOR);
        int clientIndex = entries.length - 1 - hops;
        if (clientIndex < 0) {
            return request.getRemoteAddr();
        }
        String candidate = entries[clientIndex].trim();
        return candidate.isEmpty() ? request.getRemoteAddr() : candidate;
    }
}
