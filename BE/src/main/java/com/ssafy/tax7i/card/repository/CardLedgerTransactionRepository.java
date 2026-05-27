package com.ssafy.tax7i.card.repository;

import com.ssafy.tax7i.card.entity.CardLedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CardLedgerTransactionRepository extends JpaRepository<CardLedgerTransaction, Long> {

    List<CardLedgerTransaction> findByCardNoKeyOrderByTransactedAtDesc(String cardNoKey);

    List<CardLedgerTransaction> findByCardNoKeyAndTransactedAtBetweenOrderByTransactedAtDesc(
            String cardNoKey, LocalDateTime start, LocalDateTime end);

    Optional<CardLedgerTransaction> findByTransactionUniqueNo(String transactionUniqueNo);
}
