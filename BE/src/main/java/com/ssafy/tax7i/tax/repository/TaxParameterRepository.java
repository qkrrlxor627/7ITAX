package com.ssafy.tax7i.tax.repository;

import com.ssafy.tax7i.tax.entity.TaxParameter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxParameterRepository extends JpaRepository<TaxParameter, Long> {

    List<TaxParameter> findByYearAndCategory(int year, String category);

    long countByYear(int year);
}
