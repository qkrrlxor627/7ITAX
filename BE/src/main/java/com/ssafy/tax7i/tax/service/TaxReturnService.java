package com.ssafy.tax7i.tax.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.BusinessProfileRepository;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.TaxCalculationResult;
import com.ssafy.tax7i.tax.dto.TaxPaymentStatusResponse;
import com.ssafy.tax7i.tax.dto.TaxReturnCreateRequest;
import com.ssafy.tax7i.tax.dto.TaxReturnResponse;
import com.ssafy.tax7i.tax.dto.TaxReturnSubmitResponse;
import com.ssafy.tax7i.tax.dto.TaxReturnUpdateRequest;
import com.ssafy.tax7i.tax.entity.ExpenseDetail;
import com.ssafy.tax7i.tax.entity.TaxPayment;
import com.ssafy.tax7i.tax.entity.TaxPaymentStatus;
import com.ssafy.tax7i.tax.entity.TaxPaymentType;
import com.ssafy.tax7i.tax.entity.TaxReturn;
import com.ssafy.tax7i.tax.entity.TaxReturnStatus;
import com.ssafy.tax7i.tax.repository.ExpenseDetailRepository;
import com.ssafy.tax7i.tax.repository.TaxPaymentRepository;
import com.ssafy.tax7i.tax.repository.TaxReturnRepository;
import com.ssafy.tax7i.config.TaxOfficeProperties;
import com.ssafy.tax7i.taxcalendar.repository.TaxDeadlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxReturnService {

    private final TaxReturnRepository taxReturnRepository;
    private final TaxPaymentRepository taxPaymentRepository;
    private final ExpenseDetailRepository expenseDetailRepository;
    private final BookEntryRepository bookEntryRepository;
    private final TaxCalculationEngine taxCalculationEngine;
    private final TaxParameterService taxParameterService;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final TaxOfficeProperties taxOfficeProperties;
    private final TaxDeadlineRepository taxDeadlineRepository;

    private final Random random = new Random();

    @Transactional
    public TaxReturnResponse createDraft(Long userId, TaxReturnCreateRequest request) {
        int taxYear = request.taxYear();

        // Check if non-DRAFT TaxReturn already exists for this user+year
        boolean exists = taxReturnRepository.existsByUserIdAndTaxYearAndStatusNot(
                userId, taxYear, TaxReturnStatus.DRAFT);
        if (exists) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "해당 연도에 이미 제출된 종합소득세 신고서가 존재합니다.");
        }

        // Delete existing DRAFT if re-creating
        Optional<TaxReturn> existingDraft = taxReturnRepository.findByUserIdAndTaxYear(userId, taxYear);
        if (existingDraft.isPresent()) {
            TaxReturn draft = existingDraft.get();
            if (draft.getStatus() == TaxReturnStatus.DRAFT) {
                expenseDetailRepository.deleteByTaxReturn_Id(draft.getId());
                taxReturnRepository.delete(draft);
                taxReturnRepository.flush();
            }
        }

        // Prepaid tax and deductions
        long prepaidTax = request.prepaidTax() != null ? request.prepaidTax() : 0L;
        Map<String, Long> deductions = request.deductions() != null
                ? new HashMap<>(request.deductions())
                : new HashMap<>();

        // Get user info
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 경비조정 → 기본공제 → 세액공제 일괄 계산 (TaxCalculationEngine에 위임)
        TaxCalculationResult result = taxCalculationEngine.calculateForUser(
                userId, taxYear, prepaidTax, deductions);

        // Serialize deductions to JSON (calculateForUser가 기본공제를 추가했으므로 반영)
        deductions.putIfAbsent("기본공제_본인", taxParameterService.getBasicDeduction(taxYear));
        String deductionsJson = serializeDeductions(deductions);

        // Save TaxReturn
        TaxReturn taxReturn = TaxReturn.builder()
                .userId(userId)
                .taxYear(taxYear)
                .status(TaxReturnStatus.DRAFT)
                .taxpayerName(user.getName())
                .totalRevenue(result.totalRevenue())
                .totalExpense(result.totalExpense())
                .incomeAmount(result.incomeAmount())
                .totalDeductions(result.totalDeductions())
                .taxableIncome(result.taxableIncome())
                .taxRate(result.taxRate())
                .calculatedTax(result.calculatedTax())
                .determinedTax(result.determinedTax())
                .prepaidTax(result.prepaidTax())
                .finalTax(result.finalTax())
                .localTax(result.localTax())
                .deductionsJson(deductionsJson)
                .build();

        taxReturn = taxReturnRepository.save(taxReturn);

        // Save ExpenseDetails from sumExpenseByCategoryAndYear
        List<Object[]> categoryExpenses = bookEntryRepository.sumExpenseByCategoryAndYear(userId, taxYear);
        List<ExpenseDetail> expenseDetails = saveExpenseDetails(taxReturn, categoryExpenses);

        return TaxReturnResponse.from(taxReturn, expenseDetails);
    }

    public TaxReturnResponse getReturn(Long userId, Long returnId) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "종합소득세 신고서를 찾을 수 없습니다."));

        List<ExpenseDetail> details = expenseDetailRepository.findByTaxReturn_Id(returnId);
        return TaxReturnResponse.from(taxReturn, details);
    }

    public List<TaxReturnResponse> getReturns(Long userId) {
        List<TaxReturn> returns = taxReturnRepository.findByUserIdOrderByTaxYearDesc(userId);
        if (returns.isEmpty()) {
            return List.of();
        }

        List<Long> returnIds = returns.stream()
                .map(TaxReturn::getId)
                .toList();

        Map<Long, List<ExpenseDetail>> detailsByReturnId = expenseDetailRepository
                .findByTaxReturn_IdIn(returnIds)
                .stream()
                .collect(Collectors.groupingBy(d -> d.getTaxReturn().getId()));

        return returns.stream()
                .map(r -> TaxReturnResponse.from(r, detailsByReturnId.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Transactional
    public TaxReturnResponse updateReturn(Long userId, Long returnId, TaxReturnUpdateRequest request) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "종합소득세 신고서를 찾을 수 없습니다."));

        if (taxReturn.getStatus() != TaxReturnStatus.DRAFT) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "DRAFT 상태의 신고서만 수정할 수 있습니다.");
        }

        int taxYear = taxReturn.getTaxYear();

        // Updated prepaid tax and deductions
        long prepaidTax = request.prepaidTax() != null ? request.prepaidTax() : 0L;
        Map<String, Long> deductions = request.deductions() != null
                ? new HashMap<>(request.deductions())
                : new HashMap<>();

        // 경비조정 → 기본공제 → 세액공제 일괄 계산 (TaxCalculationEngine에 위임)
        TaxCalculationResult result = taxCalculationEngine.calculateForUser(
                userId, taxYear, prepaidTax, deductions);

        deductions.putIfAbsent("기본공제_본인", taxParameterService.getBasicDeduction(taxYear));
        String deductionsJson = serializeDeductions(deductions);

        // Update TaxReturn
        taxReturn.updateCalculation(
                result.totalRevenue(), result.totalExpense(), result.incomeAmount(),
                result.totalDeductions(), result.taxableIncome(), result.taxRate(),
                result.calculatedTax(), result.determinedTax(),
                result.prepaidTax(), result.finalTax(), result.localTax(),
                deductionsJson);

        // Re-save ExpenseDetails
        expenseDetailRepository.deleteByTaxReturn_Id(returnId);
        List<Object[]> categoryExpenses = bookEntryRepository.sumExpenseByCategoryAndYear(userId, taxYear);
        List<ExpenseDetail> expenseDetails = saveExpenseDetails(taxReturn, categoryExpenses);

        return TaxReturnResponse.from(taxReturn, expenseDetails);
    }

    @Transactional
    public TaxReturnSubmitResponse submit(Long userId, Long returnId) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_RETURN_NOT_FOUND));

        if (taxReturn.getStatus() != TaxReturnStatus.DRAFT) {
            throw new BusinessException(ErrorCode.TAX_ALREADY_SUBMITTED);
        }

        // Generate receipt number using Redis atomic increment
        String officeCode = taxOfficeProperties.getOfficeCode();
        int receiptYear = taxReturn.getTaxYear() + 1;
        String redisKey = "receipt:seq:" + receiptYear + ":" + officeCode;
        Long seq = redisTemplate.opsForValue().increment(redisKey);
        String receiptNumber = String.format("G%d-%s-%07d", receiptYear, officeCode, seq);

        // Transition DRAFT → SUBMITTED → ACCEPTED (simulation: instant acceptance)
        taxReturn.transitionTo(TaxReturnStatus.SUBMITTED);
        taxReturn.transitionTo(TaxReturnStatus.ACCEPTED);
        taxReturn.assignReceiptNumber(receiptNumber);

        // Create NATIONAL tax payment record
        String nationalVirtualAccount = "880-" + officeCode + "-" + String.format("%08d", random.nextInt(100000000));
        TaxPayment nationalPayment = TaxPayment.builder()
                .taxReturn(taxReturn)
                .paymentType(TaxPaymentType.NATIONAL)
                .amount(taxReturn.getFinalTax())
                .virtualAccount(nationalVirtualAccount)
                .virtualBank("싸피뱅크")
                .status(TaxPaymentStatus.PENDING)
                .build();

        // Create LOCAL tax payment record
        String localVirtualAccount = "770-6200-" + String.format("%08d", random.nextInt(100000000));
        TaxPayment localPayment = TaxPayment.builder()
                .taxReturn(taxReturn)
                .paymentType(TaxPaymentType.LOCAL)
                .amount(taxReturn.getLocalTax())
                .virtualAccount(localVirtualAccount)
                .virtualBank("싸피뱅크")
                .status(TaxPaymentStatus.PENDING)
                .build();

        taxPaymentRepository.save(nationalPayment);
        taxPaymentRepository.save(localPayment);

        // 납부기한: DB 조회 (폴백: 5월 31일)
        int deadlineYear = taxReturn.getTaxYear() + 1;
        LocalDate paymentDeadline = taxDeadlineRepository.findByYearAndName(deadlineYear, "종합소득세 신고")
                .or(() -> taxDeadlineRepository.findByYearAndName(taxReturn.getTaxYear(), "종합소득세 신고"))
                .map(d -> LocalDate.of(deadlineYear, d.getDeadlineMonth(), d.getDeadlineDay()))
                .orElse(LocalDate.of(deadlineYear, 5, 31));

        return new TaxReturnSubmitResponse(
                receiptNumber,
                taxReturn.getSubmittedAt(),
                taxReturn.getStatus(),
                taxReturn.getFinalTax(),
                taxReturn.getLocalTax(),
                taxReturn.getFinalTax() + taxReturn.getLocalTax(),
                new TaxReturnSubmitResponse.AccountInfo("싸피뱅크", nationalVirtualAccount),
                new TaxReturnSubmitResponse.AccountInfo("싸피뱅크", localVirtualAccount),
                paymentDeadline
        );
    }

    @Transactional(readOnly = true)
    public TaxPaymentStatusResponse getPaymentStatus(Long userId, Long returnId) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_RETURN_NOT_FOUND));

        List<TaxPayment> payments = taxPaymentRepository.findByTaxReturn_Id(taxReturn.getId());

        TaxPaymentStatusResponse.PaymentInfo nationalInfo = null;
        TaxPaymentStatusResponse.PaymentInfo localInfo = null;
        long totalPaid = 0;

        for (TaxPayment payment : payments) {
            TaxPaymentStatusResponse.PaymentInfo info = new TaxPaymentStatusResponse.PaymentInfo(
                    payment.getStatus(), payment.getAmount(), payment.getPaidAt());

            if (payment.getPaymentType() == TaxPaymentType.NATIONAL) {
                nationalInfo = info;
            } else if (payment.getPaymentType() == TaxPaymentType.LOCAL) {
                localInfo = info;
            }

            if (payment.getStatus() == TaxPaymentStatus.COMPLETED) {
                totalPaid += payment.getAmount();
            }
        }

        return new TaxPaymentStatusResponse(nationalInfo, localInfo, totalPaid);
    }

    private List<ExpenseDetail> saveExpenseDetails(TaxReturn taxReturn, List<Object[]> categoryExpenses) {
        List<ExpenseDetail> details = categoryExpenses.stream()
                .map(row -> ExpenseDetail.builder()
                        .taxReturn(taxReturn)
                        .expenseCode(row[0] != null ? row[0].toString() : "UNKNOWN")
                        .expenseName(row[1] != null ? row[1].toString() : "기타")
                        .amount(((Number) row[2]).longValue())
                        .build())
                .toList();

        return expenseDetailRepository.saveAll(details);
    }

    private String serializeDeductions(Map<String, Long> deductions) {
        try {
            return objectMapper.writeValueAsString(deductions);
        } catch (Exception e) {
            log.warn("Failed to serialize deductions: {}", e.getMessage());
            return "{}";
        }
    }
}
