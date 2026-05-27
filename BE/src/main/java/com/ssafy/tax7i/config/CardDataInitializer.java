package com.ssafy.tax7i.config;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyCreditCardClient;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateCreditCardResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreditCardProductListResponse;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyAccountListResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateAccountResponse;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardDataInitializer implements CommandLineRunner {

    private static final String DEFAULT_ACCOUNT_TYPE = "001-1-c7a14e921c7a44";

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final SsafyCreditCardClient ssafyCreditCardClient;
    private final SsafyFinanceClient ssafyFinanceClient;

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

            List<Card> existingCards = cardRepository.findByUser_IdAndDeletedFalse(user.getId());
            if (!existingCards.isEmpty()) {
                log.info("[CardDataInitializer] 유저 {}에 이미 카드 {}개 존재, 건너뜁니다.", user.getId(), existingCards.size());
                continue;
            }

            if (user.getSsafyUserKey() == null) {
                try {
                    String memberId = "tax7i-user-" + user.getId() + "@tax7i.dev";
                    String userKey = ssafyFinanceClient.getOrRegisterMember(memberId);
                    user.assignSsafyUserKey(userKey);
                    userRepository.save(user);
                    log.info("[CardDataInitializer] 유저 {} ssafyUserKey 등록 완료: {}", user.getId(), userKey);
                } catch (Exception e) {
                    log.error("[CardDataInitializer] 유저 {} SSAFY 멤버 등록 실패, 건너뜁니다: {}", user.getId(), e.getMessage());
                    continue;
                }
            }

            String userKey = user.getSsafyUserKey();

            try {
                // 카드 상품 목록 조회
                SsafyCreditCardProductListResponse products = ssafyCreditCardClient.getCreditCardProducts(userKey);
                if (products.rec() == null || products.rec().isEmpty()) {
                    log.warn("[CardDataInitializer] 카드 상품이 없습니다. 카드 생성을 건너뜁니다.");
                    continue;
                }

                // 사용자의 수시입출금 계좌 조회 (없으면 자동 생성)
                SsafyAccountListResponse accountList = ssafyFinanceClient.getAccountList(userKey);
                String withdrawalAccountNo;
                if (accountList.rec() == null || accountList.rec().isEmpty()) {
                    log.info("[CardDataInitializer] 유저 {}의 계좌가 없어 수시입출금 계좌를 생성합니다.", user.getId());
                    SsafyCreateAccountResponse accountResponse = ssafyFinanceClient.createAccount(userKey, DEFAULT_ACCOUNT_TYPE);
                    if (accountResponse.rec() == null) {
                        log.error("[CardDataInitializer] 유저 {} 계좌 생성 응답이 비어있습니다.", user.getId());
                        continue;
                    }
                    withdrawalAccountNo = accountResponse.rec().accountNo();
                    log.info("[CardDataInitializer] 유저 {} 계좌 생성 완료: {}", user.getId(), withdrawalAccountNo);
                } else {
                    withdrawalAccountNo = accountList.rec().get(0).accountNo();
                }

                // 첫 번째 카드 상품으로 카드 발급
                SsafyCreditCardProductListResponse.CreditCardProductRec product = products.rec().get(0);

                SsafyCreateCreditCardResponse response = ssafyCreditCardClient.createCreditCard(
                        userKey, product.cardUniqueNo(), withdrawalAccountNo, "4");

                SsafyCreateCreditCardResponse.CreditCardRec rec = response.rec();
                String cardNo = rec.cardNo();
                String last4 = cardNo.substring(cardNo.length() - 4);

                Card card = Card.builder()
                        .user(user)
                        .cardName(product.cardName() != null ? product.cardName() : "개인카드")
                        .cardType(CardType.PERSONAL)
                        .last4Digits(last4)
                        .cardNo(cardNo)
                        .cvc(rec.cvc())
                        .cardUniqueNo(rec.cardUniqueNo())
                        .ssafyAccountNo(withdrawalAccountNo)
                        .withdrawalAccountNo(rec.withdrawalAccountNo())
                        .withdrawalDate(rec.withdrawalDate())
                        .cardExpiryDate(rec.cardExpiryDate())
                        .build();
                card.markDefault();
                cardRepository.save(card);

                log.info("[CardDataInitializer] 유저 {} 신용카드 생성 완료: {} - 카드번호 끝 4자리: {}",
                        user.getId(), card.getCardName(), last4);
            } catch (Exception e) {
                log.error("[CardDataInitializer] 유저 {} 카드 생성 실패: {}", user.getId(), e.getMessage());
            }
        }
    }

    private List<User> createTestUsers() {
        record TestUserSeed(String name, String birthDate, String gender, String phoneNumber) {}

        List<TestUserSeed> seeds = List.of(
                new TestUserSeed("김싸피", "1995-03-15", "M", "01011111111"),
                new TestUserSeed("이싸피", "1998-07-22", "F", "01022222222")
        );

        List<User> created = new java.util.ArrayList<>();
        for (TestUserSeed seed : seeds) {
            String ci = "test-ci-" + seed.phoneNumber();
            if (userRepository.findByCi(ci).isPresent()) {
                continue;
            }
            User user = userRepository.save(
                    User.builder()
                            .ci(ci)
                            .di("test-di-" + seed.phoneNumber())
                            .name(seed.name())
                            .birthDate(LocalDate.parse(seed.birthDate()))
                            .gender(seed.gender())
                            .phoneNumber(seed.phoneNumber())
                            .phoneLast4(seed.phoneNumber().substring(seed.phoneNumber().length() - 4))
                            .build()
            );
            log.info("[CardDataInitializer] 더미 유저 생성: {} (id={})", seed.name(), user.getId());
            created.add(user);
        }
        return created;
    }
}
