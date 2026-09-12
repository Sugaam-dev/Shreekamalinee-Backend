package com.pmrgsolution.features.address.dto;

import com.pmrgsolution.constant.AddressType;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    private UUID id;
    private String fullName;
    private String phoneNumber;
    private String alternatePhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private AddressType addressType;
    private boolean isDefault;
    private LocalDateTime createdAt;
}