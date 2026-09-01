package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.auth.dto.UserAccountDTO;
import java.util.UUID;

public interface ProfileService {
    UserAccountDTO getUserAccountDetails(UUID userId);
    UserAccountDTO updateProfile(UUID userId, UserAccountDTO updateRequest);
}