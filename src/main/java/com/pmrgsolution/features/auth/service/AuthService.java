package com.pmrgsolution.features.auth.service;

// Removed AuthResponse import as we now return raw JWT Strings
import com.pmrgsolution.features.auth.dto.CompleteProfileRequest;

import com.pmrgsolution.features.auth.dto.GoogleLoginRequest;
import com.pmrgsolution.features.auth.dto.LoginRequest;
import com.pmrgsolution.features.auth.dto.OtpVerificationRequest;
import com.pmrgsolution.features.auth.dto.RegistrationRequest;
import com.pmrgsolution.features.auth.dto.ResetPasswordRequest;


import com.pmrgsolution.features.auth.dto.*;

public interface AuthService {
    String register(RegistrationRequest request);
    
    // Now returning AuthResponse objects for immediate frontend sync
    AuthResponse verifyOtp(OtpVerificationRequest request); 
    AuthResponse login(LoginRequest request);               
    AuthResponse authenticateWithGoogle(GoogleLoginRequest request); 
    AuthResponse completeUserProfile(CompleteProfileRequest request); 

    // Essential for the React "checkAuthStatus" recovery logic
    AuthResponse getAuthDetailsByEmail(String email);

    String initiateForgotPassword(String email);
    String resetPassword(ResetPasswordRequest request);
    String setPasswordForGoogleUser(String email, String newPassword);
    TokenRefreshResponse refreshToken(String refreshToken);
    void logout(String sessionId);
}