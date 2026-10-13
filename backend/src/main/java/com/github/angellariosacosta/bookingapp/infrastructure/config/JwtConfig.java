package com.github.angellariosacosta.bookingapp.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;

/**
 * Firma y validación del JWT con HS256 (HMAC-SHA256, secreto simétrico). La app es un monolito: el mismo
 * proceso emite y valida los tokens, así que un único secreto basta. Ver docs/jwt-secret.md.
 */
@Configuration
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String base64Secret) {
        return JwtSecretKeys.fromBase64(base64Secret);
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    // issuer = claim "iss" del JWT: identifica quién EMITIÓ el token (en este caso, esta misma
    // aplicación/authorization server). El decoder compara el "iss" que trae el token contra
    // este valor; si no coincide, lo rechaza. Sirve para que un token firmado por otro emisor
    // (aunque use un algoritmo/llave compatible) no sea aceptado como válido.
    //
    // audience = claim "aud" del JWT: identifica para QUIÉN está destinado el token (qué
    // API/servicio consumidor debe aceptarlo). El decoder compara el "aud" del token contra este
    // valor; si no coincide, lo rechaza. Sirve para evitar que un token válido, emitido
    // pensando en otro servicio/cliente, se reutilice aquí.
    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience
    ) {
        // macAlgorithm fija HS256: un token con otro "alg" en el header (none, RS256...) se rechaza.
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Validación explícita de exp/iss/aud además de la firma HS256.
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(issuer),
                new JwtAudienceValidator(audience)
        );
        decoder.setJwtValidator(validator);

        return decoder;
    }
}
