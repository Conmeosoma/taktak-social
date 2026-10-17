package com.taktak.app.data.model;

import java.util.List;

public final class FriendModels {
    private FriendModels() { }
    public static final class User {
        public String id;
        public String username;
        public String displayName;
        public boolean valid() { return uuid(id) && username != null && username.matches("[a-z0-9_]{1,255}")
                && displayName != null && !displayName.trim().isEmpty() && displayName.length() <= 255; }
        @Override public String toString() { return "FriendUser[redacted]"; }
    }
    public static final class Invitation {
        public String id;
        public User sender;
        public String receiverId;
        public String status;
        public String createdAt;
        public boolean valid() { return uuid(id) && sender != null && sender.valid() && uuid(receiverId)
                && ("pending".equals(status) || "accepted".equals(status) || "rejected".equals(status))
                && createdAt != null; }
        @Override public String toString() { return "FriendInvitation[redacted]"; }
    }
    public static final class Page<T> {
        public List<T> items;
        public int page;
        public boolean hasNext;
    }
    public static final class Send {
        private final String receiverId;
        public Send(String id) { receiverId = id; }
    }
    public static final class Respond {
        private final String action;
        public Respond(boolean accept) { action = accept ? "accept" : "reject"; }
    }
    public static boolean uuid(String value) {
        return value != null && value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }
}
