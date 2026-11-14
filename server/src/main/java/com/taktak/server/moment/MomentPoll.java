package com.taktak.server.moment;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moment_polls")
public class MomentPoll {
    @Id
    private UUID id;

    @Column(name = "moment_id", nullable = false, unique = true)
    private UUID momentId;

    @Column(nullable = false, length = 100)
    private String question;

    @Column(name = "option_a", nullable = false, length = 40)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 40)
    private String optionB;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MomentPoll() { }

    public MomentPoll(UUID id, UUID momentId, String question, String optionA, String optionB, String status, Instant createdAt) {
        this.id = id;
        this.momentId = momentId;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.status = status != null ? status : "OPEN";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getMomentId() { return momentId; }
    public String getQuestion() { return question; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
}

