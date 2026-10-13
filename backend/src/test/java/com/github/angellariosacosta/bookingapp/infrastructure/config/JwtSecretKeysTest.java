package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecretKeysTest {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static String randomBase64Secret(int bytes) {
        byte[] secret = new byte[bytes];
        RANDOM.nextBytes(secret);
        return Base64.getEncoder().encodeToString(secret);
    }

    @DisplayName("Should build an HmacSHA256 key when the secret has the minimum size")
    @Test
    void shouldBuildHmacKeyWhenSecretHasMinimumSize() {
        String base64Secret = randomBase64Secret(JwtSecretKeys.MIN_SECRET_BYTES);

        SecretKey key = JwtSecretKeys.fromBase64(base64Secret);

        assertEquals("HmacSHA256", key.getAlgorithm());
        assertArrayEquals(Base64.getDecoder().decode(base64Secret), key.getEncoded());
    }

    @DisplayName("Should ignore surrounding whitespace in the secret")
    @Test
    void shouldIgnoreSurroundingWhitespace() {
        String base64Secret = randomBase64Secret(JwtSecretKeys.MIN_SECRET_BYTES);

        SecretKey key = JwtSecretKeys.fromBase64("  " + base64Secret + "\n");

        assertArrayEquals(Base64.getDecoder().decode(base64Secret), key.getEncoded());
    }

    @DisplayName("Should fail when the secret is missing")
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldFailWhenSecretIsMissing(String base64Secret) {
        assertThrows(IllegalStateException.class, () -> JwtSecretKeys.fromBase64(base64Secret));
    }

    @DisplayName("Should fail when the secret is not valid base64")
    @Test
    void shouldFailWhenSecretIsNotBase64() {
        assertThrows(IllegalStateException.class, () -> JwtSecretKeys.fromBase64("no-es-base64!"));
    }

    @DisplayName("Should fail without exposing the secret when it is shorter than 256 bits")
    @Test
    void shouldFailWithoutExposingSecretWhenTooShort() {
        String shortSecret = randomBase64Secret(JwtSecretKeys.MIN_SECRET_BYTES - 1);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> JwtSecretKeys.fromBase64(shortSecret));

        assertFalse(ex.getMessage().contains(shortSecret));
    }
}
