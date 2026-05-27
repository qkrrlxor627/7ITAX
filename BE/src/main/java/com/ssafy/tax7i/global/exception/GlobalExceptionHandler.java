package com.ssafy.tax7i.global.exception;

import com.ssafy.tax7i.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse<?>> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: [{}] {}", e.getErrorCode().name(), e.getMessage());

        ErrorResponse<?> errorResponse = e.getErrorData() != null
                ? ErrorResponse.of(e.getErrorCode().name(), e.getMessage(), e.getErrorData())
                : ErrorResponse.of(e.getErrorCode().name(), e.getMessage());

        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity
                .status(ErrorCode.INVALID_ARGUMENT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INVALID_ARGUMENT.name(), "입력값이 올바르지 않습니다.", fieldErrors));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse<?>> handleOptimisticLocking(OptimisticLockingFailureException e) {
        log.warn("Optimistic locking conflict: {}", e.getMessage());

        return ResponseEntity
                .status(ErrorCode.CONCURRENT_MODIFICATION.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.CONCURRENT_MODIFICATION.name(),
                        ErrorCode.CONCURRENT_MODIFICATION.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse<?>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage());

        return ResponseEntity
                .status(ErrorCode.CONFLICT.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.CONFLICT.name(), "데이터 충돌이 발생했습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse<?>> handleException(Exception e) {
        log.error("Unexpected exception: ", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR.name(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }
}
