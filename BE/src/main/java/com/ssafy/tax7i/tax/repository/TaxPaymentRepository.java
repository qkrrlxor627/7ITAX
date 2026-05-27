package com.ssafy.tax7i.tax.repository;

import com.ssafy.tax7i.tax.entity.TaxPayment;
import com.ssafy.tax7i.tax.entity.TaxPaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxPaymentRepository extends JpaRepository<TaxPayment, Long> {

    Optional<TaxPayment> findByTaxReturn_IdAndPaymentType(Long taxReturnId, TaxPaymentType type);

    List<TaxPayment> findByTaxReturn_Id(Long taxReturnId);
}
