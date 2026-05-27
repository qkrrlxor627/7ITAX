package com.ssafy.tax7i.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ssafy.finance")
public record SsafyFinanceProperties(
        String baseUrl,
        String apiKey,
        String institutionCode,
        String fintechAppNo,
        String userKey
) {
}
