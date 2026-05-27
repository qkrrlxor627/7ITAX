package com.ssafy.tax7i.card.repository;

import com.ssafy.tax7i.card.entity.CardTransaction;
import com.ssafy.tax7i.card.entity.CardTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTransactionRepository extends JpaRepository<CardTransaction, Long> {

    Page<CardTransaction> findByCard_IdOrderByCreatedAtDesc(Long cardId, Pageable pageable);

    Page<CardTransaction> findByCard_IdAndTransactionTypeOrderByCreatedAtDesc(
            Long cardId, CardTransactionType type, Pageable pageable);
}
