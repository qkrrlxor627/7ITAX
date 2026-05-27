package com.ssafy.tax7i.bookentry.service;

import com.ssafy.tax7i.bookentry.dto.BookEntryCategoryUpdateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryCreateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryResponse;
import com.ssafy.tax7i.bookentry.dto.BookEntrySummaryResponse;
import com.ssafy.tax7i.bookentry.dto.IncomeCreateRequest;
import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.bookentry.repository.BookEntrySpecification;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookEntryService {

    private final BookEntryRepository bookEntryRepository;
    private final TaxParameterService taxParameterService;

    @CacheEvict(value = "entertainmentUsed", key = "#userId")
    @Transactional
    public BookEntryResponse create(Long userId, BookEntryCreateRequest request) {
        // 중복 장부 방어: 같은 paymentId로 이미 생성된 경우
        if (request.paymentId() != null) {
            bookEntryRepository.findByPaymentId(request.paymentId()).ifPresent(existing -> {
                throw new BusinessException(ErrorCode.DUPLICATE_BOOK_ENTRY,
                        "이미 장부가 생성된 결제입니다. paymentId=" + request.paymentId());
            });
        }

        long amount = request.amount();
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "금액은 0보다 커야 합니다.");
        }

        long vatAmount;
        long supplyPrice;
        if (request.isVatExemptOrDefault()) {
            vatAmount = 0L;
            supplyPrice = amount;
        } else {
            double vatRate = taxParameterService.getVatRate(request.entryDate().getYear());
            vatAmount = Math.round(amount * vatRate / (1 + vatRate));
            supplyPrice = amount - vatAmount;
        }

        long incomeAmount = 0L;
        long expenseAmount = 0L;
        long fixedAssetAmount = 0L;

        switch (request.entryType()) {
            case INCOME -> incomeAmount = amount;
            case EXPENSE -> expenseAmount = amount;
            case ASSET -> fixedAssetAmount = amount;
        }

        BookEntry entry = BookEntry.builder()
                .userId(userId)
                .paymentId(request.paymentId())
                .entryDate(request.entryDate())
                .description(request.description())
                .merchantName(request.merchantName())
                .entryType(request.entryType())
                .incomeAmount(incomeAmount)
                .expenseAmount(expenseAmount)
                .fixedAssetAmount(fixedAssetAmount)
                .vatAmount(vatAmount)
                .supplyPrice(supplyPrice)
                .categoryCode(request.categoryCode())
                .categoryName(request.categoryName())
                .isVatDeductible(!request.isVatExemptOrDefault())
                .confidenceScore(request.confidenceScore())
                .note(request.note())
                .build();

        BookEntry saved = bookEntryRepository.save(entry);
        log.info("장부 생성: id={}, userId={}, paymentId={}, type={}, amount={}, supply={}, vat={}, exempt={}",
                saved.getId(), userId, request.paymentId(), request.entryType(),
                amount, supplyPrice, vatAmount, request.isVatExemptOrDefault());
        return BookEntryResponse.from(saved);
    }

    public BookEntryResponse getEntry(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        return BookEntryResponse.from(entry);
    }

    public Page<BookEntryResponse> getEntries(Long userId, Boolean confirmed, Pageable pageable) {
        return getEntries(userId, confirmed, null, null, null, null, null, null, null, pageable);
    }

    /**
     * 간편장부 목록 조회 — 동적 필터 지원
     * 모든 필터는 optional, null이면 무시됨 (AND 조합)
     */
    public Page<BookEntryResponse> getEntries(Long userId, Boolean confirmed,
                                               EntryType entryType,
                                               LocalDate startDate, LocalDate endDate,
                                               String categoryCode, String keyword,
                                               Boolean hasEvidence, Boolean unclassified,
                                               Pageable pageable) {
        Specification<BookEntry> spec = Specification.where(BookEntrySpecification.withUserId(userId))
                .and(BookEntrySpecification.withConfirmed(confirmed))
                .and(BookEntrySpecification.withEntryType(entryType))
                .and(BookEntrySpecification.withStartDate(startDate))
                .and(BookEntrySpecification.withEndDate(endDate))
                .and(BookEntrySpecification.withCategoryCode(categoryCode))
                .and(BookEntrySpecification.withKeyword(keyword))
                .and(BookEntrySpecification.withHasEvidence(hasEvidence))
                .and(BookEntrySpecification.withUnclassified(unclassified));

        return bookEntryRepository.findAll(spec, pageable).map(BookEntryResponse::from);
    }

    public long getUnconfirmedCount(Long userId) {
        return bookEntryRepository.countByUserIdAndConfirmed(userId, false);
    }

    @Transactional
    public BookEntryResponse confirm(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        entry.confirm();
        log.info("장부 확인: id={}, userId={}, merchant={}", entryId, userId, entry.getMerchantName());
        return BookEntryResponse.from(entry);
    }

    @Transactional
    public BookEntryResponse updateCategory(Long userId, Long entryId,
                                            BookEntryCategoryUpdateRequest request) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        String beforeCode = entry.getCategoryCode();
        entry.updateCategory(request.categoryCode(), request.categoryName());
        log.info("세목 변경: id={}, userId={}, {} → {}", entryId, userId, beforeCode, request.categoryCode());
        return BookEntryResponse.from(entry);
    }

    @Transactional
    public BookEntryResponse markAsPersonal(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        entry.markAsPersonal();
        log.info("개인지출 전환: id={}, userId={}, merchant={}, amount={}",
                entryId, userId, entry.getMerchantName(), entry.getExpenseAmount());
        return BookEntryResponse.from(entry);
    }

    @Transactional
    public BookEntryResponse markAsBusiness(Long userId, Long entryId) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        entry.markAsBusiness();
        log.info("사업경비 복원: id={}, userId={}", entryId, userId);
        return BookEntryResponse.from(entry);
    }

    @Transactional
    public BookEntryResponse updateNote(Long userId, Long entryId, String note) {
        BookEntry entry = findByIdAndUserId(entryId, userId);
        entry.updateNote(note);
        log.info("메모 수정: id={}, userId={}", entryId, userId);
        return BookEntryResponse.from(entry);
    }

    @CacheEvict(value = "entertainmentUsed", key = "#userId")
    @Transactional
    public BookEntryResponse createIncome(Long userId, IncomeCreateRequest request) {
        long amount = request.amount();
        long withholdingTax = 0L;

        if (request.withholdingRate() != null && request.withholdingRate() > 0) {
            withholdingTax = Math.round(amount * request.withholdingRate() / 100.0);
        }

        long supplyPrice = amount;
        long vatAmount = 0L;

        BookEntry entry = BookEntry.builder()
                .userId(userId)
                .entryDate(request.transactionDate())
                .description(request.description())
                .entryType(EntryType.INCOME)
                .incomeAmount(supplyPrice)
                .expenseAmount(0L)
                .fixedAssetAmount(0L)
                .vatAmount(vatAmount)
                .supplyPrice(supplyPrice)
                .categoryCode(com.ssafy.tax7i.classification.entity.TaxCategory.SALES.getCode())
                .categoryName(com.ssafy.tax7i.classification.entity.TaxCategory.SALES.getName())
                .isVatDeductible(false)
                .confidenceScore(100)
                .note(request.note())
                .build();

        BookEntry saved = bookEntryRepository.save(entry);
        log.info("수입 등록: id={}, userId={}, amount={}, withholdingTax={}",
                saved.getId(), userId, amount, withholdingTax);
        return BookEntryResponse.from(saved);
    }

    public BookEntrySummaryResponse getSummary(Long userId, int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long totalIncome = agg.totalIncome();
        long totalExpense = agg.totalExpense();

        List<Object[]> monthlyData = bookEntryRepository.sumByMonthAndYear(userId, year);
        List<BookEntrySummaryResponse.MonthSummary> byMonth = monthlyData.stream()
                .map(row -> new BookEntrySummaryResponse.MonthSummary(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue()
                ))
                .toList();

        List<Object[]> categoryData = bookEntryRepository.sumExpenseByCategoryAndYear(userId, year);
        long categoryTotal = categoryData.stream()
                .mapToLong(row -> ((Number) row[2]).longValue())
                .sum();
        List<BookEntrySummaryResponse.CategorySummary> byCategory = categoryData.stream()
                .map(row -> {
                    long amount = ((Number) row[2]).longValue();
                    double percentage = categoryTotal == 0 ? 0.0
                            : Math.round(amount * 1000.0 / categoryTotal) / 10.0;
                    return new BookEntrySummaryResponse.CategorySummary(
                            (String) row[0],
                            (String) row[1],
                            amount,
                            ((Number) row[3]).longValue(),
                            percentage
                    );
                })
                .toList();

        // 거래처별 수입 집계
        List<Object[]> merchantData = bookEntryRepository.sumIncomeByMerchant(userId, start, end);
        long merchantTotal = merchantData.stream()
                .mapToLong(row -> ((Number) row[1]).longValue())
                .sum();
        List<BookEntrySummaryResponse.MerchantSummary> byMerchant = merchantData.stream()
                .map(row -> {
                    long amount = ((Number) row[1]).longValue();
                    double pct = merchantTotal == 0 ? 0.0
                            : Math.round(amount * 1000.0 / merchantTotal) / 10.0;
                    return new BookEntrySummaryResponse.MerchantSummary(
                            (String) row[0],
                            amount,
                            ((Number) row[2]).longValue(),
                            pct
                    );
                })
                .toList();

        // 전년 대비 비교
        BookEntrySummaryResponse.YearOverYearComparison comparison = buildComparison(
                userId, year - 1, totalIncome, totalExpense);

        return new BookEntrySummaryResponse(year, totalIncome, totalExpense, byMonth, byCategory, byMerchant, comparison);
    }

    private BookEntrySummaryResponse.YearOverYearComparison buildComparison(
            Long userId, int prevYear, long currentIncome, long currentExpense) {
        LocalDate prevStart = LocalDate.of(prevYear, 1, 1);
        LocalDate prevEnd = LocalDate.of(prevYear, 12, 31);

        AggregateResult prev = bookEntryRepository.safeAggregate(userId, prevStart, prevEnd);
        long prevIncome = prev.totalIncome();
        long prevExpense = prev.totalExpense();

        // 전년 데이터가 아예 없으면 비교 불가
        if (prevIncome == 0 && prevExpense == 0) {
            return null;
        }

        long prevProfit = prevIncome - prevExpense;
        long currentProfit = currentIncome - currentExpense;

        return new BookEntrySummaryResponse.YearOverYearComparison(
                prevIncome, prevExpense, prevProfit,
                changeRate(currentIncome, prevIncome),
                changeRate(currentExpense, prevExpense),
                changeRate(currentProfit, prevProfit)
        );
    }

    private Double changeRate(long current, long previous) {
        if (previous == 0) return null;
        return Math.round((current - previous) * 1000.0 / previous) / 10.0;
    }

    private BookEntry findByIdAndUserId(Long entryId, Long userId) {
        return bookEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> {
                    log.warn("장부 조회 실패: entryId={}, userId={}", entryId, userId);
                    return new BusinessException(ErrorCode.BOOK_ENTRY_NOT_FOUND);
                });
    }
}
