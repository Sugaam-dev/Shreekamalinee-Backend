package com.PMRGSolution.RENAISSANCE.features.payment.dto;



import lombok.Data;
import java.util.UUID;

@Data
public class OrderRequest {
    private UUID packageId; // The MASTER ID for the purchase
    private String couponCode;
}