package com.taktak.server.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
        @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {
    public LoginRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @JsonIgnore
    @AssertTrue
    public boolean isPasswordSupported() {
        return password == null || (password.length() <= 72
                && password.codePointCount(0, password.length()) >= 8
                && password.getBytes(StandardCharsets.UTF_8).length <= 72 && password.indexOf('\0') < 0);
    }

    @Override
    public String toString() { return "LoginRequest[redacted]"; }
}
