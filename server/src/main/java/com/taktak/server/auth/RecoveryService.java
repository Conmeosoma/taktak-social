package com.taktak.server.auth;

import com.taktak.server.user.User;
import com.taktak.server.user.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class RecoveryService {
    public static final String GENERIC = "Nếu email này đã được đăng ký, chúng tôi sẽ gửi mật khẩu tạm thời đến hộp thư của bạn.";
    private final UserRepository users;
    private final RecoveryCredentialsRepository credentials;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final AccountMail mail;
    private final Clock clock;
    private final String dummyHash;
    private final SecureRandom random = new SecureRandom();

    public RecoveryService(UserRepository users, RecoveryCredentialsRepository credentials,
            RefreshTokenRepository refreshTokens, PasswordEncoder passwords, AccountMail mail, Clock clock) {
        this.users = users; this.credentials = credentials; this.refreshTokens = refreshTokens;
        this.passwords = passwords; this.mail = mail; this.clock = clock;
        dummyHash = passwords.encode(secret());
    }
    private String secret() {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private User locked(String email) {
        return users.findIdByEmail(email).flatMap(users::findLockedById).orElse(null);
    }
    private RecoveryCredentials record(User user) {
        return credentials.findById(user.getId()).orElseGet(() -> new RecoveryCredentials(user.getId()));
    }
    private User authenticated(LoginRequest request) {
        User user = locked(request.email());
        boolean valid = passwords.matches(request.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !valid) { throw new AuthFailure(); }
        return user;
    }
    @Transactional
    public void requestVerification(LoginRequest request) {
        User user = authenticated(request);
        if (user.isEmailVerified()) { return; }
        var stored = record(user); String code = secret();
        stored.verificationHash = TokenService.hash(code); stored.verificationExpiresAt = clock.instant().plusSeconds(900);
        credentials.saveAndFlush(stored);
        mail.send(user.getEmail(), "Taktak — Xác minh email", "Mã xác minh (15 phút): " + code
                + "\nChỉ nhập mã trong ứng dụng Taktak. Nếu không yêu cầu, hãy bỏ qua thư.");
    }
    @Transactional
    public void confirmVerification(RecoveryRequests.Verify request) {
        User user = authenticated(new LoginRequest(request.email(), request.password()));
        var stored = record(user);
        if (stored.verificationHash == null || !stored.verificationExpiresAt.isAfter(clock.instant())
                || !constantEquals(stored.verificationHash, TokenService.hash(request.code()))) { throw new AuthFailure(); }
        user.verifyEmail(clock.instant()); stored.verificationHash = null; stored.verificationExpiresAt = null;
        credentials.save(stored);
    }
    @Transactional
    public void issueTemporary(String email) {
        User user = locked(email);
        if (user == null || !user.isEmailVerified()) { return; }
        var stored = record(user); String temporary = secret();
        stored.clearRecovery(); stored.temporaryHash = passwords.encode(temporary);
        stored.temporaryExpiresAt = clock.instant().plusSeconds(900); credentials.saveAndFlush(stored);
        // Nếu SMTP lỗi, rollback hash mới; không đổi mật khẩu thật hoặc khóa tài khoản.
        mail.send(user.getEmail(), "Taktak — Mật khẩu tạm thời", "Mật khẩu tạm thời (15 phút, một lần): "
                + temporary + "\nĐăng nhập bằng mật khẩu này để đổi mật khẩu. Không chia sẻ với người khác.");
    }
    @Transactional
    public RecoveryRequests.ResetSession loginTemporary(User user, String temporary) {
        var stored = record(user);
        if (!user.isEmailVerified() || stored.temporaryHash == null || stored.resetHash != null
                || !stored.temporaryExpiresAt.isAfter(clock.instant())
                || !passwords.matches(temporary, stored.temporaryHash)) { throw new AuthFailure(); }
        String token = secret(); stored.resetHash = TokenService.hash(token);
        stored.resetExpiresAt = stored.temporaryExpiresAt; credentials.saveAndFlush(stored);
        return new RecoveryRequests.ResetSession(true, token,
                java.time.Duration.between(clock.instant(), stored.resetExpiresAt).toSeconds());
    }
    @Transactional
    public void reset(RecoveryRequests.Reset request) {
        var userId = credentials.findUserIdByResetHash(TokenService.hash(request.resetToken())).orElseThrow(AuthFailure::new);
        User user = users.findLockedById(userId).orElseThrow(AuthFailure::new);
        // Query đầu chỉ xác định user; refresh entity sau khi chờ khóa để không đọc trạng thái cũ.
        var stored = credentials.findById(user.getId()).orElseThrow(AuthFailure::new);
        if (stored.resetHash == null || !constantEquals(stored.resetHash, TokenService.hash(request.resetToken()))
                || !stored.resetExpiresAt.isAfter(clock.instant()) || stored.temporaryHash == null
                || !passwords.matches(request.temporaryPassword(), stored.temporaryHash)) { throw new AuthFailure(); }
        if (!request.isPasswordSupported() || passwords.matches(request.newPassword(), user.getPasswordHash())
                || passwords.matches(request.newPassword(), stored.temporaryHash)) { throw new RecoveryValidation(); }
        user.resetPassword(passwords.encode(request.newPassword())); stored.clearRecovery();
        stored.verificationHash = null; stored.verificationExpiresAt = null;
        refreshTokens.revokeAll(user.getId(), clock.instant());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { mail.send(user.getEmail(), "Taktak — Mật khẩu đã thay đổi",
                        "Mật khẩu của bạn đã được thay đổi. Mọi phiên cũ đã bị thu hồi. Nếu không phải bạn, hãy liên hệ hỗ trợ."); }
                catch (RuntimeException ignored) {
                    org.slf4j.LoggerFactory.getLogger(RecoveryService.class).error("Password-change notification failed; reset committed.");
                }
            }
        });
    }
    private static boolean constantEquals(String first, String second) {
        return java.security.MessageDigest.isEqual(first.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                second.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
    public static final class RecoveryValidation extends RuntimeException {
        public RecoveryValidation() { super("Invalid replacement password."); }
    }
}
