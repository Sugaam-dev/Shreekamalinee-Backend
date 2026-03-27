package com.PMRGSolution.RENAISSANCE.features.auth.service;
public interface EmailService {
    void sendOtpEmail(String to, String otp); // For Registration
    void sendPasswordResetEmail(String to, String otp); // For Forgot Password
}