package com.taktak.server.moment;

import com.taktak.server.user.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class MomentDtos {
    private MomentDtos() { }

    public record PublicUser(UUID id, String username, String displayName) {
        public static PublicUser from(User user) {
            return new PublicUser(user.getId(), user.getUsername(), user.getDisplayName());
        }
    }

    public record MomentItem(UUID id, PublicUser sender, String caption, Instant createdAt) { }

    public record MomentsPage(List<MomentItem> items, int page, boolean hasNext) { }
}
