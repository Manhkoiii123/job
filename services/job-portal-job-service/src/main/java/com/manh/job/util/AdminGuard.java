package com.manh.job.util;

import com.manh.job.client.UserClient;
import com.manh.job.domain.UserRole;
import com.manh.job.domain.UserStatus;
import com.manh.job.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminGuard {

    private final UserClient userClient;

    public void assertAdmin(Long userId) throws Exception {
        if (userId == null) {
            throw new Exception("Admin access required");
        }
        try {
            UserResponse user = userClient.getUserById(userId);
            if (user == null
                    || user.getRole() != UserRole.ROLE_ADMIN
                    || user.getStatus() != UserStatus.ACTIVE) {
                throw new Exception("Admin access required");
            }
        } catch (Exception ex) {
            throw new Exception("Admin access required");
        }
    }
}
