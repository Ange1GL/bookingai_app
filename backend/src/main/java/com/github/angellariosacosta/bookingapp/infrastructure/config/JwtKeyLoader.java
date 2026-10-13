package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * Carga las llaves RSA del JWT desde texto PEM (variable de entorno: en AWS la inyecta ECS desde
 * Parameter Store; en local puede venir del .env o del sistema) o, si no hay texto, desde un recurso
 * (archivo en el classpath para desarrollo). El texto se normaliza con {@link PemText} antes de parsearlo.
 */
final class JwtKeyLoader {

    private static final String PRIVATE_KEY_LABEL = "PRIVATE KEY";
    private static final String PUBLIC_KEY_LABEL = "PUBLIC KEY";

    private JwtKeyLoader() {
    }

    static RSAPrivateKey privateKey(String pem, Resource fallback) {
        return load(pem, fallback, PRIVATE_KEY_LABEL, RsaKeyConverters.pkcs8()::convert);
    }

    static RSAPublicKey publicKey(String pem, Resource fallback) {
        return load(pem, fallback, PUBLIC_KEY_LABEL, RsaKeyConverters.x509()::convert);
    }

    private static <K> K load(String pem, Resource fallback, String label, KeyParser<K> parser) {
        boolean fromText = pem != null && !pem.isBlank();
        String source = fromText ? "texto PEM (variable de entorno)" : fallback.getDescription();
        try (InputStream input = fromText ? fromText(pem, label) : fallback.getInputStream()) {
            return parser.parse(input);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("No se pudo cargar la llave JWT " + label + " desde " + source, e);
        }
    }

    private static InputStream fromText(String pem, String label) {
        return new ByteArrayInputStream(PemText.canonical(pem, label).getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface KeyParser<K> {
        K parse(InputStream source);
    }
}
