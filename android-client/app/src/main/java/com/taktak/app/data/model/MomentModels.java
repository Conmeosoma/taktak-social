package com.taktak.app.data.model;

import java.util.List;

public final class MomentModels {
    private MomentModels() { }

    public static final class PublicUser {
        public String id;
        public String username;
        public String displayName;

        public boolean valid() {
            return FriendModels.uuid(id) && username != null && username.matches("[a-z0-9_]{1,255}")
                    && displayName != null && !displayName.trim().isEmpty() && displayName.length() <= 255;
        }

        @Override public String toString() { return "PublicUser[redacted]"; }
    }

    public static final class MomentItem {
        public String id;
        public PublicUser sender;
        public String caption;
        public String createdAt;

        public boolean valid() {
            return FriendModels.uuid(id) && sender != null && sender.valid()
                    && caption != null && caption.codePointCount(0, caption.length()) <= 50
                    && createdAt != null;
        }

        @Override public String toString() { return "MomentItem[redacted]"; }
    }

    public static final class MomentsPage {
        public List<MomentItem> items;
        public int page;
        public boolean hasNext;
    }
}
