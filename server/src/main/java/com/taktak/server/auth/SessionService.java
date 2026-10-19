package com.taktak.server.auth;

import com.taktak.server.user.UserRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwords;
    private final TokenService tokens;
    private final Clock clock;
    private final RecoveryService recovery;
    private final String dummyPasswordHash;

    public SessionService(UserRepository users, RefreshTokenRepository refreshTokens,
            PasswordEncoder passwords, TokenService tokens, Clock clock, RecoveryService recovery) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.tokens = tokens;
        this.clock = clock;
        this.recovery = recovery;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        this.dummyPasswordHash = passwords.encode(Base64.getEncoder().encodeToString(bytes));
    }

    @Transactional
    public Object login(LoginRequest request) {
        var user = users.findIdByEmail(request.email()).flatMap(users::findLockedById);
        boolean matches = passwords.matches(request.password(),
                user.map(value -> value.getPasswordHash()).orElse(dummyPasswordHash));
        if (user.isEmpty()) { throw new AuthFailure(); }
        if (!matches) { return recovery.loginTemporary(user.get(), request.password()); }
        return tokens.issue(user.get().getId());
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        String hash = TokenService.hash(refreshToken);
        var found = refreshTokens.findByTokenHash(hash).orElseThrow(AuthFailure::new);
        users.findLockedById(found.userId()).orElseThrow(AuthFailure::new);
        // Native bulk revoke trong reset không được đọc qua entity đã cache trước lúc chờ khóa.
        var stored = refreshTokens.findLockedByTokenHash(hash).orElseThrow(AuthFailure::new);
        entityManager.refresh(stored);
        if (!stored.isActive(clock.instant())) { throw new AuthFailure(); }
        stored.revoke(clock.instant());
        return tokens.issue(stored.userId());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.findLockedByTokenHash(TokenService.hash(refreshToken))
                .ifPresent(stored -> stored.revoke(clock.instant()));
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
}
