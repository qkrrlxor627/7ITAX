package com.ssafy.tax7i.fcm.entity;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fcm_tokens", indexes = {
        @Index(name = "idx_fcm_token_user_id", columnList = "user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    private String deviceInfo;

    @Builder
    public FcmToken(User user, String token, String deviceInfo) {
        this.user = user;
        this.token = token;
        this.deviceInfo = deviceInfo;
    }

    public void updateUser(User user, String deviceInfo) {
        this.user = user;
        this.deviceInfo = deviceInfo;
    }
}
