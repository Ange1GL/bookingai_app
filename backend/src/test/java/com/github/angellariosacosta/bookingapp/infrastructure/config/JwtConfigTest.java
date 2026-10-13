package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTest {

    private static final String ISSUER = "bookingapp-api";
    private static final String AUDIENCE = "bookingapp-frontend";
    private static final String SUBJECT = "1";

    private final JwtConfig config = new JwtConfig();
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    private static SecretKey randomSecretKey() {
        byte[] secret = new byte[JwtSecretKeys.MIN_SECRET_BYTES];
        new SecureRandom().nextBytes(secret);
        return JwtSecretKeys.fromBase64(Base64.getEncoder().encodeToString(secret));
    }

    private static String encode(JwtEncoder encoder, String issuer, String audience, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .audience(List.of(audience))
                .subject(SUBJECT)
                .issuedAt(expiresAt.minus(15, ChronoUnit.MINUTES))
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static String validToken(JwtEncoder encoder) {
        return encode(encoder, ISSUER, AUDIENCE, Instant.now().plus(15, ChronoUnit.MINUTES));
    }

    @BeforeEach
    void setUp() {
        SecretKey key = randomSecretKey();
        encoder = config.jwtEncoder(key);
        decoder = config.jwtDecoder(key, ISSUER, AUDIENCE);
    }

    @DisplayName("Should decode a token signed with HS256 by the same secret")
    @Test
    void shouldDecodeTokenSignedWithSameSecret() {
        Jwt jwt = decoder.decode(validToken(encoder));

        assertEquals("HS256", jwt.getHeaders().get("alg"));
        assertEquals(SUBJECT, jwt.getSubject());
        assertEquals(List.of(AUDIENCE), jwt.getAudience());
    }

    @DisplayName("Should reject a token signed with a different secret")
    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtEncoder otherEncoder = config.jwtEncoder(randomSecretKey());

        assertThrows(JwtException.class, () -> decoder.decode(validToken(otherEncoder)));
    }

    @DisplayName("Should reject a token from another issuer")
    @Test
    void shouldRejectTokenFromAnotherIssuer() {
        String token = encode(encoder, "otro-emisor", AUDIENCE, Instant.now().plus(15, ChronoUnit.MINUTES));

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @DisplayName("Should reject a token meant for another audience")
    @Test
    void shouldRejectTokenForAnotherAudience() {
        String token = encode(encoder, ISSUER, "otro-servicio", Instant.now().plus(15, ChronoUnit.MINUTES));

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @DisplayName("Should reject an expired token")
    @Test
    void shouldRejectExpiredToken() {
        String token = encode(encoder, ISSUER, AUDIENCE, Instant.now().minus(5, ChronoUnit.MINUTES));

        assertThrows(JwtValidationException.class, () -> decoder.decode(token));
    }

    @DisplayName("Should reject an unsigned token (alg none)")
    @Test
    void shouldRejectUnsignedToken() {
        String[] parts = validToken(encoder).split("\\.");
        String noneHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8));
        String unsignedToken = noneHeader + "." + parts[1] + ".";

        assertThrows(JwtException.class, () -> decoder.decode(unsignedToken));
    }
}
