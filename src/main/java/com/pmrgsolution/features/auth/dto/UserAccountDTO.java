package com.pmrgsolution.features.auth.dto;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountDTO {
    private UUID userId;
    private String firstName; 
    private String lastName;  
    private String email;
    private String phone; // Maps to phone_No in DB
    private boolean isEmailVerified;
}