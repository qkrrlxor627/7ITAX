package com.ssafy.tax7i.sms.service;

import com.solapi.sdk.SolapiClient;
import com.solapi.sdk.message.model.Message;
import com.solapi.sdk.message.service.DefaultMessageService;
import com.ssafy.tax7i.config.SolapiProperties;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolapiSmsSender implements SmsSender {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[0-9]{8,9}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^[0-9]{6}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SolapiProperties solapiProperties;
    private DefaultMessageService messageService;

    @PostConstruct
    void init() {
        this.messageService = SolapiClient.INSTANCE.createInstance(
                solapiProperties.apiKey(),
                solapiProperties.apiSecret()
        );
    }

    @Override
    public void send(String phoneNumber, String otpCode) {
        validatePayload(phoneNumber, otpCode);

        Message message = new Message();
        message.setFrom(solapiProperties.senderPhone());
        message.setTo(phoneNumber);
        message.setText("[7ITAX] 인증번호: " + otpCode);

        try {
            messageService.send(message);
            log.info("SMS 발송 성공: {}", maskPhone(phoneNumber));
        } catch (Exception e) {
            log.error("SMS 발송 실패: {}", maskPhone(phoneNumber), e);
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "SMS 발송에 실패했습니다.");
        }
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

    private void validatePayload(String phoneNumber, String otpCode) {
        if (phoneNumber == null || !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new BusinessException(ErrorCode.INVALID_FORMAT, "유효하지 않은 전화번호 형식입니다.");
        }
        if (otpCode == null || !OTP_PATTERN.matcher(otpCode).matches()) {
            throw new BusinessException(ErrorCode.INVALID_FORMAT, "유효하지 않은 인증번호 형식입니다.");
        }
    }
}
