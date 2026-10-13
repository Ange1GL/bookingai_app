package com.github.angellariosacosta.bookingapp.infrastructure.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Convierte el secreto HS256 del JWT (texto base64 en {@code JWT_SECRET}: en dev desde el .env, en QA inyectado
 * por ECS desde Parameter Store) en una {@link SecretKey}. Se valida al arrancar para fallar rápido con un
 * mensaje claro; los mensajes nunca incluyen el valor del secreto.
 */
final class JwtSecretKeys {

    // HS256 exige una clave de al menos 256 bits (RFC 7518, sección 3.2).
    static final int MIN_SECRET_BYTES = 32;
    private static final String HMAC_SHA256 = "HmacSHA256";

    private JwtSecretKeys() {
    }

    static SecretKey fromBase64(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("El secreto del JWT (JWT_SECRET) no está definido");
        }
        byte[] secret = decode(base64Secret.strip());
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("El secreto del JWT (JWT_SECRET) debe tener al menos "
                    + MIN_SECRET_BYTES + " bytes decodificados; tiene " + secret.length);
        }
        return new SecretKeySpec(secret, HMAC_SHA256);
    }

    private static byte[] decode(String base64Secret) {
        try {
            return Base64.getDecoder().decode(base64Secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("El secreto del JWT (JWT_SECRET) no es base64 válido", e);
        }
    }
}
