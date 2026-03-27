package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor // Required for JSON deserialization
@AllArgsConstructor // Fixes the "Constructor is undefined" error
@Builder // Allows for cleaner object creation
public class TierPackageRequest {
    private UUID categoryId;
    private TierType tierType;
    private double price;
    private int durationMonths;
}