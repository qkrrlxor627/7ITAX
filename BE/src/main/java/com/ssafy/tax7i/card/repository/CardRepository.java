package com.ssafy.tax7i.card.repository;

import com.ssafy.tax7i.card.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByIdAndUser_IdAndDeletedFalse(Long id, Long userId);

    List<Card> findByUser_IdAndDeletedFalse(Long userId);

    Optional<Card> findByUser_IdAndIsDefaultTrueAndDeletedFalse(Long userId);
}
