package com.taktak.server.auth;

import com.taktak.server.user.User;
import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(UUID id, String email, String username, String displayName, Instant createdAt) {
    public static RegisterResponse from(User user) {
        return new RegisterResponse(user.getId(), user.getEmail(), user.getUsername(),
                user.getDisplayName(), user.getCreatedAt());
    }
}
