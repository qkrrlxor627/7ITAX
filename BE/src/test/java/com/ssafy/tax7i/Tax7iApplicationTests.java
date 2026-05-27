package com.ssafy.tax7i;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class Tax7iApplicationTests {

	@BeforeAll
	static void setup() {
		// JPA/Hibernate가 Spring보다 먼저 AesEncryptor를 초기화할 수 있으므로
		// System property로 AES 키를 미리 설정
		System.setProperty("encryption.aes-key", "dGVzdC1hZXMtZW5jcnlwdGlvbi1rZXktMzItYnl0ZXM=");
	}

	@Test
	void contextLoads() {
	}

}
