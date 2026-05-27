package com.ssafy.tax7i.tax.repository;

import com.ssafy.tax7i.tax.entity.ExpenseDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseDetailRepository extends JpaRepository<ExpenseDetail, Long> {

    List<ExpenseDetail> findByTaxReturn_Id(Long taxReturnId);

    List<ExpenseDetail> findByTaxReturn_IdIn(List<Long> taxReturnIds);

    void deleteByTaxReturn_Id(Long taxReturnId);
}
