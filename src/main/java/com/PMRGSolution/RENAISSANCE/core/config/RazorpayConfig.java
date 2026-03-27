package com.PMRGSolution.RENAISSANCE.core.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    /**
     * Creates the RazorpayClient bean.
     * This client will be used by PaymentServiceImpl to create orders 
     * and verify payment signatures.
     */
    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        // Validation to ensure keys are present
        if (keyId == null || keySecret == null) {
            throw new RuntimeException("Razorpay API keys are missing in the configuration.");
        }
        return new RazorpayClient(keyId, keySecret);
    }
}