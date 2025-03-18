package com.adorsys.webank.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "user_documents")
public class UserDocumentsEntity {

    @Id
    @Column(name = "public_key_hash", length = 255, nullable = false)
    private String publicKeyHash;

    @Lob
    @Column(name = "front_id", nullable = false)
    private byte[] frontID;

    @Lob
    @Column(name = "back_id", nullable = false)
    private byte[] backID;

    @Lob
    @Column(name = "selfie_id", nullable = false)
    private byte[] selfieID;

    @Lob
    @Column(name = "tax_id", nullable = false)
    private byte[] taxID;
}

