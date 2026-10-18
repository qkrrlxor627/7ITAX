package com.ssafy.tax7i.config;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.auth.service.TestUserProvisioner;
import com.ssafy.tax7i.card.entity.CardType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class CardDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TestUserProvisioner testUserProvisioner;

    @Override
    @Transactional
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.info("[CardDataInitializer] 유저가 없어 더미 유저 2명을 생성합니다.");
            users = createTestUsers();
        }

        int targetUserCount = Math.min(users.size(), 2);
        for (int i = 0; i < targetUserCount; i++) {
            User user = users.get(i);
            // 두 번째 유저는 사업자 카드로 발급한다. 장부 자동 생성은 카드 유형이 BUSINESS일 때만 동작하므로
            // (PaymentService.publishPaymentCapturedEvent → BookEntryEventListener) 시드에 사업자 카드가 하나는 있어야
            // 결제→장부 흐름을 OTP 없이 확인할 수 있다(scripts/smoke_payment_ledger.py)
            CardType cardType = i == 1 ? CardType.BUSINESS : CardType.PERSONAL;
            try {
                if (!testUserProvisioner.provisionCardIfAbsent(user, cardType)) {
                    log.info("[CardDataInitializer] 유저 {}에 이미 카드가 있어 건너뜁니다.", user.getId());
                }
            } catch (Exception e) {
                log.error("[CardDataInitializer] 유저 {} 카드 생성 실패: {}", user.getId(), e.getMessage());
            }
        }
    }

    private List<User> createTestUsers() {
        return List.of(
                testUserProvisioner.findOrCreate("01011111111", "김싸피", LocalDate.parse("1995-03-15"), "M"),
                testUserProvisioner.findOrCreate("01022222222", "이싸피", LocalDate.parse("1998-07-22"), "F")
        );
    }
}
