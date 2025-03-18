package com.adorsys.webank.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "personal_information_table")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalInfoEntity {

    @Id
    @Column(name = "public_key_hash", nullable = false)
    private String publicKeyHash;

    @Column(name = "id", nullable = false, length = 20)
    private String documentUniqueId;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "date_of_birth", nullable = false, length = 20)
    private String dateOfBirth;

    @Column(name = "profession", nullable = false, length = 20)
    private String profession;

    @Column(name = "region", nullable = false, length = 20)
    private String region;

    @CreationTimestamp
    @Column(name = "expires_on", nullable = false, updatable = false)
    private LocalDateTime expirationDate;

    @Column(name = "location")
    private String location;

    @Column(name = "email", length = 20)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PersonalInfoStatus status;
}
