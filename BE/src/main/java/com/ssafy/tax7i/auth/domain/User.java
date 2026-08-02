package com.ssafy.tax7i.auth.domain;

import com.ssafy.tax7i.global.crypto.AesEncryptor;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_phone_last4", columnList = "phone_last4")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String ci;

    @Column(nullable = false, unique = true, length = 128)
    private String di;

    @Convert(converter = AesEncryptor.class)
    @Column(length = 512)
    private String name;

    private LocalDate birthDate;

    private String gender;

    @Convert(converter = AesEncryptor.class)
    @Column(length = 512)
    private String phoneNumber;

    @Column(length = 4)
    private String phoneLast4;

    @Column(length = 200)
    private String pinHash;

    @Column(length = 200)
    private String payPinHash;

    @Column(nullable = false)
    private Boolean biometricEnabled = false;

    private String deviceId;

    @Column(nullable = false)
    private Boolean kycVerified = false;

    @Column(nullable = false)
    private Boolean isBusiness = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 100)
    private String ssafyUserKey;

    private LocalDateTime lastLoginAt;

    @Builder
    public User(String ci, String di, String name,
                LocalDate birthDate, String gender,
                String phoneNumber, String phoneLast4, String ssafyUserKey) {
        this.ci = ci;
        this.di = di;
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.phoneLast4 = phoneLast4;
        this.ssafyUserKey = ssafyUserKey;
    }

    public void setupPin(String pinHash) {
        this.pinHash = pinHash;
    }

    public void setupPayPin(String payPinHash) {
        this.payPinHash = payPinHash;
    }

    public boolean hasPayPin() {
        return this.payPinHash != null;
    }

    public void registerDevice(String deviceId) {
        this.deviceId = deviceId;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void markKycVerified() {
        this.kycVerified = true;
    }

    public void markBusiness() {
        this.isBusiness = true;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    public void assignSsafyUserKey(String ssafyUserKey) {
        this.ssafyUserKey = ssafyUserKey;
    }
}
