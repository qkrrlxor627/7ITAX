package com.ssafy.tax7i.tax.repository;

import com.ssafy.tax7i.tax.entity.TaxBracket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaxBracketRepository extends JpaRepository<TaxBracket, Long> {

    List<TaxBracket> findByYearOrderByBracketMinAsc(int year);
}
