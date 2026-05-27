package com.ssafy.tax7i.global.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AesEncryptorTest {

    private AesEncryptor aesEncryptor;

    @BeforeEach
    void setUp() {
        String base64Key = java.util.Base64.getEncoder().encodeToString("tax7i-local-aes256-key-32bytes!!".getBytes());
        EncryptionProperties properties = new EncryptionProperties(base64Key);
        aesEncryptor = new AesEncryptor(properties);
    }

    @Test
    void 암호화_복호화_원본복원() {
        String original = "01012345678";
        String encrypted = aesEncryptor.convertToDatabaseColumn(original);
        String decrypted = aesEncryptor.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(original);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void null_입력시_null_반환() {
        assertThat(aesEncryptor.convertToDatabaseColumn(null)).isNull();
        assertThat(aesEncryptor.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void 동일입력_다른암호문_IV랜덤() {
        String original = "홍길동";
        String encrypted1 = aesEncryptor.convertToDatabaseColumn(original);
        String encrypted2 = aesEncryptor.convertToDatabaseColumn(original);

        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // 둘 다 복호화하면 동일
        assertThat(aesEncryptor.convertToEntityAttribute(encrypted1)).isEqualTo(original);
        assertThat(aesEncryptor.convertToEntityAttribute(encrypted2)).isEqualTo(original);
    }

    @Test
    void 한글_영문_특수문자_모두_처리() {
        String[] inputs = {"홍길동", "test@example.com", "010-1234-5678", "abc123!@#"};
        for (String input : inputs) {
            String encrypted = aesEncryptor.convertToDatabaseColumn(input);
            String decrypted = aesEncryptor.convertToEntityAttribute(encrypted);
            assertThat(decrypted).isEqualTo(input);
        }
    }
}
