package com.pmrgsolution.features.auth.service;

import com.pmrgsolution.features.auth.dto.AdminUserDTO;
import java.util.List;
import java.util.UUID;

public interface AdminUserService {
    List<AdminUserDTO> searchUsers(String query);
    AdminUserDTO updateUserStatus(UUID userId, boolean enabled);
    void deleteUser(UUID userId);
}
