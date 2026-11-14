package com.taktak.app.data.repository;

import com.taktak.app.data.model.PollModels.*;
import com.taktak.app.data.remote.ApiClient;
import com.taktak.app.data.remote.ApiFailure;
import java.io.IOException;
import retrofit2.Response;

public final class PollRepository {
    private final ApiClient client;
    private final com.taktak.app.data.session.Session owner;

    public PollRepository(ApiClient client) {
        this.client = client;
        this.owner = client.sessions.snapshot();
    }
    private void requireCurrent() throws ApiFailure {
        var current = client.sessions.snapshot();
        if (current == null) throw new ApiFailure(ApiFailure.Kind.UNAUTHORIZED);
        if (owner == null || owner.generation != current.generation) throw new ApiFailure(ApiFailure.Kind.CANCELLED);
    }

    public PollResponse createPoll(String momentId, String question, String optionA, String optionB) throws IOException {
        requireCurrent();
        Response<PollResponse> response = client.poll.createPoll(momentId, new CreatePollRequest(question, optionA, optionB)).execute();
        requireCurrent();
        if (!response.isSuccessful()) {
            throw ApiFailure.from(response);
        }
        PollResponse result = response.body();
        if (result == null || !com.taktak.app.data.model.FriendModels.uuid(result.pollId)
                || !momentId.equals(result.momentId) || !question.trim().equals(result.question)
                || !optionA.trim().equals(result.optionA) || !optionB.trim().equals(result.optionB))
            throw new ApiFailure(ApiFailure.Kind.SERVER);
        return result;
    }

    public PollResponse getPoll(String momentId) throws IOException {
        requireCurrent();
        Response<PollResponse> response = client.poll.getPoll(momentId).execute();
        requireCurrent();
        if (response.code() == 404) {
            return null; // No poll for this moment
        }
        if (!response.isSuccessful()) {
            throw ApiFailure.from(response);
        }
        return response.body();
    }

    public PollResponse vote(String momentId, String option) throws IOException {
        requireCurrent();
        Response<PollResponse> response = client.poll.vote(momentId, new VoteRequest(option)).execute();
        requireCurrent();
        if (!response.isSuccessful()) {
            throw ApiFailure.from(response);
        }
        return response.body();
    }

    public PollResponse close(String momentId) throws IOException {
        requireCurrent();
        Response<PollResponse> response = client.poll.close(momentId).execute();
        requireCurrent();
        if (!response.isSuccessful()) {
            throw ApiFailure.from(response);
        }
        return response.body();
    }
}
