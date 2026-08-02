package com.ssafy.tax7i.sms.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Slf4j
@Component
@ConditionalOnProperty(name = "sms.mock.enabled", havingValue = "true")
public class MockSmsSender implements SmsSender {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Override
    public void send(String phoneNumber, String otpCode) {
        log.info("[MOCK SMS] 수신: {}, 인증번호: {}", maskPhone(phoneNumber), otpCode);
    }

    @Override
    public String generateOtp() {
        int otp = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", otp);
    }

    @Override
    public String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return "***";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
