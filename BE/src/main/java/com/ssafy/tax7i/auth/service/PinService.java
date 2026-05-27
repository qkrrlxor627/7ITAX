package com.ssafy.tax7i.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hashPin(String pin) {
        return encoder.encode(pin);
    }

    public boolean verifyPin(String rawPin, String hashedPin) {
        return encoder.matches(rawPin, hashedPin);
    }
}
