package com.pmrgsolution.features.order.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingDetailsUpdateRequest {
    private String trackingNumber;
    private String courierPartner;
    private String trackingUrl;
    private String estimatedDeliveryDate;
}