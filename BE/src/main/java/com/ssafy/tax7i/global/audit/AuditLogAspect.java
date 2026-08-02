package com.ssafy.tax7i.global.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;

/**
 * {@link Auditable}이 부착된 메서드의 실행을 가로채 감사 로그를 남기는 Aspect.
 * <p>
 * 로깅 관심사를 비즈니스 로직과 분리하기 위한 횡단 관심사 구현이며,
 * 전용 "AUDIT" 로거를 사용하므로 logback에서 별도 appender(파일/외부 수집)로 라우팅할 수 있다.
 */
@Aspect
@Component
@Slf4j(topic = "AUDIT")
public class AuditLogAspect {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String ANONYMOUS = "anonymous";
    private static final String UNKNOWN = "-";

    @Around("@annotation(auditable)")
    public Object writeAuditLog(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long start = System.currentTimeMillis();
        String actor = resolveActor();
        String clientIp = resolveClientIp();
        String method = ((MethodSignature) joinPoint.getSignature()).toShortString();
        String args = auditable.includeArgs() ? Arrays.toString(joinPoint.getArgs()) : UNKNOWN;

        try {
            Object result = joinPoint.proceed();
            log.info("action={} result=SUCCESS actor={} ip={} method={} args={} took={}ms",
                    auditable.action(), actor, clientIp, method, args, elapsed(start));
            return result;
        } catch (Throwable e) {
            log.warn("action={} result=FAILURE actor={} ip={} method={} args={} took={}ms cause={}: {}",
                    auditable.action(), actor, clientIp, method, args, elapsed(start),
                    e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private long elapsed(long start) {
        return System.currentTimeMillis() - start;
    }

    /**
     * SecurityContext에 저장된 principal(JwtAuthenticationFilter가 주입한 userId)을 반환한다.
     */
    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return ANONYMOUS;
        }
        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            return ANONYMOUS;
        }
        return String.valueOf(principal);
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return UNKNOWN;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader(FORWARDED_FOR_HEADER);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (IllegalStateException e) {
            // 요청 스레드 밖(@Async, 스케줄러 등)에서 호출된 경우
            return UNKNOWN;
        }
    }
}
