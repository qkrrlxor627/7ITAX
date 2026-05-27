package com.ssafy.tax7i.classification.repository;

import com.ssafy.tax7i.classification.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findFirstByMerchantName(String merchantName);

    @Query("SELECT m FROM Merchant m WHERE m.merchantName LIKE CONCAT('%', :name, '%') ESCAPE '\\'")
    List<Merchant> findByMerchantNameContainedIn(String name);

    List<Merchant> findByMcc(String mcc);
}
