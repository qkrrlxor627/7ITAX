package com.ssafy.tax7i.banking.repository;

import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByIdAndUser_IdAndDeletedFalse(Long id, Long userId);
    List<Account> findByUser_IdAndDeletedFalse(Long userId);
    List<Account> findByUser_IdAndAccountTypeAndDeletedFalse(Long userId, AccountType accountType);
}
