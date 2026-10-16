package com.taktak.server.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RefreshRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String refreshToken) {
    @Override
    public String toString() { return "RefreshRequest[redacted]"; }
}
