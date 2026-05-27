package com.ssafy.tax7i.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.fcm.entity.FcmToken;
import com.ssafy.tax7i.fcm.repository.FcmTokenRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;
    private final Optional<FirebaseMessaging> firebaseMessaging;

    @Transactional
    public void saveToken(Long userId, String token, String deviceInfo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<FcmToken> existing = fcmTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            existing.get().updateUser(user, deviceInfo);
        } else {
            FcmToken fcmToken = FcmToken.builder()
                    .user(user)
                    .token(token)
                    .deviceInfo(deviceInfo)
                    .build();
            fcmTokenRepository.save(fcmToken);
        }
        log.info("FCM 토큰 저장: userId={}, deviceInfo={}", userId, deviceInfo);
    }

    @Transactional
    public void removeToken(Long userId, String token) {
        FcmToken fcmToken = fcmTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!fcmToken.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        fcmTokenRepository.deleteByToken(token);
        log.info("FCM 토큰 삭제: userId={}", userId);
    }

    public void sendNotification(Long userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging.isEmpty()) {
            log.debug("FCM 비활성 상태: userId={}, title={}", userId, title);
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUser_Id(userId);
        if (tokens.isEmpty()) {
            log.debug("FCM 토큰 없음: userId={}", userId);
            return;
        }

        for (FcmToken fcmToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(fcmToken.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data)
                        .build();
                firebaseMessaging.get().send(message);
                log.debug("FCM 발송 성공: userId={}, title={}", userId, title);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                    fcmTokenRepository.deleteByToken(fcmToken.getToken());
                    log.info("만료된 FCM 토큰 삭제: userId={}", userId);
                } else {
                    log.warn("FCM 발송 실패: userId={}, error={}", userId, e.getMessage());
                }
            } catch (Exception e) {
                log.warn("FCM 발송 실패: userId={}, error={}", userId, e.getMessage());
            }
        }
    }
}
