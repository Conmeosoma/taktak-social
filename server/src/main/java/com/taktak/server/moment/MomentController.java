package com.taktak.server.moment;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/moments")
public class MomentController {
    private final MomentService moments;
    public MomentController(MomentService moments) { this.moments = moments; }
    public record CreatedMoment(UUID id) { }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatedMoment> upload(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestPart("image") MultipartFile image, @RequestParam(defaultValue = "") String caption,
            @RequestParam org.springframework.util.MultiValueMap<String, String> fields) throws IOException {
        UUID id = moments.upload(UUID.fromString(jwt.getSubject()), image, caption, fields.get("recipientIds"), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(new CreatedMoment(id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) throws IOException {
        byte[] image = moments.image(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=moment.jpg").contentLength(image.length).body(image);
    }

    @GetMapping
    public ResponseEntity<MomentDtos.MomentsPage> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String type, @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(moments.listMoments(UUID.fromString(jwt.getSubject()), type, page));
    }

    @GetMapping("/latest")
    public ResponseEntity<MomentDtos.MomentItem> latest(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return moments.latestMoment(userId)
                .map(item -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(item))
                .orElseGet(() -> ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        moments.delete(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.noContent().build();
    }
}
