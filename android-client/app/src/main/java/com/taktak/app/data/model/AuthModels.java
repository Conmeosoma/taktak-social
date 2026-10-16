package com.taktak.app.data.model;

public final class AuthModels {
    private AuthModels() { }

    public static final class LoginRequest {
        private final String email;
        private final String password;
        public LoginRequest(String email, String password) { this.email = email; this.password = password; }
        @Override public String toString() { return "LoginRequest[redacted]"; }
    }

    public static final class RegisterRequest {
        private final String email;
        private final String username;
        private final String displayName;
        private final String password;
        public RegisterRequest(String email, String username, String displayName, String password) {
            this.email = email; this.username = username; this.displayName = displayName; this.password = password;
        }
        @Override public String toString() { return "RegisterRequest[redacted]"; }
    }

    public static final class RefreshRequest {
        private final String refreshToken;
        public RefreshRequest(String refreshToken) { this.refreshToken = refreshToken; }
        @Override public String toString() { return "RefreshRequest[redacted]"; }
    }

    public static final class TokenResponse {
        public String accessToken;
        public String refreshToken;
        public long expiresIn;
        public String tokenType;
        public boolean isValid() {
            return accessToken != null && accessToken.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")
                    && accessToken.length() <= 4096 && refreshToken != null && refreshToken.matches("[A-Za-z0-9_-]{43}")
                    && "Bearer".equals(tokenType) && expiresIn >= 1 && expiresIn <= 3600;
        }
        @Override public String toString() { return "TokenResponse[redacted]"; }
    }

    public static final class UserProfile {
        public String id;
        public String email;
        public String username;
        public String displayName;
        public String createdAt;
        public boolean isValid() {
            return id != null && id.matches("[0-9a-fA-F-]{36}") && email != null && username != null
                    && displayName != null && createdAt != null;
        }
        @Override public String toString() { return "UserProfile[redacted]"; }
    }

    public static final class ApiError {
        public String timestamp;
        public int status;
        public String code;
        public String message;
        public String path;
        @Override public String toString() { return "ApiError[redacted]"; }
    }
}
