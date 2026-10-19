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
        if (username.length() < 3) { return R.string.auth_username_too_short; }
        if (username.length() > 255) { return R.string.auth_invalid_username; }
        if (username.startsWith("_") || username.endsWith("_")) { return R.string.auth_username_underscore_edge; }
        if (username.contains("__")) { return R.string.auth_username_double_underscore; }
        if (!username.matches("^[a-z0-9_]+$")) { return R.string.auth_invalid_username; }
        return 0;
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

    public enum PasswordStrength {
        NONE(0, 0, 0),
        WEAK(1, 33, R.string.password_strength_weak),
        MEDIUM(2, 66, R.string.password_strength_medium),
        STRONG(3, 100, R.string.password_strength_strong);

        public final int level;
        public final int percent;
        public final int labelRes;

        PasswordStrength(int level, int percent, int labelRes) {
            this.level = level;
            this.percent = percent;
            this.labelRes = labelRes;
        }
    }

    public static PasswordStrength calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.NONE;
        }
        int length = password.length();
        int codePoints = password.codePointCount(0, length);
        if (codePoints < 8) {
            return PasswordStrength.WEAK;
        }
        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (int i = 0; i < length; i++) {
            char c = password.charAt(i);
            if (Character.isLowerCase(c)) { hasLower = true; }
            else if (Character.isUpperCase(c)) { hasUpper = true; }
            else if (Character.isDigit(c)) { hasDigit = true; }
            else { hasSpecial = true; }
        }
        int varieties = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSpecial ? 1 : 0);
        if (codePoints >= 12 && varieties >= 3) {
            return PasswordStrength.STRONG;
        }
        if (codePoints >= 8 && varieties >= 4) {
            return PasswordStrength.STRONG;
        }
        if (codePoints >= 8 && varieties >= 2) {
            return PasswordStrength.MEDIUM;
        }
        return PasswordStrength.WEAK;
    }
}
