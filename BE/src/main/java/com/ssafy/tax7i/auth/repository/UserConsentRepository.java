package com.ssafy.tax7i.auth.repository;

import com.ssafy.tax7i.auth.domain.ConsentType;
import com.ssafy.tax7i.auth.domain.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    List<UserConsent> findByUserId(Long userId);

    Optional<UserConsent> findByUserIdAndConsentType(Long userId, ConsentType consentType);
}
