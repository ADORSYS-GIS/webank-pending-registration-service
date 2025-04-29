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
@Table(name = "user_documents")
public class UserDocumentsEntity {

    @Id
    @Column(name = "account_id", nullable = false)
    private String accountId;


    @Column(name = "front_id", nullable = true)
    private String frontID;


    @Column(name = "back_id", nullable = true)
    private String backID;


    @Column(name = "selfie_id", nullable = true)
    private String selfieID;


    @Column(name = "tax_id", nullable = true)
    private String taxID;
}
