package com.taktak.server.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class TokenConfiguration {
    // Định danh issuer logic, không dùng để discovery hoặc gọi HTTP.
    public static final String ISSUER = "https://taktak.invalid";
    public static final String AUDIENCE = "taktak-app";

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretKey jwtSigningKey(@Value("${taktak.jwt.access-secret:}") String secret) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException ignored) {
            throw new IllegalStateException("JWT_ACCESS_SECRET must be Base64 encoding at least 32 random bytes.");
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("JWT_ACCESS_SECRET must be Base64 encoding at least 32 random bytes.");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, Clock clock) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamp = new JwtTimestampValidator(Duration.ZERO);
        timestamp.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp,
                new JwtIssuerValidator(ISSUER),
                new JwtClaimValidator<List<String>>("aud", values -> values != null && values.contains(AUDIENCE)),
                new JwtClaimValidator<Instant>("exp", value -> value != null && value.isAfter(clock.instant())),
                new JwtClaimValidator<Instant>("iat", value -> value != null && !value.isAfter(clock.instant())),
                new JwtClaimValidator<String>("sub", value -> value != null && value.matches(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))));
        return decoder;
    }
}
