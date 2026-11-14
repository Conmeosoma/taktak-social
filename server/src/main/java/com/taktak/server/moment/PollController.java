package com.taktak.server.moment;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/moments/{id}/poll")
public class PollController {
    private final PollService polls;

    public PollController(PollService polls) {
        this.polls = polls;
    }

    @PostMapping
    public ResponseEntity<PollDtos.PollResponse> createPoll(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody PollDtos.CreatePollRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(polls.createPoll(userId, id, request.question(), request.optionA(), request.optionB()));
    }

    @GetMapping
    public ResponseEntity<PollDtos.PollResponse> getPoll(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(polls.getPoll(userId, id));
    }

    @PostMapping("/vote")
    public ResponseEntity<PollDtos.PollResponse> vote(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody PollDtos.VoteRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(polls.vote(userId, id, request.option()));
    }

    @PostMapping("/close")
    public ResponseEntity<PollDtos.PollResponse> close(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(polls.closePoll(userId, id));
    }
}

