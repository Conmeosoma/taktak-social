package com.taktak.server.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(@NotBlank @Size(max = 255) String displayName) {
    public UpdateProfileRequest {
        displayName = displayName == null ? null : displayName.strip();
    }
}
