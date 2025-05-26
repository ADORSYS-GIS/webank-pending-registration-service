package com.adorsys.webank.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Simplified test class for JwtValidator.
 * More extensive tests would be needed in a real application.
 */
@ExtendWith(MockitoExtension.class)
public class JwtValidatorTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void extractClaim_ThrowsExceptionForInvalidToken() {
        // Arrange
        String invalidToken = "invalid.token.format";
        
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> 
            JwtValidator.extractClaim(invalidToken, "sub"));
    }
}