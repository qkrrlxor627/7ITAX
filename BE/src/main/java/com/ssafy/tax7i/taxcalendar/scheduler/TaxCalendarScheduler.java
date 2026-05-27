package com.ssafy.tax7i.taxcalendar.scheduler;

import com.ssafy.tax7i.fcm.repository.FcmTokenRepository;
import com.ssafy.tax7i.fcm.service.FcmService;
import com.ssafy.tax7i.taxcalendar.dto.TaxDeadlineResponse;
import com.ssafy.tax7i.taxcalendar.service.TaxCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaxCalendarScheduler {

    private static final Set<Integer> ALERT_DAYS = Set.of(0, 3, 7);

    private final TaxCalendarService taxCalendarService;
    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRepository;

    @Scheduled(cron = "0 0 9 * * *")
    public void checkTaxDeadlines() {
        LocalDate today = LocalDate.now();
        List<TaxDeadlineResponse> deadlines = taxCalendarService.getUpcomingDeadlines(today);

        List<TaxDeadlineResponse> alertDeadlines = deadlines.stream()
                .filter(d -> ALERT_DAYS.contains(d.dDay()))
                .toList();

        if (alertDeadlines.isEmpty()) {
            log.debug("오늘 세금 알림 대상 없음");
            return;
        }

        List<Long> userIds = fcmTokenRepository.findDistinctUserIds();
        log.info("세금 마감 알림 전송: deadlines={}, users={}", alertDeadlines.size(), userIds.size());

        for (TaxDeadlineResponse deadline : alertDeadlines) {
            String title = formatTitle(deadline);
            String body = formatBody(deadline);
            Map<String, String> data = Map.of(
                    "type", "tax_calendar",
                    "taxName", deadline.taxName(),
                    "dDay", String.valueOf(deadline.dDay()),
                    "deadline", deadline.deadline());

            for (Long userId : userIds) {
                fcmService.sendNotification(userId, title, body, data);
            }
        }
    }

    private String formatTitle(TaxDeadlineResponse d) {
        if (d.dDay() == 0) return "오늘이 " + d.taxName() + " 마감일입니다!";
        return d.taxName() + " D-" + d.dDay();
    }

    private String formatBody(TaxDeadlineResponse d) {
        if (d.dDay() == 0) return d.description() + " 마감일입니다. 지금 바로 확인하세요!";
        return d.description() + " 마감 " + d.dDay() + "일 전입니다.";
    }
}
