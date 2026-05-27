package com.ssafy.tax7i.global.crypto;

import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Converter
public class AesEncryptor implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private static volatile SecretKeySpec secretKey;

    @Autowired
    public AesEncryptor(EncryptionProperties properties) {
        byte[] keyBytes = Base64.getDecoder().decode(properties.getAesKey());
        AesEncryptor.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public AesEncryptor() {
        // JPA/Hibernate가 Spring보다 먼저 이 인스턴스를 생성할 수 있음
        // Spring 빈이 아직 키를 설정하지 않은 경우 환경 변수에서 직접 로드
        if (secretKey == null) {
            String envKey = System.getenv("AES_ENCRYPTION_KEY");
            if (envKey == null) {
                envKey = System.getProperty("encryption.aes-key");
            }
            if (envKey == null) {
                envKey = System.getProperty("AES_ENCRYPTION_KEY");
            }
            if (envKey != null && !envKey.isBlank()) {
                byte[] keyBytes = Base64.getDecoder().decode(envKey);
                AesEncryptor.secretKey = new SecretKeySpec(keyBytes, "AES");
            }
        }
    }

    private void ensureKeyAvailable() {
        if (secretKey == null) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "암호화 키가 초기화되지 않았습니다.");
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        ensureKeyAvailable();
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes());

            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "암호화에 실패했습니다.");
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        ensureKeyAvailable();
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);

            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "복호화에 실패했습니다.");
        }
    }
}
