package com.ssafy.tax7i.transfer.repository;

import com.ssafy.tax7i.transfer.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    @Query("SELECT t FROM Transfer t WHERE t.senderUser.id = :userId OR t.receiverUser.id = :userId ORDER BY t.createdAt DESC")
    Page<Transfer> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Transfer t WHERE t.id = :id AND (t.senderUser.id = :userId OR t.receiverUser.id = :userId)")
    Optional<Transfer> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
