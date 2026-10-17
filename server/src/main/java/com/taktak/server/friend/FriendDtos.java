package com.taktak.server.friend;

import com.taktak.server.user.User;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FriendDtos {
    private FriendDtos() { }
    public record PublicUser(UUID id, String username, String displayName) {
        static PublicUser from(User user) { return new PublicUser(user.getId(), user.getUsername(), user.getDisplayName()); }
    }
    public record SendRequest(@NotNull UUID receiverId) { }
    public record DecisionRequest(@NotNull @Pattern(regexp = "accept|reject") String action) { }
    public record RequestResponse(UUID id, PublicUser sender, UUID receiverId, String status, Instant createdAt) {
        static RequestResponse from(FriendRequest request, User sender) {
            return new RequestResponse(request.getId(), PublicUser.from(sender), request.getReceiverId(),
                    request.getStatus(), request.getCreatedAt());
        }
    }
    public record RequestsPage(List<RequestResponse> items, int page, boolean hasNext) { }
    public record FriendsPage(List<PublicUser> items, int page, boolean hasNext) { }
}
