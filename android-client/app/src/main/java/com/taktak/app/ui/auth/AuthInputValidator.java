package com.taktak.app.ui.auth;

import android.util.Patterns;
import com.taktak.app.R;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class AuthInputValidator {
    private AuthInputValidator() { }

    // String.strip() chưa có trên mọi API từ minSdk 26.
    public static String strip(String value) {
        if (value == null) { return ""; }
        int start = 0;
        int end = value.length();
        while (start < end && Character.isWhitespace(value.codePointAt(start))) {
            start += Character.charCount(value.codePointAt(start));
        }
        while (end > start && Character.isWhitespace(value.codePointBefore(end))) {
            end -= Character.charCount(value.codePointBefore(end));
        }
        return value.substring(start, end);
    }

    public static int emailError(String value) {
        String email = strip(value);
        if (email.isEmpty()) { return R.string.auth_required; }
        return email.length() <= 254 && Patterns.EMAIL_ADDRESS.matcher(email).matches() ? 0 : R.string.auth_invalid_email;
    }

    public static int usernameError(String value) {
        String username = strip(value).toLowerCase(Locale.ROOT);
        if (username.isEmpty()) { return R.string.auth_required; }
        return username.length() <= 255 && username.matches("[a-z0-9_]+") ? 0 : R.string.auth_invalid_username;
    }

    public static int displayNameError(String value) {
        String name = strip(value);
        if (name.isEmpty()) { return R.string.auth_required; }
        return name.length() <= 255 ? 0 : R.string.auth_invalid_display_name;
    }

    public static int passwordError(String password) {
        if (strip(password).isEmpty()) { return R.string.auth_required; }
        // Không trim, cắt hoặc đổi mật khẩu trước khi kiểm tra/gửi ở bước 3B.
        return password.length() <= 72 && password.indexOf('\0') < 0 && password.codePointCount(0, password.length()) >= 8
                && password.getBytes(StandardCharsets.UTF_8).length <= 72 ? 0 : R.string.auth_invalid_password;
    }

    public static int confirmationError(String password, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) { return R.string.auth_required; }
        return confirmation.equals(password) ? 0 : R.string.auth_password_mismatch;
    }
}
