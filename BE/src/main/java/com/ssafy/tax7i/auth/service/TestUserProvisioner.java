package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyCreditCardClient;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyAccountListResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreateCreditCardResponse;
import com.ssafy.tax7i.banking.client.dto.SsafyCreditCardProductListResponse;
import com.ssafy.tax7i.banking.dto.AccountCreateRequest;
import com.ssafy.tax7i.banking.dto.AccountResponse;
import com.ssafy.tax7i.banking.entity.AccountType;
import com.ssafy.tax7i.banking.service.AccountService;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 테스트 사용자 준비: 전화번호로 사용자를 찾거나 만들고, 계좌(500만원)와 기본 카드를 발급한다.
 * 시드(CardDataInitializer)와 간편 로그인(AuthService.simpleLogin)이 같이 쓴다. 본인인증·OTP 없이 동작하므로
 * 운영에서는 호출 경로가 app.test-login.enabled / app.seed.enabled로 막혀 있어야 한다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestUserProvisioner {

    private static final String DEFAULT_ACCOUNT_TYPE = "001-1-c7a14e921c7a44";

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final SsafyCreditCardClient ssafyCreditCardClient;
    private final SsafyFinanceClient ssafyFinanceClient;
    private final AccountService accountService;

    /**
     * 전화번호가 같은 기존 사용자(본인인증 가입자 포함)를 우선 찾고, 없으면 시드와 같은 규칙(ci = test-ci-{전화번호})으로 만든다
     */
    @Transactional
    public User findOrCreate(String phoneNumber, String name, LocalDate birthDate, String gender) {
        String phoneLast4 = phoneNumber.substring(phoneNumber.length() - 4);
        Optional<User> existing = userRepository.findByPhoneLast4(phoneLast4).stream()
                .filter(u -> phoneNumber.equals(u.getPhoneNumber()))
                .findFirst();
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = userRepository.save(User.builder()
                .ci("test-ci-" + phoneNumber)
                .di("test-di-" + phoneNumber)
                .name(name)
                .birthDate(birthDate)
                .gender(gender)
                .phoneNumber(phoneNumber)
                .phoneLast4(phoneLast4)
                .build());
        log.info("[TestUserProvisioner] 테스트 사용자 생성: {} (id={})", name, user.getId());
        return user;
    }

    /**
     * 카드가 하나도 없으면 계좌(없을 때만)와 기본 카드를 발급한다. 이미 카드가 있으면 아무것도 하지 않는다.
     * 장부 자동 생성은 카드 유형이 BUSINESS일 때만 동작한다(PaymentService → BookEntryEventListener)
     *
     * @return 새로 발급했으면 true
     */
    @Transactional
    public boolean provisionCardIfAbsent(User user, CardType cardType) {
        if (!cardRepository.findByUser_IdAndDeletedFalse(user.getId()).isEmpty()) {
            return false;
        }
        boolean business = cardType == CardType.BUSINESS;

        if (user.getSsafyUserKey() == null) {
            String memberId = "tax7i-user-" + user.getId() + "@tax7i.dev";
            user.assignSsafyUserKey(ssafyFinanceClient.getOrRegisterMember(memberId));
            userRepository.save(user);
        }
        String userKey = user.getSsafyUserKey();

        SsafyCreditCardProductListResponse products = ssafyCreditCardClient.getCreditCardProducts(userKey);
        if (products.rec() == null || products.rec().isEmpty()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND, "발급 가능한 카드 상품이 없습니다.");
        }

        // 사용자의 수시입출금 계좌 조회 (없으면 자동 생성)
        SsafyAccountListResponse accountList = ssafyFinanceClient.getAccountList(userKey);
        String withdrawalAccountNo;
        if (accountList.rec() == null || accountList.rec().isEmpty()) {
            // 클라이언트는 계좌번호만 발급하고 영속화는 AccountService가 한다(SsafyFinanceClient.createAccount 주석).
            // 클라이언트를 직접 부르면 accounts 행이 없어 이 카드의 결제 확정이 ACCOUNT_NOT_FOUND로 실패한다
            AccountResponse account = accountService.createAccount(user.getId(),
                    new AccountCreateRequest(business ? AccountType.BUSINESS : AccountType.PERSONAL,
                            DEFAULT_ACCOUNT_TYPE, null));
            withdrawalAccountNo = account.accountNumber();
        } else {
            withdrawalAccountNo = accountList.rec().get(0).accountNo();
        }

        // 첫 번째 카드 상품으로 카드 발급
        SsafyCreditCardProductListResponse.CreditCardProductRec product = products.rec().get(0);
        SsafyCreateCreditCardResponse.CreditCardRec rec = ssafyCreditCardClient.createCreditCard(
                userKey, product.cardUniqueNo(), withdrawalAccountNo, "4").rec();
        String cardNo = rec.cardNo();

        Card card = Card.builder()
                .user(user)
                .cardName(product.cardName() != null ? product.cardName() : (business ? "사업자카드" : "개인카드"))
                .cardType(cardType)
                .last4Digits(cardNo.substring(cardNo.length() - 4))
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
        log.info("[TestUserProvisioner] 유저 {} {} 카드 발급: 계좌 {}", user.getId(), cardType, withdrawalAccountNo);
        return true;
    }
}
