package com.taktak.server.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(UUID id, String email, String username, String displayName, Instant createdAt) {
    static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getUsername(),
                user.getDisplayName(), user.getCreatedAt());
    }
}
