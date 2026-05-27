package com.ssafy.tax7i.banking.client;

import com.ssafy.tax7i.banking.client.dto.SsafyCommonHeader;
import com.ssafy.tax7i.config.SsafyFinanceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class SsafyFinanceHeaderBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");
    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private final SsafyFinanceProperties properties;

    public SsafyCommonHeader build(String apiName, String userKey) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        String date = now.format(DATE_FMT);
        String time = now.format(TIME_FMT);
        String uniqueNo = date + time + generateSequence();

        return new SsafyCommonHeader(
                apiName,
                date,
                time,
                properties.institutionCode(),
                properties.fintechAppNo(),
                apiName,
                uniqueNo,
                properties.apiKey(),
                userKey
        );
    }

    private String generateSequence() {
        long seq = SEQUENCE.getAndIncrement() % 1_000_000;
        return String.format("%06d", seq);
    }
}
