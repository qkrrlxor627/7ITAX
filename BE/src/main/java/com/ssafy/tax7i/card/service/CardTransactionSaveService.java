package com.ssafy.tax7i.card.service;

import com.ssafy.tax7i.card.entity.CardTransaction;
import com.ssafy.tax7i.card.event.CardTransactionEvent;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.card.repository.CardTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 카드 거래 기록 저장. 결제 트랜잭션 커밋 후 별도 스레드에서 저장한다.
 * <p>예전에는 결제 트랜잭션 안에서 REQUIRES_NEW로 저장해 커넥션 하나를 쥔 채 두 번째 커넥션을 요구했다.
 * 동시 결제가 DB 풀 크기만큼 몰리면 전원이 두 번째 커넥션을 기다리며 30초(Hikari 대기 한도) 멈췄고,
 * 결제가 롤백돼도 기록은 커밋됐다(docs/PERF_TEST.md 7절)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardTransactionSaveService {

    private final CardTransactionRepository cardTransactionRepository;
    private final CardRepository cardRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(CardTransactionEvent event) {
        try {
            CardTransaction tx = CardTransaction.builder()
                    .card(cardRepository.getReferenceById(event.cardId()))
                    .transactionType(event.type())
                    .amount(event.amount())
                    .balanceAfter(event.paymentBalance() != null ? event.paymentBalance() : 0L)
                    .description(event.description())
                    .build();
            cardTransactionRepository.save(tx);
            log.info("카드 거래 기록 저장: cardId={}, type={}, amount={}", event.cardId(), event.type(), event.amount());
        } catch (Exception e) {
            log.warn("카드 거래 기록 저장 실패 (결제는 정상): cardId={}, error={}", event.cardId(), e.getMessage());
        }
    }
}
