package com.ssafy.tax7i.banking.client;

import com.ssafy.tax7i.banking.client.dto.SsafyResponseHeader;
import io.hypersistence.tsid.TSID;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 로컬 뱅킹/카드 시뮬레이션 공용 헬퍼.
 * 외부 SSAFY 금융망을 자체 구현 원장으로 대체하면서,
 * 기존 서비스 계약(Ssafy*Response DTO)을 그대로 유지하기 위한 응답 헤더/거래번호 생성을 담당한다.
 */
final class LocalLedgerSupport {

    static final String BANK_CODE = "999";
    static final String INSTITUTION_CODE = "00100";

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private LocalLedgerSupport() {
    }

    /** 거래 고유번호 생성 (TSID 기반, 문자열). */
    static String newTransactionUniqueNo() {
        return String.valueOf(TSID.Factory.getTsid().toLong());
    }

    static String today() {
        return LocalDateTime.now(ZONE).format(DATE_FMT);
    }

    static String now() {
        return LocalDateTime.now(ZONE).format(TIME_FMT);
    }

    /** 정상 처리(H0000) 응답 헤더 생성. */
    static SsafyResponseHeader successHeader(String apiName, String txnId) {
        return new SsafyResponseHeader(
                "H0000",
                "정상처리되었습니다.",
                apiName,
                today(),
                now(),
                INSTITUTION_CODE,
                null,
                apiName,
                txnId
        );
    }
}
