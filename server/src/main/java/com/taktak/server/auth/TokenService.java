package com.taktak.server.auth;

import com.taktak.server.config.TokenConfiguration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;
    private final long accessSeconds;
    private final long refreshDays;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, RefreshTokenRepository refreshTokens, Clock clock,
            @Value("${taktak.jwt.access-minutes:15}") long accessMinutes,
            @Value("${taktak.jwt.refresh-days:30}") long refreshDays) {
        if (accessMinutes < 1 || accessMinutes > 60 || refreshDays < 1 || refreshDays > 90) {
            throw new IllegalStateException("JWT access minutes must be 1..60; refresh days must be 1..90.");
        }
        this.encoder = encoder;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.accessSeconds = accessMinutes * 60;
        this.refreshDays = refreshDays;
    }

    // Caller giữ transaction để insert token và revoke token cũ cùng commit/rollback.
    public TokenPair issue(UUID userId) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        var claims = JwtClaimsSet.builder().issuer(TokenConfiguration.ISSUER)
                .audience(java.util.List.of(TokenConfiguration.AUDIENCE)).subject(userId.toString())
                .issuedAt(now).expiresAt(now.plusSeconds(accessSeconds)).id(UUID.randomUUID().toString()).build();
        String access = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.saveAndFlush(new RefreshToken(userId, hash(refresh), now, now.plus(refreshDays, ChronoUnit.DAYS)));
        return new TokenPair(access, refresh, accessSeconds, "Bearer");
    }

    public static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable.");
        }
    }
}
