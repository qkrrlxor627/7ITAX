package com.ssafy.tax7i.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    MISSING_FIELD(HttpStatus.BAD_REQUEST, "필수 항목이 누락되었습니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "데이터 형식이 올바르지 않습니다."),
    DUPLICATE_VALUE(HttpStatus.BAD_REQUEST, "중복된 값입니다."),
    SELF_TRANSFER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신에게 송금할 수 없습니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),

    // 401 Unauthorized (Auth/PIN)
    PIN_INVALID(HttpStatus.UNAUTHORIZED, "PIN이 올바르지 않습니다."),
    PIN_NOT_SET(HttpStatus.UNAUTHORIZED, "PIN이 설정되지 않았습니다."),
    CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "필수 약관 동의가 필요합니다."),
    IDENTITY_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "본인인증에 실패했습니다."),
    PIN_ATTEMPTS_EXCEEDED(HttpStatus.BAD_REQUEST, "PIN 입력 횟수를 초과했습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 계정입니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다."),
    CARD_INACTIVE(HttpStatus.FORBIDDEN, "비활성 카드입니다."),
    CHAT_SESSION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 채팅 세션에 대한 접근 권한이 없습니다."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "카드를 찾을 수 없습니다."),
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "송금 내역을 찾을 수 없습니다."),
    BOOK_ENTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "장부 항목을 찾을 수 없습니다."), // 장부 항목 찾을 수 없음
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계좌를 찾을 수 없습니다."),

    // 409 Conflict
    CONFLICT(HttpStatus.CONFLICT, "리소스 충돌이 발생했습니다."),
    DUPLICATE_BOOK_ENTRY(HttpStatus.CONFLICT, "이미 장부가 생성된 결제입니다."), // 동일 paymentId 중복 장부 방지
    ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확인된 전표입니다."), // 확인 완료된 전표 재확인 방지
    TAX_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출된 신고서입니다."),
    TAX_ALREADY_PAID(HttpStatus.CONFLICT, "이미 납부 완료된 세금입니다."),
    TAX_NATIONAL_FIRST(HttpStatus.BAD_REQUEST, "국세 납부를 먼저 완료해주세요."),
    TAX_RETURN_NOT_FOUND(HttpStatus.NOT_FOUND, "신고서를 찾을 수 없습니다."),
    TAX_INVALID_TRANSITION(HttpStatus.CONFLICT, "현재 상태에서 해당 작업을 수행할 수 없습니다."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "다른 요청과 동시에 처리되었습니다. 다시 시도해주세요."),

    // 402 Payment Required
    PAYMENT_DECLINED(HttpStatus.valueOf(402), "결제가 거절되었습니다."),

    // 410 Gone
    QR_TOKEN_EXPIRED(HttpStatus.valueOf(410), "QR 결제 토큰이 만료되었습니다."),

    // 404 Not Found
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제를 찾을 수 없습니다."),

    // 422 Unprocessable Entity
    INSUFFICIENT_BALANCE(HttpStatus.UNPROCESSABLE_ENTITY, "잔액이 부족합니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 400 Bad Request (OTP)
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    OTP_INVALID(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다."),
    OTP_ATTEMPTS_EXCEEDED(HttpStatus.BAD_REQUEST, "인증번호 입력 횟수를 초과했습니다."),

    // 401 Unauthorized (OTP Token)
    OTP_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "OTP 인증 토큰이 유효하지 않습니다."),

    // 429 Too Many Requests
    OTP_SEND_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "인증번호 발송 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),

    // 429 Too Many Requests (AI)
    AI_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),

    // 500 Internal Server Error (FCM)
    FCM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "푸시 알림 전송에 실패했습니다."),

    // 503 Service Unavailable
    BANK_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "은행 연동 서비스가 일시적으로 불가합니다."),
    AI_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 일시적으로 불가합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public int getHttpStatusCode() {
        return httpStatus.value();
    }
}
