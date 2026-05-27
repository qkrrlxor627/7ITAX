package com.ssafy.tax7i.banking.repository;

import com.ssafy.tax7i.banking.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByAccountNoKeyOrderByTransactedAtDesc(String accountNoKey);

    List<BankTransaction> findByAccountNoKeyAndTransactedAtBetweenOrderByTransactedAtDesc(
            String accountNoKey, LocalDateTime start, LocalDateTime end);
}
