package com.taktak.server.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class RecoveryRequests {
    private RecoveryRequests() { }
    public record Email(@NotBlank @jakarta.validation.constraints.Email @Size(max = 254) String email) {
        public Email { email = normalize(email); }
        @Override public String toString() { return "Email[redacted]"; }
    }
    public record Verify(@NotBlank @jakarta.validation.constraints.Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}")
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String code) {
        public Verify { email = normalize(email); }
        @JsonIgnore @AssertTrue public boolean isPasswordSupported() {
            return new LoginRequest(email,password).isPasswordSupported();
        }
        @Override public String toString() { return "Verify[redacted]"; }
    }
    public record Reset(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}")
            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String resetToken,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String temporaryPassword,
            @NotBlank @Size(max = 72) @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String newPassword,
            @NotBlank @Size(max = 72) @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String confirmation) {
        @JsonIgnore @AssertTrue public boolean isPasswordSupported() {
            return newPassword == null || (newPassword.codePointCount(0, newPassword.length()) >= 8
                    && newPassword.getBytes(StandardCharsets.UTF_8).length <= 72
                    && newPassword.indexOf('\0') < 0 && newPassword.equals(confirmation));
        }
        @Override public String toString() { return "Reset[redacted]"; }
    }
    public record Notice(String message) { }
    public record ResetSession(boolean resetRequired, String resetToken, long expiresIn) {
        @Override public String toString() { return "ResetSession[redacted]"; }
    }
    private static String normalize(String value) { return value == null ? null : value.strip().toLowerCase(Locale.ROOT); }
}
