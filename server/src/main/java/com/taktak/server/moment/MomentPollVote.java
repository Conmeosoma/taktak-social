package com.taktak.server.moment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moment_poll_votes")
public class MomentPollVote {
    @Id
    private UUID id;

    @Column(name = "poll_id", nullable = false)
    private UUID pollId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "selected_option", nullable = false, length = 1)
    private String selectedOption;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MomentPollVote() { }

    public MomentPollVote(UUID id, UUID pollId, UUID userId, String selectedOption, Instant createdAt) {
        this.id = id;
        this.pollId = pollId;
        this.userId = userId;
        this.selectedOption = selectedOption;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getPollId() { return pollId; }
    public UUID getUserId() { return userId; }
    public String getSelectedOption() { return selectedOption; }
    public Instant getCreatedAt() { return createdAt; }
}

