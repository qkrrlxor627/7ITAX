package com.ssafy.tax7i.global.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 감사 로깅 대상 메서드에 부착하는 마커 애너테이션.
 * <p>
 * 컨트롤러/서비스 메서드에 부착하면 {@link AuditLogAspect}가 실행 주체(userId), 요청 IP,
 * 성공/실패 여부, 소요 시간을 "AUDIT" 로거로 남긴다. 송금/결제/세금 신고/인증 등
 * 추적이 필요한 민감 작업에만 선별적으로 사용한다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {

    /**
     * 감사 로그에 기록될 행위 식별자. 예: "TRANSFER", "PAYMENT", "TAX_RETURN_SUBMIT".
     */
    String action();

    /**
     * 메서드 인자를 로그에 포함할지 여부. 개인정보/PIN 등 민감 인자가 있는 메서드는 false 유지.
     */
    boolean includeArgs() default false;
}
