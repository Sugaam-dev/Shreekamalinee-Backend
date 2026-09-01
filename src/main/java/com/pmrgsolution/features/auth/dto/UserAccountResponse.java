package com.pmrgsolution.features.auth.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountResponse {
    private UUID userId;
    private String firstName; 
    private String lastName;  
    private String email;
    private String phone;
    private boolean isEmailVerified;
}