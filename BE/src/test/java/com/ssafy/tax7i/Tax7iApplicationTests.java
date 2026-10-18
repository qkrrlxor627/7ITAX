package com.ssafy.tax7i;

import com.ssafy.tax7i.banking.repository.AccountRepository;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class Tax7iApplicationTests {

	@Autowired
	private CardRepository cardRepository;

	@Autowired
	private AccountRepository accountRepository;

	@BeforeAll
	static void setup() {
		// JPA/Hibernate가 Spring보다 먼저 AesEncryptor를 초기화할 수 있으므로
		// System property로 AES 키를 미리 설정
		System.setProperty("encryption.aes-key", "dGVzdC1hZXMtZW5jcnlwdGlvbi1rZXktMzItYnl0ZXM=");
	}

	@Test
	void contextLoads() {
	}

	@Test
	void 시드_카드의_출금계좌가_accounts에_실제로_저장된다() {
		// CardDataInitializer가 기동 시 만든 카드로 결제를 확정하면 출금계좌를 accounts에서 찾는다.
		// 계좌가 저장되지 않으면 결제 확정이 ACCOUNT_NOT_FOUND로 실패한다
		List<Card> cards = cardRepository.findAll();

		assertThat(cards).isNotEmpty();
		assertThat(cards).allSatisfy(card ->
				assertThat(accountRepository.findByAccountNoKey(card.getWithdrawalAccountNo())).isPresent());
	}

	@Test
	void 시드에_장부_자동생성_대상인_사업자_카드가_있다() {
		// 장부 자동 생성은 카드 유형이 BUSINESS일 때만 동작한다(BookEntryEventListener)
		assertThat(cardRepository.findAll()).anyMatch(card -> card.getCardType() == CardType.BUSINESS);
	}

}
