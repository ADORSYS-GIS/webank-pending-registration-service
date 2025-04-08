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
    @Column(name = "\"AccountId\"", nullable = false)
    private String accountId;

    @Lob
    @Column(name = "front_id", nullable = true)
    private String frontID;

    @Lob
    @Column(name = "back_id", nullable = true)
    private String backID;

    @Lob
    @Column(name = "selfie_id", nullable = true)
    private String selfieID;

    @Lob
    @Column(name = "tax_id", nullable = true)
    private String taxID;
}

