package com.ssafy.tax7i.tax.repository;

import com.ssafy.tax7i.tax.entity.TaxReturn;
import com.ssafy.tax7i.tax.entity.TaxReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxReturnRepository extends JpaRepository<TaxReturn, Long> {

    Optional<TaxReturn> findByIdAndUserId(Long id, Long userId);

    Optional<TaxReturn> findByUserIdAndTaxYear(Long userId, int taxYear);

    boolean existsByUserIdAndTaxYearAndStatusNot(Long userId, int taxYear, TaxReturnStatus status);

    List<TaxReturn> findByUserIdOrderByTaxYearDesc(Long userId);
}
