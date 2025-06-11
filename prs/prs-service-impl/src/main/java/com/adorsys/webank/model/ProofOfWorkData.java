package com.adorsys.webank.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.jwk.JWK;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProofOfWorkData {
    @JsonProperty("initiationNonce")
    private String initiationNonce;

    @JsonProperty("devicePub")
    private Object devicePub;

    @JsonProperty("powNonce")
    private String powNonce;

    public static ProofOfWorkData create(String initiationNonce, JWK devicePub, String powNonce) {
        return new ProofOfWorkData(initiationNonce, devicePub.toJSONObject(), powNonce);
    }
}
