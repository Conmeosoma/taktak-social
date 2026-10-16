package com.taktak.server.user;

import com.taktak.server.auth.AuthFailure;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID currentUserId) {
        return UserProfileResponse.from(currentUser(currentUserId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID currentUserId, UpdateProfileRequest request) {
        User user = currentUser(currentUserId);
        user.updateDisplayName(request.displayName());
        // Entity đang được quản lý; JPA ghi thay đổi khi transaction commit.
        return UserProfileResponse.from(user);
    }

    private User currentUser(UUID currentUserId) {
        return users.findById(currentUserId).orElseThrow(AuthFailure::new);
    }
}
