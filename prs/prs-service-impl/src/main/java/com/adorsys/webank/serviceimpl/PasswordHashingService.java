package com.adorsys.webank.serviceimpl;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Centralized service for password hashing operations using Argon2.
 * This class replaces the previous implementation that used SHA-256 with static salt.
 */
@Service
public class PasswordHashingService {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashingService() {
        // Configure Argon2 with appropriate parameters
        // - saltLength: 16 bytes of salt (recommended minimum)
        // - hashLength: 32 bytes for the hash output
        // - parallelism: 1 for minimal resource usage (can be increased based on server capabilities)
        // - memory: 4096 (4MB) - adjust based on server resources
        // - iterations: 2 - adjust based on server resources and security needs
        this.passwordEncoder = new Argon2PasswordEncoder(
                16,  // salt length
                32,  // hash length
                1,   // parallelism
                4096, // memory cost
                2    // iterations
        );
    }

    /**
     * Hashes the input string using Argon2.
     * Argon2PasswordEncoder automatically generates and incorporates a random salt for each hash.
     *
     * @param input The string to be hashed
     * @return The hashed string with the salt embedded
     */
    public String hash(String input) {
        return passwordEncoder.encode(input);
    }

    /**
     * Verifies if the raw input matches the encoded hash.
     * Argon2PasswordEncoder automatically extracts the salt from the encoded hash for verification.
     *
     * @param rawInput The raw input to check
     * @param encodedHash The encoded hash to compare against
     * @return true if the input matches the hash, false otherwise
     */
    public boolean verify(String rawInput, String encodedHash) {
        return passwordEncoder.matches(rawInput, encodedHash);
    }

    /**
     * Validates if a hash needs upgrading based on the current encoder settings.
     * Useful for handling password upgrades if the encoding parameters change.
     *
     * @param encodedHash The hash to check
     * @return true if the hash needs upgrading, false otherwise
     */
    public boolean needsUpgrade(String encodedHash) {
        return passwordEncoder.upgradeEncoding(encodedHash);
    }
}
