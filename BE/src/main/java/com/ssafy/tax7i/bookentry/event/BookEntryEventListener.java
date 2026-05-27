package com.ssafy.tax7i.bookentry.event;

import com.ssafy.tax7i.bookentry.dto.BookEntryCreateRequest;
import com.ssafy.tax7i.bookentry.dto.BookEntryResponse;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import com.ssafy.tax7i.bookentry.service.BookEntryService;
import com.ssafy.tax7i.classification.dto.ClassificationRequest;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.entity.TaxCategory;
import com.ssafy.tax7i.classification.service.TaxClassificationService;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 결제 확정 이벤트를 수신하여 간편장부를 자동 생성 + 세목 자동분류하는 리스너.
 * - AFTER_COMMIT: 결제 트랜잭션 커밋 후 실행 → 장부 실패해도 결제 롤백 안 됨
 * - 사업자 카드 결제(BUSINESS)만 장부 생성, 개인 결제(PERSONAL)는 스킵
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookEntryEventListener {

    /** mcc_tax_rule.vat_deductible 컬럼의 면세 상태값 */
    private static final String VAT_STATUS_EXEMPT = "면세";

    private final BookEntryService bookEntryService;
    private final TaxClassificationService classificationService;
    private final TaxParameterService taxParameterService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCaptured(PaymentCapturedEvent event) {
        // 사업자 카드 결제만 장부 생성 (개인 결제는 장부 불필요)
        if (!"BUSINESS".equals(event.purpose())) {
            log.debug("개인 결제는 장부 생성 스킵: paymentId={}", event.paymentId());
            return;
        }

        try {
            // 1. MCC + 가맹점명 기반 세목 자동분류
            ClassificationResult classification = classificationService.classify(
                    new ClassificationRequest(
                            event.merchantName(),
                            event.merchantCategoryCode(),
                            event.amount(),
                            null, // isBusinessPurpose — 사업용 결제이므로 나중에 확인
                            null, // isClientAccompanied — 나중에 사용자 확인
                            event.userId(),
                            null  // note — 증빙 메모 없음
                    )
            );

            // 2. 분류 결과에서 세목 코드/이름 추출
            String categoryCode = resolveCategoryCode(classification.taxCategory());
            String categoryName = classification.taxCategory();

            // 3. 경비불인정이면 면세 처리
            boolean isVatExempt = TaxCategory.NOT_DEDUCTIBLE.getName().equals(categoryName)
                    || VAT_STATUS_EXEMPT.equals(classification.vatDeductible());

            // 4. 100만원 이상 감가상각비면 ASSET 타입
            EntryType entryType = EntryType.EXPENSE;
            if (TaxCategory.DEPRECIATION.getName().equals(categoryName)) {
                entryType = EntryType.ASSET;
            }

            String note = buildNote(classification);

            BookEntryCreateRequest request = new BookEntryCreateRequest(
                    event.paymentId(),
                    event.capturedAt().toLocalDate(),
                    event.merchantName() + " 결제",
                    event.merchantName(),
                    entryType,
                    event.amount(),
                    isVatExempt,
                    categoryCode,
                    categoryName,
                    classification.confidenceScore(),
                    note
            );

            BookEntryResponse response = bookEntryService.create(event.userId(), request);

            log.info("장부 자동 생성+분류: id={}, paymentId={}, category={}({}), confidence={}",
                    response.id(), event.paymentId(), categoryName, categoryCode,
                    classification.confidence());

        } catch (Exception e) {
            log.error("장부 자동 생성 실패: paymentId={}, userId={}, amount={}, error={}",
                    event.paymentId(), event.userId(), event.amount(), e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTransferReceived(TransferReceivedEvent event) {
        try {
            String description = event.senderName() != null
                    ? event.senderName() + " 입금"
                    : event.description();

            int taxYear = event.transferredAt().getYear();
            double withholdingRate = taxParameterService.getWithholdingRate(taxYear) * 100;

            bookEntryService.createIncome(
                    event.receiverUserId(),
                    new com.ssafy.tax7i.bookentry.dto.IncomeCreateRequest(
                            event.transferredAt().toLocalDate(),
                            description,
                            event.amount(),
                            withholdingRate,
                            "P2P 이체 자동 매출 등록"
                    )
            );

            log.info("이체 매출 자동 생성: transferId={}, receiverUserId={}, amount={}",
                    event.transferId(), event.receiverUserId(), event.amount());

        } catch (Exception e) {
            log.error("이체 매출 자동 생성 실패: transferId={}, error={}",
                    event.transferId(), e.getMessage(), e);
        }
    }

    private String resolveCategoryCode(String taxCategoryName) {
        for (TaxCategory tc : TaxCategory.values()) {
            if (tc.getName().equals(taxCategoryName)) {
                return tc.getCode();
            }
        }
        return TaxCategory.OTHER_EXPENSE.getCode();
    }

    private String buildNote(ClassificationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(result.confidence()).append("]");

        if (result.remark() != null && !result.remark().isBlank()) {
            sb.append(" ").append(result.remark());
        }

        if (result.entertainmentLimit() != null) {
            var limit = result.entertainmentLimit();
            sb.append(String.format(" | 접대비 한도: %,d/%,d원 (잔여 %,d원)",
                    limit.usedAmount(), limit.annualLimit(), limit.remainingAmount()));
            if (limit.isOverLimit()) {
                sb.append(" ⚠ 한도초과");
            }
        }

        return sb.toString();
    }
}
