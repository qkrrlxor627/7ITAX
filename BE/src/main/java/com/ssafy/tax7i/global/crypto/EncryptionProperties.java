package com.ssafy.tax7i.global.crypto;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties {

    private final String aesKey;

    public EncryptionProperties(String aesKey) {
        if (aesKey == null || aesKey.isBlank()) {
            throw new IllegalStateException("encryption.aes-key must be configured");
        }
        this.aesKey = aesKey;
    }
}
