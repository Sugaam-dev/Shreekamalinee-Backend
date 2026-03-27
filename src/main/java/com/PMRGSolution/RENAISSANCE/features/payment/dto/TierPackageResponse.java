package com.PMRGSolution.RENAISSANCE.features.payment.dto;

import java.util.UUID;

import com.PMRGSolution.RENAISSANCE.Constant.TierType;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class TierPackageResponse {
	private UUID packageId;
    private TierType tierType;
    private Double price;
    private Integer durationMonths;
}
