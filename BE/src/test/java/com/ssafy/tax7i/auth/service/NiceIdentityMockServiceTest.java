package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.auth.service.NiceIdentityMockService.VerificationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NiceIdentityMockServiceTest {

    private final NiceIdentityMockService service = new NiceIdentityMockService();

    @Test
    void verify_결정론적_CI_DI_생성() {
        IdentityVerifyRequest request = new IdentityVerifyRequest("홍길동", "19900101", "M", "01012345678");

        VerificationResult result1 = service.verify(request);
        VerificationResult result2 = service.verify(request);

        assertThat(result1.ci()).isEqualTo(result2.ci());
        assertThat(result1.di()).isEqualTo(result2.di());
        assertThat(result1.ci()).isNotEqualTo(result1.di());
    }

    @Test
    void verify_다른입력_다른CI() {
        IdentityVerifyRequest req1 = new IdentityVerifyRequest("홍길동", "19900101", "M", "01012345678");
        IdentityVerifyRequest req2 = new IdentityVerifyRequest("김철수", "19900101", "M", "01012345678");

        VerificationResult result1 = service.verify(req1);
        VerificationResult result2 = service.verify(req2);

        assertThat(result1.ci()).isNotEqualTo(result2.ci());
    }

    @Test
    void verify_phoneLast4_추출() {
        IdentityVerifyRequest request = new IdentityVerifyRequest("홍길동", "19900101", "M", "01012345678");

        VerificationResult result = service.verify(request);

        assertThat(result.phoneLast4()).isEqualTo("5678");
    }

    @Test
    void verify_필드값_정확히_전달() {
        IdentityVerifyRequest request = new IdentityVerifyRequest("홍길동", "19900101", "M", "01012345678");

        VerificationResult result = service.verify(request);

        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.birthDate()).isEqualTo("19900101");
        assertThat(result.gender()).isEqualTo("M");
        assertThat(result.phoneNumber()).isEqualTo("01012345678");
    }
}
