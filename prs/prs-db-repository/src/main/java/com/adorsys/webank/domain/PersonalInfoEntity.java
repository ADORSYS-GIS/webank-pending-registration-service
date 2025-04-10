package com.adorsys.webank.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "personal_information_table")
public class PersonalInfoEntity {

    @Id
    @Column(name = "\"AccountId\"", nullable = false)
    private String accountId;

    @Column(name = "id", nullable = false, length = 20)
    private String documentUniqueId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "date_of_birth", nullable = false, length = 20)
    private String dateOfBirth;

    @Column(name = "profession", nullable = false, length = 20)
    private String profession;

    @Column(name = "region", nullable = false, length = 20)
    private String region;

    @Column(name = "expires_on", nullable = false, updatable = false)
    private String expirationDate;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpirationDateTime;


    @Column(name = "location")
    private String location;

    @Column(name = "email", length = 30)
    private String email;

    @Column(name = "email_otp_hash")
    private String emailOtpHash;

    @Column(name = "email_otp_code")
    private String emailOtpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PersonalInfoStatus status;
}
