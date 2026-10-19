package com.taktak.server.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 3, max = 255) @Pattern(regexp = "^(?!.*__)[a-z0-9][a-z0-9_]{1,253}[a-z0-9]$") String username,
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password) {

    public RegisterRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        username = username == null ? null : username.strip().toLowerCase(Locale.ROOT);
        displayName = displayName == null ? null : displayName.strip();
        // Không trim hoặc chuẩn hóa password.
    }

    @JsonIgnore
    @AssertTrue(message = "Mật khẩu cần ít nhất 8 ký tự Unicode, tối đa 72 byte UTF-8 và không chứa NUL.")
    public boolean isPasswordSupported() {
        return password == null || (password.length() <= 72
                && password.codePointCount(0, password.length()) >= 8
                && password.getBytes(StandardCharsets.UTF_8).length <= 72
                && password.indexOf('\0') < 0);
    }

    @Override
    public String toString() {
        return "RegisterRequest[redacted]";
    }
}
