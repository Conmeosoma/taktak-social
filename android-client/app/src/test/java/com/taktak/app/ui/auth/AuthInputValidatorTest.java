package com.taktak.app.ui.auth;

import com.taktak.app.R;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuthInputValidatorTest {
    private String repeat(String text, int count) {
        StringBuilder value = new StringBuilder();
        for (int index = 0; index < count; index++) { value.append(text); }
        return value.toString();
    }

    @Test public void stripHandlesUnicodeWithoutNewerAndroidApis() {
        assertEquals("Tiến 📷", AuthInputValidator.strip(" \u2003Tiến 📷\t\n"));
        assertEquals("", AuthInputValidator.strip(null));
    }

    @Test public void usernameAllowsServerNormalizationButRejectsInvalidCharacters() {
        assertEquals(0, AuthInputValidator.usernameError("  Nam_Tien09  "));
        assertEquals(R.string.auth_invalid_username, AuthInputValidator.usernameError("nam.tien"));
        assertEquals(R.string.auth_invalid_username, AuthInputValidator.usernameError("tiến"));
        assertEquals(R.string.auth_required, AuthInputValidator.usernameError("\u2003"));
    }

    @Test public void usernameLengthMatchesServer() {
        assertEquals(0, AuthInputValidator.usernameError(repeat("a", 255)));
        assertEquals(R.string.auth_invalid_username, AuthInputValidator.usernameError(repeat("a", 256)));
    }

    @Test public void displayNameAllowsUnicodeAndChecksUtf16Length() {
        assertEquals(0, AuthInputValidator.displayNameError("  Tiến 📷  "));
        assertEquals(0, AuthInputValidator.displayNameError(repeat("📷", 127) + "a"));
        assertEquals(R.string.auth_invalid_display_name, AuthInputValidator.displayNameError(repeat("📷", 128)));
        assertEquals(R.string.auth_required, AuthInputValidator.displayNameError("  \t"));
    }

    @Test public void passwordChecksCodePointsAndUtf8ByteLimit() {
        assertEquals(0, AuthInputValidator.passwordError(repeat("a", 72)));
        assertEquals(0, AuthInputValidator.passwordError(repeat("📷", 18)));
        assertEquals(R.string.auth_invalid_password, AuthInputValidator.passwordError(repeat("a", 73)));
        assertEquals(R.string.auth_invalid_password, AuthInputValidator.passwordError(repeat("📷", 19)));
        assertEquals(R.string.auth_invalid_password, AuthInputValidator.passwordError(repeat("📷", 7)));
    }

    @Test public void passwordIsNotTrimmedOrSilentlyTruncated() {
        assertEquals(0, AuthInputValidator.passwordError(" " + repeat("a", 6) + " "));
        assertEquals(R.string.auth_required, AuthInputValidator.passwordError(repeat(" ", 8)));
        assertEquals(R.string.auth_invalid_password, AuthInputValidator.passwordError(repeat("a", 8) + '\0'));
        assertEquals(R.string.auth_required, AuthInputValidator.passwordError(null));
    }

    @Test public void usernameUnderscoreAndLengthRules() {
        assertEquals(R.string.auth_username_too_short, AuthInputValidator.usernameError("ab"));
        assertEquals(0, AuthInputValidator.usernameError("abc"));
        assertEquals(0, AuthInputValidator.usernameError("a_b"));
        assertEquals(R.string.auth_username_underscore_edge, AuthInputValidator.usernameError("_abc"));
        assertEquals(R.string.auth_username_underscore_edge, AuthInputValidator.usernameError("abc_"));
        assertEquals(R.string.auth_username_double_underscore, AuthInputValidator.usernameError("a__bc"));
        assertEquals(0, AuthInputValidator.usernameError("a_b_c"));
    }

    @Test public void passwordStrengthEvaluatesComplexity() {
        assertEquals(AuthInputValidator.PasswordStrength.NONE, AuthInputValidator.calculatePasswordStrength(""));
        assertEquals(AuthInputValidator.PasswordStrength.NONE, AuthInputValidator.calculatePasswordStrength(null));
        assertEquals(AuthInputValidator.PasswordStrength.WEAK, AuthInputValidator.calculatePasswordStrength("1234567"));
        assertEquals(AuthInputValidator.PasswordStrength.WEAK, AuthInputValidator.calculatePasswordStrength("abcdefgh"));
        assertEquals(AuthInputValidator.PasswordStrength.MEDIUM, AuthInputValidator.calculatePasswordStrength("Password12"));
        assertEquals(AuthInputValidator.PasswordStrength.STRONG, AuthInputValidator.calculatePasswordStrength("Password12!@#"));
        assertEquals(AuthInputValidator.PasswordStrength.STRONG, AuthInputValidator.calculatePasswordStrength("LongPassword123"));
    }

    @Test public void confirmationMustMatchIncludingWhitespace() {
        String fixture = java.util.UUID.randomUUID().toString();
        assertEquals(0, AuthInputValidator.confirmationError(fixture, fixture));
        assertEquals(R.string.auth_password_mismatch, AuthInputValidator.confirmationError(fixture, fixture + " "));
        assertEquals(R.string.auth_required, AuthInputValidator.confirmationError(fixture, ""));
    }
}
