package com.taktak.server.user;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(users.getProfile(UUID.fromString(jwt.getSubject())));
    }

    @PutMapping(consumes = "application/json")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(users.updateProfile(UUID.fromString(jwt.getSubject()), request));
    }
}
