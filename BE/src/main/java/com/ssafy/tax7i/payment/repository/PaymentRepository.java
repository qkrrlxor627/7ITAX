package com.ssafy.tax7i.payment.repository;

import com.ssafy.tax7i.payment.entity.Payment;
import com.ssafy.tax7i.payment.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.user JOIN FETCH p.card WHERE p.id = :id AND p.user.id = :userId")
    Optional<Payment> findByIdAndUserIdWithFetch(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.user JOIN FETCH p.card WHERE p.id = :id")
    Optional<Payment> findByIdWithFetch(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p JOIN FETCH p.user JOIN FETCH p.card WHERE p.id = :id")
    Optional<Payment> findByIdWithFetchForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Payment p JOIN FETCH p.card WHERE p.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findByUserIdWithFetch(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN FETCH p.card " +
            "WHERE p.user.id = :userId AND p.capturedAt BETWEEN :start AND :end " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findByUserIdAndCapturedAtBetweenWithFetch(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    Page<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN FETCH p.card " +
            "WHERE p.user.id = :userId AND p.capturedAt BETWEEN :start AND :end AND p.status = :status " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findByUserIdAndCapturedAtBetweenAndStatusWithFetch(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") PaymentStatus status,
            Pageable pageable);
}
