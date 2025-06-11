/*
 * Copyright (c) 2018-2023 adorsys GmbH and Co. KG
 * All rights are reserved.
 */

package com.adorsys.webank.model;

import com.nimbusds.jose.jwk.JWK;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data structure for OTP information used for sending and validating OTPs.
 * Replaces the previous Map-based structure for better type safety.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpData {
    private String otp;
    private JWK devicePub;
    private String phoneNumber;
    private String salt;
}