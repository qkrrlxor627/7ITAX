package com.ssafy.tax7i.auth.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PinServiceTest {

    private final PinService pinService = new PinService();

    @Test
    void hashPin_해시생성() {
        String hash = pinService.hashPin("123456");
        assertThat(hash).isNotNull();
        assertThat(hash).isNotEqualTo("123456");
        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void verifyPin_올바른PIN_true() {
        String hash = pinService.hashPin("123456");
        assertThat(pinService.verifyPin("123456", hash)).isTrue();
    }

    @Test
    void verifyPin_잘못된PIN_false() {
        String hash = pinService.hashPin("123456");
        assertThat(pinService.verifyPin("654321", hash)).isFalse();
    }

    @Test
    void hashPin_동일PIN_다른해시() {
        String hash1 = pinService.hashPin("123456");
        String hash2 = pinService.hashPin("123456");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
