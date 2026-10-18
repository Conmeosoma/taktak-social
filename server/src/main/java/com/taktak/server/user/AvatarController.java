package com.taktak.server.user;

import java.io.IOException;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AvatarController {
    private final AvatarService avatars;
    public AvatarController(AvatarService avatars) { this.avatars=avatars; }
    @PostMapping(value="/api/users/me/avatar",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> upload(@AuthenticationPrincipal Jwt jwt,@RequestPart("image") MultipartFile image) throws IOException {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(avatars.upload(UUID.fromString(jwt.getSubject()),image));
    }
    @GetMapping("/api/users/me/avatar")
    public ResponseEntity<byte[]> mine(@AuthenticationPrincipal Jwt jwt) throws IOException {
        UUID actor=UUID.fromString(jwt.getSubject()); return response(avatars.read(actor,actor));
    }
    @GetMapping("/api/users/{id}/avatar")
    public ResponseEntity<byte[]> avatar(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) throws IOException {
        return response(avatars.read(UUID.fromString(jwt.getSubject()),id));
    }
    private ResponseEntity<byte[]> response(byte[] bytes) {
        if(bytes==null) return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .header("X-Content-Type-Options","nosniff").body(bytes);
    }
}
