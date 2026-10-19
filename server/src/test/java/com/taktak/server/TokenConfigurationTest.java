package com.taktak.server;

import com.taktak.server.config.TokenConfiguration;
import com.taktak.server.auth.TokenService;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenConfigurationTest {
    @Test
    void unsafeTokenLifetimesFailFast() {
        for (long[] values : List.of(new long[]{0, 30}, new long[]{61, 30}, new long[]{15, 0}, new long[]{15, 91})) {
            assertThatThrownBy(() -> new TokenService(null, null, java.time.Clock.systemUTC(), null, values[0], values[1]))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("JWT access minutes must be 1..60; refresh days must be 1..90.");
        }
    }

    @Test
    void missingMalformedAndShortKeysAreRejectedWithoutReflectingValues() {
        var configuration = new TokenConfiguration();
        for (String secret : List.of("", " ", "<set-locally>", Base64.getEncoder().encodeToString(new byte[31]))) {
            assertThatThrownBy(() -> configuration.jwtSigningKey(secret)).isInstanceOf(IllegalStateException.class)
                    .hasMessage("JWT_ACCESS_SECRET must be Base64 encoding at least 32 random bytes.");
        }
    }

    @Test
    void random256BitKeyIsAccepted() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        var key = new TokenConfiguration().jwtSigningKey(Base64.getEncoder().encodeToString(bytes));
        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).hasSize(32);
    }
}
