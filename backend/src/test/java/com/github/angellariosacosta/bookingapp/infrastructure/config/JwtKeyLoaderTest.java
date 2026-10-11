package com.github.angellariosacosta.bookingapp.infrastructure.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyLoaderTest {

    private static final int RSA_KEY_SIZE = 2048;
    private static final Base64.Encoder PEM_ENCODER = Base64.getMimeEncoder(64, "\n".getBytes());

    private static KeyPair keyPair;
    private static String privatePem;
    private static String publicPem;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE);
        keyPair = generator.generateKeyPair();
        privatePem = pem("PRIVATE KEY", keyPair.getPrivate().getEncoded());
        publicPem = pem("PUBLIC KEY", keyPair.getPublic().getEncoded());
    }

    @Test
    void loadsKeysFromPemText() {
        Resource unused = new ByteArrayResource(new byte[0]);

        RSAPrivateKey privateKey = JwtKeyLoader.privateKey(privatePem, unused);
        RSAPublicKey publicKey = JwtKeyLoader.publicKey(publicPem, unused);

        assertThat(privateKey.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
        assertThat(publicKey.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    }

    @Test
    void fallsBackToResourceWhenPemIsBlank() {
        Resource privateResource = new ByteArrayResource(privatePem.getBytes());
        Resource publicResource = new ByteArrayResource(publicPem.getBytes());

        RSAPrivateKey privateKey = JwtKeyLoader.privateKey("", privateResource);
        RSAPublicKey publicKey = JwtKeyLoader.publicKey(null, publicResource);

        assertThat(privateKey.getEncoded()).isEqualTo(keyPair.getPrivate().getEncoded());
        assertThat(publicKey.getEncoded()).isEqualTo(keyPair.getPublic().getEncoded());
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n" + PEM_ENCODER.encodeToString(der) + "\n-----END " + type + "-----\n";
    }
}
