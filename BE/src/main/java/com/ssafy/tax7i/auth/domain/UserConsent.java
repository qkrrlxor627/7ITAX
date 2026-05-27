package com.ssafy.tax7i.auth.domain;

import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_consents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "consentType"}),
        indexes = @Index(name = "idx_consent_user_id", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserConsent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentType consentType;

    @Column(nullable = false)
    private Boolean consented;

    private LocalDateTime consentedAt;

    private LocalDateTime revokedAt;

    @Builder
    public UserConsent(User user, ConsentType consentType, Boolean consented) {
        this.user = user;
        this.consentType = consentType;
        this.consented = consented;
        this.consentedAt = consented ? LocalDateTime.now() : null;
    }

    public void consent() {
        this.consented = true;
        this.consentedAt = LocalDateTime.now();
        this.revokedAt = null;
    }

    public void revoke() {
        this.consented = false;
        this.revokedAt = LocalDateTime.now();
    }
}
