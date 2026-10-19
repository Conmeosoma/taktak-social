package com.taktak.server.chat;

import com.taktak.server.chat.ChatDtos.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
public class ChatController {
    private final ChatService service;
    public ChatController(ChatService service) { this.service=service; }
    private UUID actor(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    private <T> ResponseEntity<T> ok(T value) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value); }
    @GetMapping public ResponseEntity<ConversationsPage> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page) { return ok(service.list(actor(jwt),page)); }
    @GetMapping("/{id}") public ResponseEntity<Conversation> conversation(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return ok(service.conversation(actor(jwt),id)); }
    @PostMapping("/with-friend/{friendId}") public ResponseEntity<Conversation> open(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID friendId) { return ok(service.open(actor(jwt),friendId)); }
    @GetMapping("/{id}/messages") public ResponseEntity<MessagesPage> messages(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page) { return ok(service.messages(actor(jwt),id,page)); }
    @PostMapping(value="/{id}/messages",consumes="application/json") public ResponseEntity<Message> send(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody SendRequest body) { return ok(service.send(actor(jwt),id,body)); }
    @PutMapping(value="/{id}/read",consumes="application/json") public ResponseEntity<Void> read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ReadRequest body) {
        service.read(actor(jwt),id,body.messageId());return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
