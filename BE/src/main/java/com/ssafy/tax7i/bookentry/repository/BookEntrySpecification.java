package com.ssafy.tax7i.bookentry.repository;

import com.ssafy.tax7i.bookentry.entity.BookEntry;
import com.ssafy.tax7i.bookentry.entity.EntryType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * 간편장부 목록 동적 필터 Specification
 *
 * 지원 필터:
 * - userId (필수)
 * - confirmed (사용자 확인 여부)
 * - entryType (수입/비용/자산)
 * - startDate~endDate (거래일 범위)
 * - categoryCode (계정과목)
 * - keyword (거래처 검색, LIKE)
 * - hasEvidence (적격증빙 — 소득세법 시행령 §208의2: paymentId 존재 = 카드전표)
 * - unclassified (미분류: categoryCode IS NULL)
 */
public final class BookEntrySpecification {

    private BookEntrySpecification() {}

    public static Specification<BookEntry> withUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<BookEntry> withConfirmed(Boolean confirmed) {
        if (confirmed == null) return null;
        return (root, query, cb) -> cb.equal(root.get("confirmed"), confirmed);
    }

    public static Specification<BookEntry> withEntryType(EntryType entryType) {
        if (entryType == null) return null;
        return (root, query, cb) -> cb.equal(root.get("entryType"), entryType);
    }

    public static Specification<BookEntry> withStartDate(LocalDate startDate) {
        if (startDate == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("entryDate"), startDate);
    }

    public static Specification<BookEntry> withEndDate(LocalDate endDate) {
        if (endDate == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("entryDate"), endDate);
    }

    public static Specification<BookEntry> withCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("categoryCode"), categoryCode);
    }

    /** 거래처(가맹점명) 키워드 검색 — 대소문자 무시, LIKE 특수문자 이스케이프 */
    public static Specification<BookEntry> withKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String escaped = keyword.toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            return cb.like(cb.lower(root.get("merchantName")), "%" + escaped + "%", '\\');
        };
    }

    /**
     * 적격증빙 필터 — 소득세법 시행령 §208의2
     * paymentId NOT NULL = 카드 결제 건 = 신용카드매출전표(적격증빙)
     * paymentId NULL = 수동 등록 건 = 증빙 미확인
     */
    public static Specification<BookEntry> withHasEvidence(Boolean hasEvidence) {
        if (hasEvidence == null) return null;
        return (root, query, cb) -> hasEvidence
                ? cb.isNotNull(root.get("paymentId"))
                : cb.isNull(root.get("paymentId"));
    }

    /** 미분류 필터: categoryCode가 null인 거래 */
    public static Specification<BookEntry> withUnclassified(Boolean unclassified) {
        if (unclassified == null) return null;
        return (root, query, cb) -> unclassified
                ? cb.isNull(root.get("categoryCode"))
                : cb.isNotNull(root.get("categoryCode"));
    }
}
