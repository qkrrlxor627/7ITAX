package com.ssafy.tax7i.classification.service;

import com.ssafy.tax7i.ai.service.AiClassificationService;
import com.ssafy.tax7i.classification.dto.ClassificationRequest;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import com.ssafy.tax7i.classification.dto.ClassificationResult.Confidence;
import com.ssafy.tax7i.classification.dto.ClassificationResult.EntertainmentLimitInfo;
import com.ssafy.tax7i.classification.entity.MccTaxRule;
import com.ssafy.tax7i.classification.entity.Merchant;
import com.ssafy.tax7i.classification.entity.MerchantKeywordMapping;
import com.ssafy.tax7i.classification.entity.TaxLimit;
import com.ssafy.tax7i.classification.repository.MerchantKeywordMappingRepository;
import com.ssafy.tax7i.classification.repository.MerchantRepository;
import com.ssafy.tax7i.classification.repository.TaxLimitRepository;
import com.ssafy.tax7i.tax.service.TaxParameterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 세목 자동분류 서비스
 *
 * 분류 우선순위:
 * 1. MCC EXACT (Tier A) → CONFIRMED (confidence ≥ 90)
 * 2. 금액 조건 (AMOUNT_LT / AMOUNT_GTE) → RECOMMENDED
 * 3. 가맹점명 키워드 (MERCHANT_LIKE / keyword_mapping) → RECOMMENDED
 * 4. 사용자 확인 필요 (거래처동행, 사업용 여부) → NEEDS_CONFIRMATION
 * 5. 미분류 → NEEDS_CONFIRMATION + AI 분류 대상
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxClassificationService {

    private static final String ENTERTAINMENT = "접대비";

    private final MerchantRepository merchantRepository;
    private final ClassificationCacheService classificationCacheService;
    private final MerchantKeywordMappingRepository keywordMappingRepository;
    private final EntertainmentLimitService entertainmentLimitService;
    private final TaxLimitRepository taxLimitRepository;
    private final AiClassificationService aiClassificationService;
    private final TaxParameterService taxParameterService;
    private final CategoryLearningService categoryLearningService;

    public ClassificationResult classify(ClassificationRequest request) {
        log.info("세목 분류 시작: merchant={}, mcc={}, amount={}",
                request.merchantName(), request.mcc(), request.amount());

        // 0. 사용자 학습 — 동일 가맹점 N회 이상 동일 세목 확정 시 자동 분류
        if (request.userId() != null && request.merchantName() != null) {
            ClassificationResult learned = categoryLearningService.findLearnedClassification(
                    request.userId(), request.merchantName());
            if (learned != null) {
                log.info("사용자 학습 확정: userId={}, merchant={}, category={}",
                        request.userId(), request.merchantName(), learned.taxCategory());
                return attachEntertainmentLimitIfNeeded(learned, request);
            }
        }

        // 1. MCC 결정: 요청에 있으면 사용, 없으면 가맹점명으로 조회
        String mcc = resolveMcc(request);
        if (mcc == null) {
            log.warn("MCC 결정 실패: merchant={}, AI 분류 시도", request.merchantName());
            Optional<ClassificationResult> aiResult = aiClassificationService.classify(request.merchantName());
            if (aiResult.isPresent()) {
                log.info("MCC 미결정 → AI 분류 성공: merchant={}, category={}",
                        request.merchantName(), aiResult.get().taxCategory());
                return attachEntertainmentLimitIfNeeded(aiResult.get(), request);
            }
            return ClassificationResult.needsConfirmation(
                    "기타 경비", "확인필요", null,
                    "MCC를 결정할 수 없습니다. 가맹점명을 확인해주세요.");
        }

        // 2. 해당 MCC의 룰 조회
        List<MccTaxRule> rules = classificationCacheService.getMccRules(mcc);
        if (rules.isEmpty()) {
            log.warn("분류 룰 없음: mcc={}, merchant={}, AI 분류 시도", mcc, request.merchantName());
            Optional<ClassificationResult> aiResult = aiClassificationService.classify(request.merchantName());
            if (aiResult.isPresent()) {
                log.info("룰 없음 → AI 분류 성공: mcc={}, merchant={}, category={}",
                        mcc, request.merchantName(), aiResult.get().taxCategory());
                return attachEntertainmentLimitIfNeeded(aiResult.get(), request);
            }
            return ClassificationResult.needsConfirmation(
                    "기타 경비", "확인필요", null,
                    "MCC " + mcc + "에 대한 분류 룰이 없습니다.");
        }

        // 3. Tier A 확인 — 자동 확정
        ClassificationResult tierAResult = tryTierA(rules);
        if (tierAResult != null) {
            log.info("Tier A 확정: mcc={}, category={}, score={}",
                    mcc, tierAResult.taxCategory(), tierAResult.confidenceScore());
            return attachEntertainmentLimitIfNeeded(tierAResult, request);
        }

        // 4. Tier B — 금액 조건
        ClassificationResult amountResult = tryAmountCondition(rules, request.amount());
        if (amountResult != null) {
            log.info("Tier B 금액조건 매칭: mcc={}, amount={}, category={}, score={}",
                    mcc, request.amount(), amountResult.taxCategory(), amountResult.confidenceScore());
            return attachEntertainmentLimitIfNeeded(amountResult, request);
        }

        // 5. Tier B — 가맹점명 키워드 (룰의 MERCHANT_LIKE + keyword_mapping 테이블)
        ClassificationResult keywordResult = tryKeywordCondition(rules, mcc, request.merchantName());
        if (keywordResult != null) {
            log.info("Tier B 키워드 매칭: mcc={}, merchant={}, category={}, score={}",
                    mcc, request.merchantName(), keywordResult.taxCategory(), keywordResult.confidenceScore());
            return attachEntertainmentLimitIfNeeded(keywordResult, request);
        }

        // 6. Tier B — 사용자 확인 조건 (거래처동행, 사업용 등)
        ClassificationResult userChoiceResult = tryUserChoiceCondition(rules, request);
        if (userChoiceResult != null) {
            log.info("Tier B 사용자확인: mcc={}, category={}, confidence={}",
                    mcc, userChoiceResult.taxCategory(), userChoiceResult.confidence());
            return attachEntertainmentLimitIfNeeded(userChoiceResult, request);
        }

        // 7. AI 분류 시도 — 모든 규칙 매칭 실패 시
        Optional<ClassificationResult> aiResult = aiClassificationService.classify(request.merchantName());
        if (aiResult.isPresent()) {
            log.info("전 단계 미분류 → AI 분류 성공: mcc={}, merchant={}, category={}",
                    mcc, request.merchantName(), aiResult.get().taxCategory());
            return attachEntertainmentLimitIfNeeded(aiResult.get(), request);
        }

        // 8. 최종 폴백 — 첫 번째 Tier B 룰의 세목을 추천
        MccTaxRule fallback = rules.stream()
                .filter(r -> !r.isTierA())
                .findFirst()
                .orElse(rules.get(0));

        log.warn("미분류 폴백: mcc={}, merchant={}, fallback={}",
                mcc, request.merchantName(), fallback.getTaxCategory());

        ClassificationResult result = ClassificationResult.needsConfirmation(
                fallback.getTaxCategory(), fallback.getVatDeductible(),
                fallback.getLegalBasis(), fallback.getRemark());
        return attachEntertainmentLimitIfNeeded(result, request);
    }

    // ── MCC 결정 ──

    private String resolveMcc(ClassificationRequest request) {
        if (request.mcc() != null && !request.mcc().isBlank()) {
            return request.mcc().trim();
        }
        // 가맹점명으로 정확 매칭
        return classificationCacheService.getMerchantByName(request.merchantName())
                .map(Merchant::getMcc)
                // 부분 매칭 — LIKE 특수문자 이스케이프
                .or(() -> {
                    String escaped = request.merchantName()
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_");
                    return merchantRepository.findByMerchantNameContainedIn(escaped)
                            .stream().findFirst().map(Merchant::getMcc);
                })
                .orElse(null);
    }

    // ── Tier A: 자동 확정 ──

    private ClassificationResult tryTierA(List<MccTaxRule> rules) {
        return rules.stream()
                .filter(MccTaxRule::isTierA)
                .findFirst()
                .map(rule -> {
                    int score = rule.getConfidence() != null ? rule.getConfidence() : 90;
                    // requiresUserConfirm이 true면 RECOMMENDED로 (예: 수도광열비 - 별도사무실 조건)
                    if (Boolean.TRUE.equals(rule.getRequiresUserConfirm())) {
                        return ClassificationResult.recommended(
                                rule.getTaxCategory(), rule.getVatDeductible(),
                                rule.getLegalBasis(), rule.getRemark(), score);
                    }
                    return ClassificationResult.confirmed(
                            rule.getTaxCategory(), rule.getVatDeductible(),
                            rule.getLegalBasis(), rule.getRemark(), score);
                })
                .orElse(null);
    }

    // ── Tier B: 금액 조건 ──

    private ClassificationResult tryAmountCondition(List<MccTaxRule> rules, Long amount) {
        if (amount == null) return null;

        for (MccTaxRule rule : rules) {
            if (!rule.isAmountCondition()) continue;

            String expr = rule.getConditionExpr();
            Long threshold = rule.getAmountThreshold();
            if (threshold == null) continue;

            boolean matched = false;
            if ("AMOUNT_LT_1000000".equals(expr) && amount < threshold) {
                matched = true;
            } else if ("AMOUNT_GTE_1000000".equals(expr) && amount >= threshold) {
                matched = true;
            }

            if (matched) {
                int score = rule.getConfidence() != null ? rule.getConfidence() : 70;
                return ClassificationResult.recommended(
                        rule.getTaxCategory(), rule.getVatDeductible(),
                        rule.getLegalBasis(), rule.getRemark(), score);
            }
        }
        return null;
    }

    // ── Tier B: 키워드 매칭 ──

    private ClassificationResult tryKeywordCondition(List<MccTaxRule> rules, String mcc, String merchantName) {
        if (merchantName == null || merchantName.isBlank()) return null;
        String upperName = merchantName.toUpperCase();

        // 1) 룰의 MERCHANT_LIKE 조건 체크
        for (MccTaxRule rule : rules) {
            if (!rule.isMerchantKeywordCondition()) continue;

            String keywordsStr = rule.getConditionExpr().substring("MERCHANT_LIKE:".length()).trim();
            if (keywordsStr.isEmpty()) continue;
            String[] keywords = keywordsStr.split(",");

            for (String keyword : keywords) {
                if (upperName.contains(keyword.trim().toUpperCase())) {
                    int score = rule.getConfidence() != null ? rule.getConfidence() : 85;
                    return ClassificationResult.recommended(
                            rule.getTaxCategory(), rule.getVatDeductible(),
                            rule.getLegalBasis(), rule.getRemark(), score);
                }
            }
        }

        // 2) merchant_keyword_mapping 테이블 체크
        List<MerchantKeywordMapping> keywordMappings = keywordMappingRepository.findByMcc(mcc);
        for (MerchantKeywordMapping mapping : keywordMappings) {
            if (upperName.contains(mapping.getKeyword().toUpperCase())) {
                int score = mapping.getConfidence() != null ? mapping.getConfidence() : 80;
                return ClassificationResult.recommended(
                        mapping.getTaxCategory(), "확인필요", null, null, score);
            }
        }

        // 3) MCC 5817 DEFAULT 룰 (키워드 매칭 안 되면 지급수수료)
        if ("5817".equals(mcc)) {
            return rules.stream()
                    .filter(r -> "DEFAULT".equals(r.getConditionExpr()))
                    .findFirst()
                    .map(rule -> {
                        int score = rule.getConfidence() != null ? rule.getConfidence() : 55;
                        if (Boolean.TRUE.equals(rule.getRequiresUserConfirm())) {
                            return ClassificationResult.needsConfirmation(
                                    rule.getTaxCategory(), rule.getVatDeductible(),
                                    rule.getLegalBasis(), rule.getRemark());
                        }
                        return ClassificationResult.recommended(
                                rule.getTaxCategory(), rule.getVatDeductible(),
                                rule.getLegalBasis(), rule.getRemark(), score);
                    })
                    .orElse(null);
        }

        return null;
    }

    // ── Tier B: 사용자 확인 조건 ──

    private ClassificationResult tryUserChoiceCondition(List<MccTaxRule> rules, ClassificationRequest request) {
        for (MccTaxRule rule : rules) {
            if (!rule.isUserChoiceCondition()) continue;

            String condition = rule.getConditionExpr();
            int score = rule.getConfidence() != null ? rule.getConfidence() : 70;

            // 거래처 동행 조건
            if ("거래처동행".equals(condition)) {
                if (request.isClientAccompanied() == null) {
                    return ClassificationResult.needsConfirmation(
                            rule.getTaxCategory(), rule.getVatDeductible(),
                            rule.getLegalBasis(), rule.getRemark());
                }
                if (request.isClientAccompanied()) {
                    return ClassificationResult.recommended(
                            rule.getTaxCategory(), rule.getVatDeductible(),
                            rule.getLegalBasis(), null, score);
                }
                // 거래처 동행 아님 → 경비불인정
                return ClassificationResult.recommended(
                        "경비불인정", "불공제", "소득세법§33①5",
                        "1인 사업자 본인 식대/카페는 가사경비로 경비불인정", 90);
            }

            // 개인식사/개인음주 조건 (사용자가 거래처동행 false 선택 시 매칭)
            if ("개인식사".equals(condition) || "개인음주".equals(condition)) {
                continue; // 거래처동행 조건에서 처리
            }

            // 사업용 여부 조건 (conditionExpr = MCC_EXACT)
            if ("MCC_EXACT".equals(condition)) {
                if (request.isBusinessPurpose() == null) {
                    return ClassificationResult.needsConfirmation(
                            rule.getTaxCategory(), rule.getVatDeductible(),
                            rule.getLegalBasis(), rule.getRemark());
                }
                if (request.isBusinessPurpose()) {
                    return ClassificationResult.recommended(
                            rule.getTaxCategory(), rule.getVatDeductible(),
                            rule.getLegalBasis(), null, score);
                }
                return ClassificationResult.recommended(
                        "경비불인정", "불공제", null,
                        "사업용이 아닌 지출은 경비로 인정되지 않습니다.", 90);
            }

            // DEFAULT 룰 (5999 편의점 등)
            if ("DEFAULT".equals(condition)) {
                if (Boolean.TRUE.equals(rule.getRequiresUserConfirm())) {
                    return ClassificationResult.needsConfirmation(
                            rule.getTaxCategory(), rule.getVatDeductible(),
                            rule.getLegalBasis(), rule.getRemark());
                }
                return ClassificationResult.recommended(
                        rule.getTaxCategory(), rule.getVatDeductible(),
                        rule.getLegalBasis(), rule.getRemark(), score);
            }
        }
        return null;
    }

    // ── 접대비 누적 한도 체크 ──

    private ClassificationResult attachEntertainmentLimitIfNeeded(
            ClassificationResult result, ClassificationRequest request) {
        if (!ENTERTAINMENT.equals(result.taxCategory())) {
            return result;
        }
        if (request.userId() == null) {
            return result;
        }

        // tax_limit 테이블에서 접대비 연간기본한도 조회, 없으면 TaxParameter DB 조회
        int currentYear = LocalDate.now().getYear();
        long annualLimit = taxLimitRepository
                .findByTaxCategoryAndLimitType(ENTERTAINMENT, "연간기본한도")
                .map(TaxLimit::getLimitAmount)
                .orElseGet(() -> taxParameterService.getEntertainmentLimit(currentYear));

        long usedAmount = entertainmentLimitService.getUsedEntertainmentAmount(
                request.userId());

        long remaining = annualLimit - usedAmount;
        boolean isOver = remaining <= 0;

        EntertainmentLimitInfo limitInfo = new EntertainmentLimitInfo(
                annualLimit, usedAmount, Math.max(0, remaining), isOver);

        ClassificationResult withLimit = result.withEntertainmentLimit(limitInfo);

        if (isOver) {
            log.warn("접대비 연간 한도 초과: userId={}, used={}, limit={}",
                    request.userId(), usedAmount, annualLimit);
        }

        return withLimit;
    }
}
