package com.ssafy.tax7i.banking.client;

import com.ssafy.tax7i.config.SsafyFinanceProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@ActiveProfiles("integration")
@SpringBootTest(
        classes = SsafyFinanceClientIntegrationTest.Config.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
class SsafyFinanceClientIntegrationTest {

    @EnableConfigurationProperties(SsafyFinanceProperties.class)
    static class Config {
    }

    @Autowired
    private SsafyFinanceProperties properties;

    @Test
    void apiKey_isValid_memberSearchReturnsNon401Response() {
        // given
        assertThat(properties.apiKey())
                .as("실제 API 키가 로드되어야 합니다")
                .isNotEqualTo("test-finance-api-key")
                .isNotBlank();

        RestTemplate rt = new RestTemplate();
        rt.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatusCode statusCode) {
                return false; // 모든 응답을 에러 없이 처리
            }
        });

        String url = "https://finopenapi.ssafy.io/ssafy/api/v1/edu/member/search";
        String body = "{\"apiKey\":\"" + properties.apiKey() + "\",\"userId\":\"api-key-validation-test\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        // when
        ResponseEntity<String> response = rt.exchange(url, HttpMethod.POST, entity, String.class);

        // then — 키가 무효하면 401/403, 유효하면 그 외 상태 코드
        int statusCode = response.getStatusCode().value();
        assertThat(statusCode)
                .as("API 키가 유효하면 401/403이 아닌 응답이 와야 합니다 (실제: %d)", statusCode)
                .isNotIn(401, 403);
        assertThat(response.getBody()).isNotNull();
    }
}
