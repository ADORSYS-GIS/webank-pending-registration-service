package com.adorsys.webank.serviceimpl;

import lombok.Getter;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Centralized service for password hashing operations using Argon2.
 * This class replaces the previous implementation that used SHA-256 with static salt.
 */
@Service
public class PasswordHashingService {
    // Constants for Argon2 configuration
    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int PARALLELISM = 1;
    private static final int MEMORY = 4096; // 4MB
    private static final int ITERATIONS = 2;
    
    @Getter
    private final PasswordEncoder passwordEncoder;

    public PasswordHashingService() {
        // Configure Argon2 with proper security parameters
        this.passwordEncoder = new Argon2PasswordEncoder(
            SALT_LENGTH,
            HASH_LENGTH,
            PARALLELISM,
            MEMORY,
            ITERATIONS
        );
    }

    /**
     * Hashes the provided input using Argon2.
     *
     * @param input The input to hash (password, token, etc.)
     * @return The Argon2 hash with salt included
     */
    public String hash(String input) {
        return passwordEncoder.encode(input);
    }

    /**
     * Verifies if the provided input matches the stored hash.
     *
     * @param input The input to verify (password, token, etc.)
     * @param hash  The stored hash to verify against
     * @return true if the input matches the hash, false otherwise
     */
    public boolean verify(String input, String hash) {
        return passwordEncoder.matches(input, hash);
    }

    /**
     * Checks if a hash needs to be upgraded based on current encoder settings.
     *
     * @param hash The hash to check
     * @return true if the hash needs upgrading, false otherwise
     */
    public boolean needsUpgrade(String hash) {
        return passwordEncoder.upgradeEncoding(hash);
    }
}
