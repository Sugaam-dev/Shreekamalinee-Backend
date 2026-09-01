package com.pmrgsolution.features.auth.service;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

@Service
public class TotpServiceImpl implements TotpService {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    @Override
    public String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(BASE32_CHARS.charAt(random.nextInt(BASE32_CHARS.length())));
        }
        return sb.toString();
    }

    @Override
    public String getQrCodeUri(String secretKey, String email) {
        String issuer = "Shreekamalinee";
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, email, secretKey, issuer);
    }

    @Override
    public boolean verifyCode(String secretKey, int code) {
        byte[] keyBytes = decodeBase32(secretKey);
        long currentTimeMillis = System.currentTimeMillis();
        long currentWindow = currentTimeMillis / 1000L / 30L; // 30-second window

        // Allow clock drift of ±1 window
        for (int i = -1; i <= 1; i++) {
            if (getCodeForWindow(keyBytes, currentWindow + i) == code) {
                return true;
            }
        }
        return false;
    }

    private int getCodeForWindow(byte[] key, long timeWindow) {
        byte[] data = new byte[8];
        long temp = timeWindow;
        for (int i = 8; i-- > 0; temp >>>= 8) {
            data[i] = (byte) temp;
        }

        try {
            SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signKey);
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            long truncatedHash = 0;
            for (int i = 0; i < 4; ++i) {
                truncatedHash <<= 8;
                truncatedHash |= (hash[offset + i] & 0xFF);
            }
            truncatedHash &= 0x7FFFFFFF;
            truncatedHash %= 1000000;
            return (int) truncatedHash;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to calculate HMAC for TOTP", e);
        }
    }

    private byte[] decodeBase32(String base32) {
        String cleaned = base32.toUpperCase().replace("-", "").replace(" ", "");
        int len = cleaned.length();
        byte[] bytes = new byte[len * 5 / 8];
        int bitBuffer = 0;
        int bitLength = 0;
        int bytesIndex = 0;

        for (int i = 0; i < len; i++) {
            char c = cleaned.charAt(i);
            int val = BASE32_CHARS.indexOf(c);
            if (val == -1) continue; // Skip padding or invalid characters
            bitBuffer = (bitBuffer << 5) | val;
            bitLength += 5;
            if (bitLength >= 8) {
                bytes[bytesIndex++] = (byte) ((bitBuffer >> (bitLength - 8)) & 0xFF);
                bitLength -= 8;
            }
        }
        return Arrays.copyOf(bytes, bytesIndex);
    }
}
