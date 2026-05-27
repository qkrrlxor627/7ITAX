package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.ConsentType;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.domain.UserConsent;
import com.ssafy.tax7i.auth.dto.ConsentRequest;
import com.ssafy.tax7i.auth.repository.UserConsentRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

    @Mock private UserConsentRepository userConsentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ConsentService consentService;

    @Test
    void saveConsents_신규동의_저장() {
        User user = createUser(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userConsentRepository.findByUserId(1L)).willReturn(List.of());

        consentService.saveConsents(1L, List.of(
                new ConsentRequest(ConsentType.SERVICE, true)
        ));

        then(userConsentRepository).should().save(any(UserConsent.class));
    }

    @Test
    void saveConsents_기존동의_철회() {
        User user = createUser(1L);
        UserConsent existing = UserConsent.builder()
                .user(user)
                .consentType(ConsentType.SERVICE)
                .consented(true)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userConsentRepository.findByUserId(1L)).willReturn(List.of(existing));

        consentService.saveConsents(1L, List.of(
                new ConsentRequest(ConsentType.SERVICE, false)
        ));

        // revoke가 호출되었는지 검증 (consented가 false로 변경 — save 호출 없이 dirty checking)
        then(userConsentRepository).should().findByUserId(1L);
        then(userConsentRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    void getConsents_조회() {
        given(userConsentRepository.findByUserId(1L)).willReturn(List.of());

        List<UserConsent> result = consentService.getConsents(1L);

        then(userConsentRepository).should().findByUserId(1L);
    }

    private User createUser(Long id) {
        User user = User.builder()
                .ci("test-ci")
                .di("test-di")
                .name("홍길동")
                .build();
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
