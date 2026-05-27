package com.ssafy.tax7i.global.response;

public record SuccessResponse<T>(
        String status,
        String message,
        T data
) {
    public SuccessResponse {
        if (status == null) {
            status = "success";
        }
        if (message == null || message.isBlank()) {
            message = "OK";
        }
    }

    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>("success", "OK", data);
    }

    public static <T> SuccessResponse<T> of(String message, T data) {
        return new SuccessResponse<>("success", message, data);
    }

    public static SuccessResponse<Void> ok() {
        return new SuccessResponse<>("success", "OK", null);
    }

    public static SuccessResponse<Void> ok(String message) {
        return new SuccessResponse<>("success", message, null);
    }
}
