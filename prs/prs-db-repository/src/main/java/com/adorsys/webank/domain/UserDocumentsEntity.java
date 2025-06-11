package com.adorsys.webank.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.*;
@Builder
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Getter
@Setter
@Table(name = "user_documents", indexes = {
    @Index(name = "idx_account_id", columnList = "account_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class UserDocumentsEntity {

    @Id
    @Column(name = "account_id", nullable = false)
    private String accountId;


    @Column(name = "front_id", nullable = true, columnDefinition = "TEXT")
    private String frontID;


    @Column(name = "back_id", nullable = true, columnDefinition = "TEXT")
    private String backID;


    @Column(name = "selfie_id", nullable = true, columnDefinition = "TEXT")
    private String selfieID;


    @Column(name = "tax_id", nullable = true, columnDefinition = "TEXT")
    private String taxID;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = true)
    private UserDocumentsStatus status;
}
