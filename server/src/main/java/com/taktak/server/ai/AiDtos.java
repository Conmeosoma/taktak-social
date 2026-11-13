package com.taktak.server.ai;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public final class AiDtos {
    private AiDtos(){}
    public record ChatRequest(UUID conversationId,@NotNull UUID clientRequestId,
            @NotBlank @Size(max=4000) String message,@NotNull @AssertTrue Boolean shareWithGoogle) {
        @AssertTrue public boolean isTextSupported(){return message==null||message.indexOf('\0')<0;}
        @Override public String toString(){return "ChatRequest[redacted]";}
    }
    public record Conversation(UUID id,String title,Instant createdAt,Instant updatedAt){}
    public record Message(UUID id,UUID conversationId,String role,String content,Instant createdAt,int inputTokens,int outputTokens){
        @Override public String toString(){return "Message[redacted]";}
    }
    public record Page<T>(List<T> items,boolean hasNext){}
    public record Status(String availability,String privacyNotice){}
}
