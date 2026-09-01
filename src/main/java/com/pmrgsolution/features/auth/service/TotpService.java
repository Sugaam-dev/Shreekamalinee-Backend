package com.pmrgsolution.features.auth.service;

public interface TotpService {
    String generateSecretKey();
    String getQrCodeUri(String secretKey, String email);
    boolean verifyCode(String secretKey, int code);
}
