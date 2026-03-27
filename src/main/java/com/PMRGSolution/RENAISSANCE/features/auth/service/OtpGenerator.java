package com.PMRGSolution.RENAISSANCE.features.auth.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class OtpGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();

    public String generateOtp() {
        // Generates a 6-digit cryptographically secure OTP
        return String.format("%06d", secureRandom.nextInt(1000000));
    }
}