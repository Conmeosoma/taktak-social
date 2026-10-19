package com.taktak.server.auth;

import com.taktak.server.user.User;
import com.taktak.server.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        User user = new User(request.email(), request.username(),
                passwordEncoder.encode(request.password()), request.displayName());
        // Database quyết định unique, kể cả khi hai request đến đồng thời.
        return RegisterResponse.from(users.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public UsernameAvailabilityResponse checkUsernameAvailability(String rawUsername) {
        if (rawUsername == null) {
            return new UsernameAvailabilityResponse("", false);
        }
        String username = rawUsername.strip().toLowerCase(java.util.Locale.ROOT);
        if (!username.matches("^(?!.*__)[a-z0-9][a-z0-9_]{1,253}[a-z0-9]$")) {
            return new UsernameAvailabilityResponse(username, false);
        }
        boolean exists = users.existsByUsername(username);
        return new UsernameAvailabilityResponse(username, !exists);
    }
}
