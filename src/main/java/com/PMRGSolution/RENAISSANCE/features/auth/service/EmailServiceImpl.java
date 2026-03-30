package com.PMRGSolution.RENAISSANCE.features.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verify your Renaissance Account");
        message.setText("Welcome to Renaissance Digital Learning Hub!\n\n" +
                        "Your 6-digit OTP for account verification is: " + otp + 
                        "\nThis code expires in 10 minutes.");
        
        mailSender.send(message);
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset your Renaissance Password");
        message.setText("We received a request to reset your password for your Renaissance account.\n\n" +
                        "Your 6-digit password reset OTP is: " + otp + 
                        "\nIf you did not request this, please ignore this email and secure your account." +
                        "\nThis code expires in 10 minutes.");
        
        mailSender.send(message);
    }
}