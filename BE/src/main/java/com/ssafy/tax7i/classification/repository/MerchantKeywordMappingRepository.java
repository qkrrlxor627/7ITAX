package com.ssafy.tax7i.classification.repository;

import com.ssafy.tax7i.classification.entity.MerchantKeywordMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantKeywordMappingRepository extends JpaRepository<MerchantKeywordMapping, Long> {

    List<MerchantKeywordMapping> findByMcc(String mcc);
}
