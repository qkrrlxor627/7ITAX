package com.ssafy.tax7i.classification.repository;

import com.ssafy.tax7i.classification.entity.TaxLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxLimitRepository extends JpaRepository<TaxLimit, Long> {

    List<TaxLimit> findByTaxCategory(String taxCategory);

    Optional<TaxLimit> findByTaxCategoryAndLimitType(String taxCategory, String limitType);
}
