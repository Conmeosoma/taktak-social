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
            @RequestPart("image") MultipartFile image, @RequestParam(defaultValue = "") String caption,
            @RequestParam(required = false) List<String> recipientIds) throws IOException {
        UUID id = moments.upload(UUID.fromString(jwt.getSubject()), image, caption, recipientIds);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(new CreatedMoment(id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) throws IOException {
        byte[] image = moments.image(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=moment.jpg").contentLength(image.length).body(image);
    }
}
