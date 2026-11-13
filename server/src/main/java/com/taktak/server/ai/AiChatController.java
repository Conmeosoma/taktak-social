package com.taktak.server.ai;
import com.taktak.server.ai.AiDtos.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService service;
    public AiChatController(AiChatService service){this.service=service;}
    private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
    private <T> ResponseEntity<T> ok(T value){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(value);}
    @GetMapping("/status") public ResponseEntity<Status> status(){return ok(service.status());}
    @PostMapping("/chat") public ResponseEntity<Message> chat(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ChatRequest request){return ok(service.chat(user(jwt),request));}
    @PostMapping("/conversations") public ResponseEntity<Conversation> create(@AuthenticationPrincipal Jwt jwt){return ok(service.create(user(jwt)));}
    @GetMapping("/conversations") public ResponseEntity<Page<Conversation>> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="0") int page){return ok(service.list(user(jwt),page));}
    @GetMapping("/conversations/{id}/messages") public ResponseEntity<Page<Message>> messages(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam(defaultValue="0") int page){return ok(service.messages(user(jwt),id,page));}
    @DeleteMapping("/conversations/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){service.delete(user(jwt),id);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
}
