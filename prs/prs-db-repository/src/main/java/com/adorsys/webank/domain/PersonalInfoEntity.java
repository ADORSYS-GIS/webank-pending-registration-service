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
    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "document_id", nullable = true, length = 20)
    private String documentUniqueId;

    @Column(name = "document_expiration_date", nullable = true, length = 20)
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
    @Column(name = "status", nullable = true)
    private PersonalInfoStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}
