package com.adorsys.webank.serviceimpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHashingServiceTest {

    private PasswordHashingService passwordHashingService;

    @BeforeEach
    void setUp() {
        passwordHashingService = new PasswordHashingService();
    }

    @Test
    void testHash_GeneratesHashWithDifferentSalts() {
        // Given
        String input = "test-password";
        
        // When
        String hash1 = passwordHashingService.hash(input);
        String hash2 = passwordHashingService.hash(input);
        
        // Then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2, "Hashes should be different due to different salts");
    }

    @Test
    void testVerify_CorrectPasswordReturnsTrue() {
        // Given
        String rawPassword = "correct-password";
        String encodedPassword = passwordHashingService.hash(rawPassword);
        
        // When
        boolean result = passwordHashingService.verify(rawPassword, encodedPassword);
        
        // Then
        assertTrue(result, "Verification should succeed with correct password");
    }

    @Test
    void testVerify_IncorrectPasswordReturnsFalse() {
        // Given
        String correctPassword = "correct-password";
        String wrongPassword = "wrong-password";
        String encodedPassword = passwordHashingService.hash(correctPassword);
        
        // When
        boolean result = passwordHashingService.verify(wrongPassword, encodedPassword);
        
        // Then
        assertFalse(result, "Verification should fail with incorrect password");
    }

    @Test
    void testNeedsUpgrade_InitiallyReturnsFalse() {
        // Given
        String password = "test-password";
        String encodedPassword = passwordHashingService.hash(password);
        
        // When
        boolean result = passwordHashingService.needsUpgrade(encodedPassword);
        
        // Then
        assertFalse(result, "Newly created hash should not need an upgrade");
    }
}
