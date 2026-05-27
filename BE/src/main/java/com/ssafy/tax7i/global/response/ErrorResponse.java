package com.ssafy.tax7i.global.response;

public record ErrorResponse<T>(
        String status,
        String errorCode,
        String message,
        T data
) {
    public ErrorResponse {
        if (status == null) {
            status = "fail";
        }
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode는 필수입니다.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message는 필수입니다.");
        }
    }

    public static <T> ErrorResponse<T> of(String errorCode, String message) {
        return new ErrorResponse<>("fail", errorCode, message, null);
    }

    public static <T> ErrorResponse<T> of(String errorCode, String message, T data) {
        return new ErrorResponse<>("fail", errorCode, message, data);
    }
}
