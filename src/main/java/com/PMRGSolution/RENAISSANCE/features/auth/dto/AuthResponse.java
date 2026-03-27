package com.PMRGSolution.RENAISSANCE.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String email; 
    private String firstName;
    private String lastName;
    private String role; 
    private String phoneNumber;
    private boolean isProfileComplete;
    
    // The "Access Bridge" fields
    private int tierRank;    // 0=Free, 1=Starter, 2=Standard, 3=Pro
    private String tierName; // For the UI Badge (e.g., "Professional")
    
    // We keep this hidden from the JSON response usually, 
    // but the Controller needs it to set the Cookie.
    private String token; 
}