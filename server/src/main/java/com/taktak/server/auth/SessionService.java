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
    private final String dummyPasswordHash;

    public SessionService(UserRepository users, RefreshTokenRepository refreshTokens,
            PasswordEncoder passwords, TokenService tokens, Clock clock) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
        this.tokens = tokens;
        this.clock = clock;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        this.dummyPasswordHash = passwords.encode(Base64.getEncoder().encodeToString(bytes));
    }

    @Transactional
    public TokenPair login(LoginRequest request) {
        var user = users.findByEmailIgnoreCase(request.email());
        boolean matches = passwords.matches(request.password(),
                user.map(value -> value.getPasswordHash()).orElse(dummyPasswordHash));
        if (user.isEmpty() || !matches) { throw new AuthFailure(); }
        return tokens.issue(user.get().getId());
    }

    @Transactional
    public TokenPair refresh(String refreshToken) {
        var stored = refreshTokens.findLockedByTokenHash(TokenService.hash(refreshToken)).orElseThrow(AuthFailure::new);
        if (!stored.isActive(clock.instant())) { throw new AuthFailure(); }
        stored.revoke(clock.instant());
        return tokens.issue(stored.userId());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokens.findLockedByTokenHash(TokenService.hash(refreshToken))
                .ifPresent(stored -> stored.revoke(clock.instant()));
    }
}
