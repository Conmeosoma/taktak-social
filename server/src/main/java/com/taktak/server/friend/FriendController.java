package com.taktak.server.friend;

import com.taktak.server.friend.FriendDtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class FriendController {
    private final FriendService friends;
    public FriendController(FriendService friends) { this.friends = friends; }

    @GetMapping("/api/users/search")
    public ResponseEntity<List<PublicUser>> search(@AuthenticationPrincipal Jwt jwt, @RequestParam String q,
                                                  HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(friends.search(
                UUID.fromString(jwt.getSubject()), q, request.getRemoteAddr()));
    }

    @PostMapping(value = "/api/friends/requests", consumes = "application/json")
    public ResponseEntity<RequestResponse> send(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SendRequest request) {
        var response = friends.send(UUID.fromString(jwt.getSubject()), request.receiverId());
        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore()).body(response);
    }

    @GetMapping("/api/friends/requests")
    public ResponseEntity<RequestsPage> incoming(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(friends.incoming(UUID.fromString(jwt.getSubject()), page));
    }

    @PutMapping(value = "/api/friends/requests/{id}", consumes = "application/json")
    public ResponseEntity<RequestResponse> respond(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                  @Valid @RequestBody DecisionRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(friends.respond(UUID.fromString(jwt.getSubject()), id, request.action()));
    }

    @GetMapping("/api/friends")
    public ResponseEntity<FriendsPage> list(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(friends.friends(UUID.fromString(jwt.getSubject()), page));
    }

    @DeleteMapping("/api/friends/{id}")
    public ResponseEntity<Void> unfriend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        friends.unfriend(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/api/blocks/{userId}")
    public ResponseEntity<Void> block(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        friends.block(UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @DeleteMapping("/api/blocks/{userId}")
    public ResponseEntity<Void> unblock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        friends.unblock(UUID.fromString(jwt.getSubject()), userId);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
