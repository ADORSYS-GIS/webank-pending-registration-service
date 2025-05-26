package com.adorsys.webank;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(
    classes = { TestConfig.class }
)
public class PasswordEncoderTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testPasswordEncoderIsConfigured() {
        // Verify that the PasswordEncoder bean is injected
        assertNotNull(passwordEncoder, "PasswordEncoder should be available");

        // Test encoding
        String rawPassword = "testPassword123";
        String encoded = passwordEncoder.encode(rawPassword);

        // Verify that encoding produces a non-null result
        assertNotNull(encoded, "Encoded password should not be null");

        // Verify that the encoded string starts with the Argon2 prefix
        assertTrue(
            encoded.startsWith("{argon2}"),
            "Encoded password should use Argon2"
        );

        // Verify that matches works correctly
        assertTrue(
            passwordEncoder.matches(rawPassword, encoded),
            "Password matching should work with the encoded password"
        );

        // Verify that an incorrect password doesn't match
        assertFalse(
            passwordEncoder.matches("wrongPassword", encoded),
            "Password matching should fail with incorrect password"
        );
    }
}
