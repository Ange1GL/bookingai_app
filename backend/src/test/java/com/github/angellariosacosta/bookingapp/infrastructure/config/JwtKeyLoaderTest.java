package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtKeyLoaderTest {

    private static final int KEY_SIZE = 2048;
    private static final Resource MISSING_FALLBACK = new ByteArrayResource(new byte[0], "sin respaldo");

    private static KeyPair keyPair;
    private static String privatePem;
    private static String publicPem;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(KEY_SIZE);
        keyPair = generator.generateKeyPair();
        privatePem = toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        publicPem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    static Stream<Arguments> pemFormats() {
        return Stream.of(
                Arguments.of("saltos reales (Parameter Store)", UnaryOperator.<String>identity()),
                Arguments.of("\\n literales (.env / plugin del IDE)", (UnaryOperator<String>) pem -> pem.replace("\n", "\\n")),
                Arguments.of("CRLF de Windows", (UnaryOperator<String>) pem -> pem.replace("\n", "\r\n")),
                Arguments.of("una linea con espacios", (UnaryOperator<String>) pem -> pem.replace("\n", " ")),
                Arguments.of("entre comillas", (UnaryOperator<String>) pem -> "\"" + pem.replace("\n", "\\n") + "\""),
                Arguments.of("sin encabezado ni pie", (UnaryOperator<String>) JwtKeyLoaderTest::bodyOnly));
    }

    @DisplayName("Carga ambas llaves desde texto PEM en cualquier formato")
    @ParameterizedTest(name = "{0}")
    @MethodSource("pemFormats")
    void loadsKeysFromAnyPemFormat(String format, UnaryOperator<String> transform) {
        RSAPrivateKey privateKey = JwtKeyLoader.privateKey(transform.apply(privatePem), MISSING_FALLBACK);
        RSAPublicKey publicKey = JwtKeyLoader.publicKey(transform.apply(publicPem), MISSING_FALLBACK);

        assertEquals(keyPair.getPrivate(), privateKey);
        assertEquals(keyPair.getPublic(), publicKey);
    }

    @DisplayName("Sin texto PEM usa el recurso de respaldo")
    @Test
    void fallsBackToResourceWhenPemIsBlank() {
        Resource fallback = new ByteArrayResource(publicPem.getBytes(StandardCharsets.UTF_8), "certs/public.pem");

        assertEquals(keyPair.getPublic(), JwtKeyLoader.publicKey("  ", fallback));
    }

    @DisplayName("Texto PEM invalido falla con mensaje claro")
    @Test
    void rejectsInvalidPem() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> JwtKeyLoader.publicKey("-----BEGIN PUBLIC KEY-----\nno-es-base64!\n-----END PUBLIC KEY-----", MISSING_FALLBACK));

        assertEquals("No se pudo cargar la llave JWT PUBLIC KEY desde texto PEM (variable de entorno)", error.getMessage());
    }

    private static String toPem(String label, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        return "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n";
    }

    private static String bodyOnly(String pem) {
        return pem.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "").strip();
    }
}
