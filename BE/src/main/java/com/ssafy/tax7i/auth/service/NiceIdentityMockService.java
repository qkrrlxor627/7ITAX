package com.ssafy.tax7i.auth.service;

import com.ssafy.tax7i.auth.dto.IdentityVerifyRequest;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class NiceIdentityMockService {

    public record VerificationResult(
            String ci,
            String di,
            String name,
            String birthDate,
            String gender,
            String phoneNumber,
            String phoneLast4
    ) {}

    public VerificationResult verify(IdentityVerifyRequest request) {
        String raw = request.name() + request.birthDate() + request.phoneNumber();
        String ci = sha256(raw);
        String di = sha256("tax7i:" + raw);
        String phoneLast4 = request.phoneNumber().substring(request.phoneNumber().length() - 4);

        return new VerificationResult(
                ci, di,
                request.name(),
                request.birthDate(),
                request.gender(),
                request.phoneNumber(),
                phoneLast4
        );
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "SHA-256 not available");
        }
    }
}
