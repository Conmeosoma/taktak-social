package com.taktak.app.data.model;

public final class PollModels {
    private PollModels() { }

    public static class CreatePollRequest {
        public String question;
        public String optionA;
        public String optionB;

        public CreatePollRequest(String question, String optionA, String optionB) {
            this.question = question;
            this.optionA = optionA;
            this.optionB = optionB;
        }
    }

    public static class VoteRequest {
        public String option;

        public VoteRequest(String option) {
            this.option = option;
        }
    }

    public static class PollResponse {
        public String pollId;
        public String momentId;
        public String question;
        public String optionA;
        public String optionB;
        public long countA;
        public long countB;
        public long totalVotes;
        public int percentA;
        public int percentB;
        public String userVotedOption; // "A", "B", or null
        public String status; // "OPEN" or "CLOSED"
        public boolean isSender;
    }
}

