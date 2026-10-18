package com.ssafy.tax7i.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableRetry
public class AsyncRetryConfig {

    /**
     * 모든 @Async의 기본 실행기: 장부 자동 생성·결제 취소 반영·카드 거래 기록·FCM 알림.
     * 큐가 차면 호출 스레드가 직접 실행(CallerRuns)하는데, 커밋 직후 요청 스레드가 커넥션을 쥔 채 REQUIRES_NEW 작업을 하면
     * DB 풀 교착이 생길 수 있어 큐를 넉넉히 둔다. 스레드 수는 DB 풀(DB_POOL_MAX)보다 작게 유지한다
     */
    @Bean
    public Executor taskExecutor(@Value("${app.async.core-size:4}") int coreSize,
                                 @Value("${app.async.max-size:8}") int maxSize,
                                 @Value("${app.async.queue-capacity:500}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
