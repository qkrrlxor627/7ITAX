package com.ssafy.tax7i.sms.service;

public interface SmsSender {
    void send(String phoneNumber, String otpCode);
    String generateOtp();
    String maskPhone(String phoneNumber);
}
