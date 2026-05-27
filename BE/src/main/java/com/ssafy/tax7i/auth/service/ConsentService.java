package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.ConsentType;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.domain.UserConsent;
import com.ssafy.tax7i.auth.dto.ConsentRequest;
import com.ssafy.tax7i.auth.repository.UserConsentRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentService {

    private final UserConsentRepository userConsentRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveConsents(Long userId, List<ConsentRequest> requests) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Map<ConsentType, UserConsent> existingMap = userConsentRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserConsent::getConsentType, Function.identity()));

        for (ConsentRequest req : requests) {
            UserConsent existing = existingMap.get(req.consentType());

            if (existing != null) {
                if (req.agreed()) {
                    existing.consent();
                } else {
                    existing.revoke();
                }
            } else {
                userConsentRepository.save(UserConsent.builder()
                        .user(user)
                        .consentType(req.consentType())
                        .consented(req.agreed())
                        .build());
            }
        }
    }

    public List<UserConsent> getConsents(Long userId) {
        return userConsentRepository.findByUserId(userId);
    }

    @Transactional
    public void revokeConsent(Long userId, ConsentType consentType) {
        UserConsent consent = userConsentRepository.findByUserIdAndConsentType(userId, consentType)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "동의 정보를 찾을 수 없습니다."));
        consent.revoke();
    }
}
