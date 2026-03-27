package com.PMRGSolution.RENAISSANCE.features.auth.service;

import com.PMRGSolution.RENAISSANCE.features.auth.dto.UserAccountDTO;
import java.util.UUID;

public interface ProfileService {
    UserAccountDTO getUserAccountDetails(UUID userId);
    void updateProfile(UUID userId, UserAccountDTO updateRequest);
}