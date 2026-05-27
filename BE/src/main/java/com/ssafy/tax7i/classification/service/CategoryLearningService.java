package com.ssafy.tax7i.classification.service;

import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import com.ssafy.tax7i.bookentry.repository.LearnedCategoryProjection;
import com.ssafy.tax7i.classification.dto.ClassificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 학습 기반 세목 분류 서비스.
 * TaxClassificationService → BookEntryRepository 직접 의존을 제거하여
 * classification ↔ bookentry 양방향 의존성을 해소한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryLearningService {

    /** 동일 가맹점에서 동일 세목으로 확정된 최소 건수 (이상이면 자동 분류 적용) */
    public static final int LEARNING_THRESHOLD = 3;

    private final BookEntryRepository bookEntryRepository;

    /**
     * 사용자 학습 데이터에서 가맹점에 대한 확정 세목을 조회한다.
     * 동일 가맹점 + 동일 세목으로 {@value LEARNING_THRESHOLD}회 이상 확정된 경우 자동 분류.
     *
     * @return ���습된 분류 결과, 없으면 null
     */
    public ClassificationResult findLearnedClassification(Long userId, String merchantName) {
        List<LearnedCategoryProjection> results = bookEntryRepository.findLearnedCategory(
                userId, merchantName, LEARNING_THRESHOLD);
        if (results.isEmpty()) return null;

        LearnedCategoryProjection top = results.get(0);
        boolean vatDeductible = Boolean.TRUE.equals(top.getIsVatDeductible());
        long count = top.getCount() != null ? top.getCount() : 0;

        return ClassificationResult.confirmed(
                top.getCategoryName(),
                vatDeductible ? "공제" : "불공제",
                null,
                "사용자 학습: 동일 가맹점 " + count + "회 동일 분류",
                95);
    }
}
