package com.taktak.server.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class RecoveryController {
    private final RecoveryService recovery;
    private final RecoveryDispatcher dispatcher;
    private final LoginRateLimiter limiter;
    public RecoveryController(RecoveryService recovery, RecoveryDispatcher dispatcher, Clock clock) {
        this.recovery = recovery; this.dispatcher = dispatcher; limiter = new LoginRateLimiter(clock);
    }
    @PostMapping(value = "/verification/request", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> requestVerification(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        limiter.check(http.getRemoteAddr(), request.email()); recovery.requestVerification(request);
        return notice("Nếu cần xác minh, mã đã được gửi tới email đăng ký. Mã có hiệu lực 15 phút.");
    }
    @PostMapping(value = "/verification/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> verify(@Valid @RequestBody RecoveryRequests.Verify request, HttpServletRequest http) {
        limiter.check(http.getRemoteAddr(), request.email()); recovery.confirmVerification(request);
        return notice("Email đã được xác minh. Bạn có thể dùng tính năng khôi phục mật khẩu.");
    }
    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgot(@Valid @RequestBody RecoveryRequests.Email request, HttpServletRequest http) {
        limiter.check(http.getRemoteAddr(), request.email()); dispatcher.request(request.email());
        return notice(RecoveryService.GENERIC);
    }
    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reset(@Valid @RequestBody RecoveryRequests.Reset request, HttpServletRequest http) {
        limiter.check(http.getRemoteAddr(), "reset:" + TokenService.hash(request.resetToken())); recovery.reset(request);
        return notice("Mật khẩu đã thay đổi. Vui lòng đăng nhập lại.");
    }
    private ResponseEntity<?> notice(String message) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
                .body(new RecoveryRequests.Notice(message));
    }
}
