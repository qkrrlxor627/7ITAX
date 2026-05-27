package com.ssafy.tax7i.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    @Qualifier("aiRestTemplate")
    public RestTemplate aiRestTemplate(RestTemplateBuilder builder, AiServiceProperties aiProperties) {
        return builder
                .connectTimeout(Duration.ofSeconds(aiProperties.connectTimeout()))
                .readTimeout(Duration.ofSeconds(aiProperties.readTimeout()))
                .build();
    }
}
