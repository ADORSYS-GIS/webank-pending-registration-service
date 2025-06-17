# Hashing Strategies in the Pending Registration Service

This document provides a comprehensive overview of the cryptographic hashing strategies employed within the Pending Registration Service. The service utilizes a dual approach, leveraging both **non-deterministic** and **deterministic** hashing to meet different security requirements.

- **Non-Deterministic Hashing (Argon2)**: Used for securely storing secrets where the server is responsible for both generating and verifying the hash. The output is different for each hash operation, even with the same input, due to the use of a random salt.

- **Deterministic Hashing (SHA-256)**: Used for operations that require a consistent, reproducible output from a given input. This is essential for integrity checks and creating reliable lookup keys.

---

## 1. Non-Deterministic Hashing: Argon2

Argon2 is a modern, memory-hard key derivation function chosen for its high resistance to both GPU-based and custom hardware attacks. Its non-deterministic nature, achieved by adding a unique random salt to each password before hashing, is ideal for protecting stored secrets.

### Configuration

The `PasswordEncoder` is configured in `PasswordEncoderConfig.java` to use `Argon2PasswordEncoder`.

```java
// prs/prs-service-impl/src/main/java/com/adorsys/webank/config/PasswordEncoderConfig.java

@Bean
@Primary
public PasswordEncoder passwordEncoder() {
    String encodingId = "argon2";
    // saltLength=16, hashLength=32, parallelism=1, memory=4096KB, iterations=2
    Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 4096, 2);
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put(encodingId, argon2);
    return new DelegatingPasswordEncoder(encodingId, encoders);
}
```

### Service-Level Usage

The `PasswordEncoder` is injected into multiple services to handle various security tasks beyond user passwords.

#### `DeviceRegServiceImpl`: Time-Based Nonce Generation
To prevent replay attacks during device registration, the service generates a time-limited nonce. Argon2 is used to hash a timestamp, ensuring the nonce is secure and cannot be easily predicted.

- **Generation**: `generateNonce()` hashes the current timestamp (flattened to a 15-minute interval).
- **Validation**: `validateNonceTimestamp()` checks if a provided nonce matches the hash of the current time window, ensuring the request is recent.

#### `OtpServiceImpl`: Securing OTP Validation
To validate a One-Time Password (OTP), the service hashes a canonical JSON object containing the OTP, the device's public key, and the user's phone number. Hashing this data together ensures that an OTP is tied to a specific user and device.

- **Usage**: The `passwordEncoder.encode()` method is called on the canonical JSON string. Later, `passwordEncoder.matches()` is used to verify the OTP provided by the user against the stored hash.

#### `EmailOtpServiceImpl`: Securing Email OTPs
This service follows a similar pattern to `OtpServiceImpl`. It hashes a JSON object containing the email OTP and the `accountId` to create a secure token for email verification.

- **Usage**: The `computeOtpHash()` method uses `passwordEncoder.encode()` on the canonical data. The `validateOtp()` method uses `passwordEncoder.matches()` for verification.

---

## 2. Deterministic Hashing: SHA-256

For scenarios where a consistent, reproducible hash is required, the application uses SHA-256. This is critical when a hash needs to be recalculated and matched, either for data integrity checks or as a reliable database lookup key.

### Implementation

Deterministic hashing is implemented via helper methods within the services, typically named `computeHash` or `calculateSHA256`.

```java
// Example from DeviceRegServiceImpl.java
String calculateSHA256(String input) throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
    // ... converts bytes to hex string ...
}
```

### Service-Level Usage

#### `DeviceRegServiceImpl`: Proof-of-Work Validation
During device validation, the client performs a proof-of-work (PoW) and sends the resulting hash (`powHash`) to the server. The server must be able to reproduce this hash deterministically to verify the client's work.

- **Usage**: The `validateProofOfWork()` method calls `calculateSHA256()` on the canonicalized JSON payload received from the client and compares the result to the `powHash` provided in the request.

#### `OtpServiceImpl`: Creating a Database Lookup Key
To find OTP records efficiently without storing personally identifiable information directly as a key, the service deterministically hashes the device's public key.

- **Usage**: The `computeHash()` method (using SHA-256) is called on the device's public key JWK. The resulting hash is used as a `publicKeyHash` to query the `OtpRequestRepository`. This allows the system to quickly find if an OTP request already exists for a given device.
