package com.taktak.server.moment;

import java.time.Instant;
import java.util.UUID;

public final class PollDtos {
    private PollDtos() { }

    public record CreatePollRequest(String question, String optionA, String optionB) { }

    public record VoteRequest(String option) { }

    public record PollResponse(
            UUID pollId,
            UUID momentId,
            String question,
            String optionA,
            String optionB,
            long countA,
            long countB,
            long totalVotes,
            int percentA,
            int percentB,
            String userVotedOption,
            String status,
            boolean isSender
    ) { }
}

