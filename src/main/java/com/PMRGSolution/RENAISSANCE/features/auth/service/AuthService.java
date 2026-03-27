package com.PMRGSolution.RENAISSANCE.features.auth.service;

// Removed AuthResponse import as we now return raw JWT Strings
import com.PMRGSolution.RENAISSANCE.features.auth.dto.CompleteProfileRequest;

import com.PMRGSolution.RENAISSANCE.features.auth.dto.GoogleLoginRequest;
import com.PMRGSolution.RENAISSANCE.features.auth.dto.LoginRequest;
import com.PMRGSolution.RENAISSANCE.features.auth.dto.OtpVerificationRequest;
import com.PMRGSolution.RENAISSANCE.features.auth.dto.RegistrationRequest;
import com.PMRGSolution.RENAISSANCE.features.auth.dto.ResetPasswordRequest;


import com.PMRGSolution.RENAISSANCE.features.auth.dto.*;

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
    void logout(String sessionId);
}