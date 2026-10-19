package com.taktak.app.data.model;

public final class RecoveryModels {
    private RecoveryModels() { }
    public static final class Notice { public String message; }
    public static final class Email {
        private final String email;
        public Email(String email) { this.email=email; }
        @Override public String toString() { return "Email[redacted]"; }
    }
    public static final class Verification {
        private final String email, password, code;
        public Verification(String email, String password, String code) { this.email=email; this.password=password; this.code=code; }
        @Override public String toString() { return "Verification[redacted]"; }
    }
    public static final class Reset {
        private final String resetToken, temporaryPassword, newPassword, confirmation;
        public Reset(String token, String temporary, String password, String confirmation) {
            resetToken=token; temporaryPassword=temporary; newPassword=password; this.confirmation=confirmation;
        }
        @Override public String toString() { return "Reset[redacted]"; }
    }
}
