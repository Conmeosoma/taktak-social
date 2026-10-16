package com.taktak.server.auth;

/** Không đưa email, password hoặc token vào exception. */
public class AuthFailure extends RuntimeException {
    public AuthFailure() {
        super("Thông tin xác thực không hợp lệ hoặc đã hết hạn.");
    }
}
