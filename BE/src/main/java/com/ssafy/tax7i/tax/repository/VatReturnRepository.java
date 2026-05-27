package com.ssafy.tax7i.tax.repository;

import com.ssafy.tax7i.tax.entity.VatReturn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VatReturnRepository extends JpaRepository<VatReturn, Long> {

    Optional<VatReturn> findByIdAndUserId(Long id, Long userId);

    Optional<VatReturn> findByUserIdAndTaxYearAndTaxPeriod(Long userId, int taxYear, int taxPeriod);

    List<VatReturn> findByUserIdOrderByTaxYearDescTaxPeriodDesc(Long userId);
}
