package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.bookentry.repository.AggregateResult;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.VatReturnCreateRequest;
import com.ssafy.tax7i.tax.dto.VatReturnResponse;
import com.ssafy.tax7i.tax.entity.TaxReturnStatus;
import com.ssafy.tax7i.tax.entity.VatReturn;
import com.ssafy.tax7i.tax.repository.VatReturnRepository;
import com.ssafy.tax7i.config.TaxOfficeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VatReturnService {

    private final VatReturnRepository vatReturnRepository;
    private final BookEntryRepository bookEntryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final TaxOfficeProperties taxOfficeProperties;

    private static final String VAT_RECEIPT_SEQ_KEY_PREFIX = "vat-receipt:seq:";

    @Transactional
    public VatReturnResponse createDraft(Long userId, VatReturnCreateRequest request) {
        int taxYear = request.taxYear();
        int taxPeriod = request.taxPeriod();

        if (taxPeriod != 1 && taxPeriod != 2) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT,
                    "과세기간은 1(1기) 또는 2(2기)만 가능합니다.");
        }

        // Check if VatReturn already exists for this user+year+period (non-DRAFT)
        Optional<VatReturn> existing = vatReturnRepository
                .findByUserIdAndTaxYearAndTaxPeriod(userId, taxYear, taxPeriod);
        if (existing.isPresent() && existing.get().getStatus() != TaxReturnStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "해당 기간에 이미 제출된 부가가치세 신고서가 존재합니다.");
        }

        // Delete existing DRAFT if re-creating
        if (existing.isPresent()) {
            vatReturnRepository.delete(existing.get());
            vatReturnRepository.flush();
        }

        // Determine date range based on taxPeriod
        LocalDate start;
        LocalDate end;
        if (taxPeriod == 1) {
            start = LocalDate.of(taxYear, 1, 1);
            end = LocalDate.of(taxYear, 6, 30);
        } else {
            start = LocalDate.of(taxYear, 7, 1);
            end = LocalDate.of(taxYear, 12, 31);
        }

        // Aggregate BookEntry data for the period
        AggregateResult agg = bookEntryRepository.safeAggregate(userId, start, end);
        long totalIncome = agg.totalIncome();
        long totalExpense = agg.totalExpense();

        // Calculate VAT amounts
        Long salesVat = bookEntryRepository.sumSalesVat(userId, start, end);
        Long purchaseVat = bookEntryRepository.sumDeductiblePurchaseVat(userId, start, end);

        long salesTax = salesVat != null ? salesVat : 0L;
        long purchaseTax = purchaseVat != null ? purchaseVat : 0L;

        // salesAmount = total income (supply value)
        long salesAmount = totalIncome;
        // purchaseAmount = total expense (supply value)
        long purchaseAmount = totalExpense;

        // preliminaryPaid defaults to 0
        long preliminaryPaid = request.preliminaryPaid() != null ? request.preliminaryPaid() : 0L;

        // finalTax = salesTax - purchaseTax - preliminaryPaid
        long finalTax = salesTax - purchaseTax - preliminaryPaid;

        VatReturn vatReturn = VatReturn.builder()
                .userId(userId)
                .taxYear(taxYear)
                .taxPeriod(taxPeriod)
                .status(TaxReturnStatus.DRAFT)
                .salesAmount(salesAmount)
                .salesTax(salesTax)
                .purchaseAmount(purchaseAmount)
                .purchaseTax(purchaseTax)
                .preliminaryPaid(preliminaryPaid)
                .finalTax(finalTax)
                .build();

        vatReturn = vatReturnRepository.save(vatReturn);
        log.info("VAT return draft created: userId={}, taxYear={}, taxPeriod={}, finalTax={}",
                userId, taxYear, taxPeriod, finalTax);

        return VatReturnResponse.from(vatReturn);
    }

    public VatReturnResponse getReturn(Long userId, Long returnId) {
        VatReturn vatReturn = vatReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "부가가치세 신고서를 찾을 수 없습니다."));
        return VatReturnResponse.from(vatReturn);
    }

    public List<VatReturnResponse> getReturns(Long userId) {
        List<VatReturn> returns = vatReturnRepository
                .findByUserIdOrderByTaxYearDescTaxPeriodDesc(userId);
        return returns.stream()
                .map(VatReturnResponse::from)
                .toList();
    }

    @Transactional
    public VatReturnResponse submit(Long userId, Long returnId) {
        VatReturn vatReturn = vatReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "부가가치세 신고서를 찾을 수 없습니다."));

        // Transition DRAFT -> SUBMITTED -> ACCEPTED
        vatReturn.transitionTo(TaxReturnStatus.SUBMITTED);
        vatReturn.transitionTo(TaxReturnStatus.ACCEPTED);

        // Generate receipt number using Redis: V{year}-0305-{seq}
        String receiptNumber = generateReceiptNumber(vatReturn.getTaxYear());
        vatReturn.assignReceiptNumber(receiptNumber);

        log.info("VAT return submitted: id={}, receiptNumber={}", returnId, receiptNumber);

        return VatReturnResponse.from(vatReturn);
    }

    private String generateReceiptNumber(int taxYear) {
        String officeCode = taxOfficeProperties.getOfficeCode();
        String key = VAT_RECEIPT_SEQ_KEY_PREFIX + taxYear + ":" + officeCode;
        Long seq = redisTemplate.opsForValue().increment(key);
        return String.format("V%d-%s-%06d", taxYear, officeCode, seq);
    }
}
