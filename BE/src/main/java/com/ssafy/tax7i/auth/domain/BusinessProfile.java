package com.ssafy.tax7i.auth.domain;

import com.ssafy.tax7i.global.crypto.AesEncryptor;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "business_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Convert(converter = AesEncryptor.class)
    @Column(length = 512)
    private String businessRegNumber;

    private String businessName;

    private String industryCode;

    @Column(nullable = false)
    private Boolean ntsVerified;

    private LocalDateTime ntsVerifiedAt;

    @Builder
    public BusinessProfile(User user, String businessRegNumber, String businessName, String industryCode) {
        this.user = user;
        this.businessRegNumber = businessRegNumber;
        this.businessName = businessName;
        this.industryCode = industryCode;
        this.ntsVerified = false;
    }

    public void verifyNts() {
        this.ntsVerified = true;
        this.ntsVerifiedAt = LocalDateTime.now();
    }
}
