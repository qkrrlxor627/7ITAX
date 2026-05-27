package com.ssafy.tax7i.banking.repository;

import com.ssafy.tax7i.banking.entity.Account;
import com.ssafy.tax7i.banking.entity.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByIdAndUser_IdAndDeletedFalse(Long id, Long userId);
    List<Account> findByUser_IdAndDeletedFalse(Long userId);
    List<Account> findByUser_IdAndAccountTypeAndDeletedFalse(Long userId, AccountType accountType);

    Optional<Account> findByAccountNoKey(String accountNoKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNoKey = :key")
    Optional<Account> findByAccountNoKeyForUpdate(@Param("key") String key);

    List<Account> findByUser_SsafyUserKeyAndDeletedFalse(String ssafyUserKey);
}
