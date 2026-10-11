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
 * Carga las llaves RSA del JWT desde texto PEM (variable de entorno inyectada por ECS desde
 * Parameter Store) o, si no hay texto, desde un recurso (archivo en el classpath para desarrollo).
 */
final class JwtKeyLoader {

    private JwtKeyLoader() {
    }

    static RSAPrivateKey privateKey(String pem, Resource fallback) {
        return load(pem, fallback, RsaKeyConverters.pkcs8()::convert);
    }

    static RSAPublicKey publicKey(String pem, Resource fallback) {
        return load(pem, fallback, RsaKeyConverters.x509()::convert);
    }

    private static <K> K load(String pem, Resource fallback, KeyParser<K> parser) {
        try (InputStream source = open(pem, fallback)) {
            return parser.parse(source);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la llave JWT desde " + fallback, e);
        }
    }

    private static InputStream open(String pem, Resource fallback) throws IOException {
        if (pem != null && !pem.isBlank()) {
            return new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
        }
        return fallback.getInputStream();
    }

    @FunctionalInterface
    private interface KeyParser<K> {
        K parse(InputStream source);
    }
}
