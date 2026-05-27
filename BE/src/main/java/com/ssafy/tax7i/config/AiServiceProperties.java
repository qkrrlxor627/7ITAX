package com.ssafy.tax7i.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiServiceProperties(
        String baseUrl,
        int connectTimeout,
        int readTimeout
) {
}
