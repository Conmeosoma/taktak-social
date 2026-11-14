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
    private final PollService polls;
    public MomentController(MomentService moments, PollService polls) {
        this.moments = moments;
        this.polls = polls;
    }
    public record CreatedMoment(UUID id) { }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatedMoment> upload(@AuthenticationPrincipal Jwt jwt,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestPart("image") MultipartFile image,
            @RequestPart(value = "video", required = false) MultipartFile video,
            @RequestParam(defaultValue = "") String caption,
            @RequestParam org.springframework.util.MultiValueMap<String, String> fields) throws IOException {
        UUID senderId = UUID.fromString(jwt.getSubject());
        UUID id;
        if (video != null && !video.isEmpty()) {
            Integer durationMs = null;
            String rawDuration = fields.getFirst("durationMs");
            if (rawDuration != null && !rawDuration.isBlank()) {
                try { durationMs = Integer.parseInt(rawDuration); } catch (NumberFormatException ignored) { }
            }
            Boolean hasAudio = "true".equalsIgnoreCase(fields.getFirst("hasAudio"));
            id = moments.uploadLive(senderId, image, video, caption, fields.get("recipientIds"), idempotencyKey, durationMs, hasAudio);
        } else {
            id = moments.upload(senderId, image, caption, fields.get("recipientIds"), idempotencyKey);
        }
        String pollQuestion = fields.getFirst("pollQuestion");
        String pollOptionA = fields.getFirst("pollOptionA");
        String pollOptionB = fields.getFirst("pollOptionB");
        if (pollQuestion != null && !pollQuestion.isBlank() && pollOptionA != null && pollOptionB != null) {
            polls.createPoll(senderId, id, pollQuestion, pollOptionA, pollOptionB);
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(new CreatedMoment(id));
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> image(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) throws IOException {
        byte[] image = moments.image(UUID.fromString(jwt.getSubject()), id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=moment.jpg").contentLength(image.length).body(image);
    }

    @GetMapping("/{id}/video")
    public ResponseEntity<byte[]> video(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) throws IOException {
        byte[] video = moments.video(UUID.fromString(jwt.getSubject()), id);
        int total = video.length;
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] parts = rangeHeader.substring(6).split("-");
            int start = Integer.parseInt(parts[0]);
            int end = parts.length > 1 && !parts[1].isBlank() ? Integer.parseInt(parts[1]) : total - 1;
            if (start > end || start >= total) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + total).build();
            }
            end = Math.min(end, total - 1);
            int rangeLength = end - start + 1;
            byte[] rangeBytes = new byte[rangeLength];
            System.arraycopy(video, start, rangeBytes, 0, rangeLength);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + total)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=moment.mp4")
                    .contentLength(rangeLength)
                    .body(rangeBytes);
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("video/mp4"))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=moment.mp4")
                .contentLength(total)
                .body(video);
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
