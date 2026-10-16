package com.taktak.server.auth;

public class LoginThrottled extends RuntimeException {
    public LoginThrottled() { super("Quá nhiều yêu cầu đăng nhập. Vui lòng thử lại sau."); }
}
