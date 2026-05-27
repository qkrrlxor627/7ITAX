package com.ssafy.tax7i.fcm.repository;

import com.ssafy.tax7i.fcm.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    List<FcmToken> findByUser_Id(Long userId);

    Optional<FcmToken> findByToken(String token);

    @Modifying
    @Transactional
    void deleteByToken(String token);

    @Query("SELECT DISTINCT ft.user.id FROM FcmToken ft")
    List<Long> findDistinctUserIds();
}
