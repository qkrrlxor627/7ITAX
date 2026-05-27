package com.ssafy.tax7i.taxcalendar.repository;

import com.ssafy.tax7i.taxcalendar.entity.TaxDeadline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaxDeadlineRepository extends JpaRepository<TaxDeadline, Long> {

    List<TaxDeadline> findByYear(int year);

    Optional<TaxDeadline> findByYearAndName(int year, String name);

    long countByYear(int year);
}
