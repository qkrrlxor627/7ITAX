package com.ssafy.tax7i.bookentry.repository;

import com.ssafy.tax7i.bookentry.entity.BookEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookEntryRepository extends JpaRepository<BookEntry, Long>, JpaSpecificationExecutor<BookEntry> {

    Optional<BookEntry> findByIdAndUserId(Long id, Long userId);

    Optional<BookEntry> findByPaymentId(Long paymentId);

    Page<BookEntry> findByUserIdAndConfirmed(Long userId, Boolean confirmed, Pageable pageable);

    Page<BookEntry> findByUserId(Long userId, Pageable pageable);

    Page<BookEntry> findByUserIdAndEntryDateBetween(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    long countByUserIdAndConfirmed(Long userId, Boolean confirmed);

    @Query("SELECT COALESCE(SUM(b.vatAmount), 0) FROM BookEntry b " +
            "WHERE b.userId = :userId AND b.entryType = 'INCOME' " +
            "AND b.confirmed = true AND b.entryDate BETWEEN :start AND :end")
    Long sumSalesVat(@Param("userId") Long userId,
                     @Param("start") LocalDate start,
                     @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(b.vatAmount), 0) FROM BookEntry b " +
            "WHERE b.userId = :userId AND b.entryType = 'EXPENSE' " +
            "AND b.confirmed = true AND b.isBusinessExpense = true AND b.isVatDeductible = true " +
            "AND b.entryDate BETWEEN :start AND :end")
    Long sumDeductiblePurchaseVat(@Param("userId") Long userId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(b.expenseAmount), 0) FROM BookEntry b " +
            "WHERE b.userId = :userId AND b.categoryName = :categoryName " +
            "AND EXTRACT(YEAR FROM b.entryDate) = :year")
    Long sumAmountByUserIdAndCategoryNameAndYear(@Param("userId") Long userId,
                                                  @Param("categoryName") String categoryName,
                                                  @Param("year") int year);

    @Query("SELECT " +
            "COALESCE(SUM(CASE WHEN b.entryType = 'INCOME' THEN b.incomeAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN b.entryType = 'EXPENSE' THEN b.expenseAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN b.entryType = 'ASSET' THEN b.fixedAssetAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN b.entryType = 'EXPENSE' AND b.isBusinessExpense = true THEN b.expenseAmount ELSE 0 END), 0) " +
            "FROM BookEntry b WHERE b.userId = :userId AND b.confirmed = true " +
            "AND b.entryDate BETWEEN :start AND :end")
    Object[] aggregateByUserIdAndDateRange(@Param("userId") Long userId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end);

    /** 안전하게 unwrap된 집계 결과 반환 */
    default AggregateResult safeAggregate(Long userId, LocalDate start, LocalDate end) {
        return AggregateResult.from(aggregateByUserIdAndDateRange(userId, start, end));
    }

    @Query("SELECT b.categoryCode, b.categoryName, SUM(b.expenseAmount), COUNT(b) " +
            "FROM BookEntry b WHERE b.userId = :userId AND b.confirmed = true " +
            "AND b.entryType = 'EXPENSE' AND b.isBusinessExpense = true " +
            "AND EXTRACT(YEAR FROM b.entryDate) = :year " +
            "GROUP BY b.categoryCode, b.categoryName ORDER BY SUM(b.expenseAmount) DESC")
    List<Object[]> sumExpenseByCategoryAndYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT b.categoryCode AS categoryCode, b.categoryName AS categoryName, " +
            "b.isVatDeductible AS isVatDeductible, COUNT(b) AS count " +
            "FROM BookEntry b " +
            "WHERE b.userId = :userId AND b.merchantName = :merchantName " +
            "AND b.confirmed = true AND b.categoryCode IS NOT NULL " +
            "GROUP BY b.categoryCode, b.categoryName, b.isVatDeductible " +
            "HAVING COUNT(b) >= :threshold " +
            "ORDER BY COUNT(b) DESC")
    List<LearnedCategoryProjection> findLearnedCategory(@Param("userId") Long userId,
                                                         @Param("merchantName") String merchantName,
                                                         @Param("threshold") int threshold);

    @Query("SELECT EXTRACT(MONTH FROM b.entryDate), " +
            "COALESCE(SUM(CASE WHEN b.entryType = 'INCOME' THEN b.incomeAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN b.entryType = 'EXPENSE' THEN b.expenseAmount ELSE 0 END), 0), " +
            "COALESCE(SUM(CASE WHEN b.entryType = 'ASSET' THEN b.fixedAssetAmount ELSE 0 END), 0) " +
            "FROM BookEntry b WHERE b.userId = :userId AND b.confirmed = true " +
            "AND EXTRACT(YEAR FROM b.entryDate) = :year GROUP BY EXTRACT(MONTH FROM b.entryDate) ORDER BY EXTRACT(MONTH FROM b.entryDate)")
    List<Object[]> sumByMonthAndYear(@Param("userId") Long userId, @Param("year") int year);

    @Query("SELECT b.merchantName, SUM(b.incomeAmount), COUNT(b) " +
            "FROM BookEntry b WHERE b.userId = :userId AND b.confirmed = true " +
            "AND b.entryType = 'INCOME' AND b.merchantName IS NOT NULL " +
            "AND b.entryDate BETWEEN :start AND :end " +
            "GROUP BY b.merchantName ORDER BY SUM(b.incomeAmount) DESC")
    List<Object[]> sumIncomeByMerchant(@Param("userId") Long userId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);
}
