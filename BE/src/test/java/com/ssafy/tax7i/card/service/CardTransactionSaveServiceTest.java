package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardTransaction;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.event.CardTransactionEvent;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.card.repository.CardTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardTransactionSaveServiceTest {

    @Mock private CardTransactionRepository cardTransactionRepository;
    @Mock private CardRepository cardRepository;
    @InjectMocks private CardTransactionSaveService service;

    @Test
    void 이벤트로_카드거래기록_저장() {
        Card card = mock(Card.class);
        given(cardRepository.getReferenceById(7L)).willReturn(card);

        service.save(new CardTransactionEvent(7L, CardTransactionType.PAYMENT, 12000L, "결제: 스타벅스", null));

        ArgumentCaptor<CardTransaction> captor = ArgumentCaptor.forClass(CardTransaction.class);
        verify(cardTransactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(12000L);
        assertThat(captor.getValue().getBalanceAfter()).isZero();
    }

    @Test
    void 저장실패해도_예외를_던지지_않는다() {
        given(cardRepository.getReferenceById(7L)).willReturn(mock(Card.class));
        given(cardTransactionRepository.save(any())).willThrow(new RuntimeException("DB 오류"));

        service.save(new CardTransactionEvent(7L, CardTransactionType.REFUND, 1000L, "환불", 0L));
    }
}
