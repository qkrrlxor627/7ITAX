package com.ssafy.tax7i.classification.service;

import com.ssafy.tax7i.bookentry.repository.BookEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.Cacheable;

import java.time.LocalDate;

/**
 * 접대비 연간 누적 사용액 조회 서비스
 * 한도 계산: 1,200만원 + 총수입금액 × 0.2% (소득세법§35, 시행령§79)
 * ※ 한도 계산 자체는 TaxParameterService.getEntertainmentLimit(year, totalRevenue)에서 수행
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntertainmentLimitService {

    private final BookEntryRepository bookEntryRepository;

    /**
     * 해당 연도의 접대비 사용 누적 금액 조회
     */
    @Cacheable(value = "entertainmentUsed", key = "#userId")
    public long getUsedEntertainmentAmount(Long userId) {
        int currentYear = LocalDate.now().getYear();
        try {
            Long total = bookEntryRepository.sumAmountByUserIdAndCategoryNameAndYear(
                    userId, "접대비", currentYear);
            long result = total != null ? total : 0L;
            log.debug("접대비 누적 조회: userId={}, year={}, used={}원", userId, currentYear, result);
            return result;
        } catch (Exception e) {
            log.error("접대비 누적 조회 실패: userId={}, year={}, error={}",
                    userId, currentYear, e.getMessage());
            return 0L;
        }
    }
}
