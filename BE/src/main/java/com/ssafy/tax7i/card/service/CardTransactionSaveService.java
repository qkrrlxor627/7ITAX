package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardTransaction;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import com.ssafy.tax7i.card.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionSaveService {

    private final CardTransactionRepository cardTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Card card, CardTransactionType type, Long amount, String description, Long paymentBalance) {
        CardTransaction tx = CardTransaction.builder()
                .card(card)
                .transactionType(type)
                .amount(amount)
                .balanceAfter(paymentBalance != null ? paymentBalance : 0L)
                .description(description)
                .build();
        cardTransactionRepository.save(tx);
        log.info("카드 거래 기록 저장: cardId={}, type={}, amount={}", card.getId(), type, amount);
    }
}
