package com.taktak.server.moment;

import com.taktak.server.user.UserService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PollService {
    private final MomentRepository moments;
    private final MomentPollRepository polls;
    private final MomentPollVoteRepository votes;
    private final UserService users;

    public PollService(MomentRepository moments, MomentPollRepository polls,
            MomentPollVoteRepository votes, UserService users) {
        this.moments = moments;
        this.polls = polls;
        this.votes = votes;
        this.users = users;
    }

    @Transactional
    public PollDtos.PollResponse createPoll(UUID actorId, UUID momentId, String question, String optionA, String optionB) {
        users.getProfile(actorId);
        Moment moment = moments.lockForPoll(momentId)
                .filter(m -> m.getDeletedAt() == null && m.getSenderId().equals(actorId))
                .orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "MOMENT_NOT_FOUND"));
        if (question == null || question.isBlank() || question.codePointCount(0, question.length()) > 100) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_POLL_QUESTION");
        }
        if (optionA == null || optionA.isBlank() || optionA.codePointCount(0, optionA.length()) > 40) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_POLL_OPTION");
        }
        if (optionB == null || optionB.isBlank() || optionB.codePointCount(0, optionB.length()) > 40) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_POLL_OPTION");
        }

        var existing = polls.findByMomentId(momentId);
        if (existing.isPresent()) {
            MomentPoll poll = existing.get();
            if (!poll.getQuestion().equals(question.trim()) || !poll.getOptionA().equals(optionA.trim())
                    || !poll.getOptionB().equals(optionB.trim()))
                throw new MomentFailure(HttpStatus.CONFLICT, "POLL_ALREADY_EXISTS");
            return buildPollResponse(actorId, moment, poll);
        }
        MomentPoll poll = new MomentPoll(
                UUID.randomUUID(),
                momentId,
                question.trim(),
                optionA.trim(),
                optionB.trim(),
                "OPEN",
                Instant.now()
        );
        return buildPollResponse(actorId, moment, polls.saveAndFlush(poll));
    }

    @Transactional(readOnly = true)
    public PollDtos.PollResponse getPoll(UUID viewerId, UUID momentId) {
        users.getProfile(viewerId);
        Moment moment = requireAuthorizedMoment(viewerId, momentId);
        MomentPoll poll = polls.findByMomentId(momentId)
                .orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "POLL_NOT_FOUND"));

        return buildPollResponse(viewerId, moment, poll);
    }

    @Transactional
    public PollDtos.PollResponse vote(UUID voterId, UUID momentId, String option) {
        users.getProfile(voterId);
        if (option == null || (!option.equals("A") && !option.equals("B"))) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_VOTE_OPTION");
        }
        Moment moment = requireAuthorizedMoment(voterId, momentId);
        if (moment.getSenderId().equals(voterId)) {
            throw new MomentFailure(HttpStatus.FORBIDDEN, "SENDER_CANNOT_VOTE");
        }

        MomentPoll poll = polls.findByMomentId(momentId)
                .orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "POLL_NOT_FOUND"));

        if (!"OPEN".equalsIgnoreCase(poll.getStatus())) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "POLL_CLOSED");
        }

        if (votes.findByPollIdAndUserId(poll.getId(), voterId).isPresent()) {
            throw new MomentFailure(HttpStatus.CONFLICT, "ALREADY_VOTED");
        }

        try {
            MomentPollVote vote = new MomentPollVote(UUID.randomUUID(), poll.getId(), voterId, option, Instant.now());
            votes.saveAndFlush(vote);
        } catch (DataIntegrityViolationException e) {
            throw new MomentFailure(HttpStatus.CONFLICT, "ALREADY_VOTED");
        }

        return buildPollResponse(voterId, moment, poll);
    }

    @Transactional
    public PollDtos.PollResponse closePoll(UUID senderId, UUID momentId) {
        users.getProfile(senderId);
        Moment moment = moments.findById(momentId).filter(m -> m.getDeletedAt() == null)
                .orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "MOMENT_NOT_FOUND"));
        if (!moment.getSenderId().equals(senderId)) {
            throw new MomentFailure(HttpStatus.FORBIDDEN, "FORBIDDEN");
        }

        MomentPoll poll = polls.findByMomentId(momentId)
                .orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "POLL_NOT_FOUND"));

        poll.setStatus("CLOSED");
        polls.saveAndFlush(poll);

        return buildPollResponse(senderId, moment, poll);
    }

    private Moment requireAuthorizedMoment(UUID viewerId, UUID momentId) {
        return moments.findById(momentId).filter(m -> m.getDeletedAt() == null
                && (m.getSenderId().equals(viewerId) || moments.isRecipient(momentId, viewerId)))
                .orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "MOMENT_NOT_FOUND"));
    }

    private PollDtos.PollResponse buildPollResponse(UUID viewerId, Moment moment, MomentPoll poll) {
        long countA = votes.countByPollIdAndSelectedOption(poll.getId(), "A");
        long countB = votes.countByPollIdAndSelectedOption(poll.getId(), "B");
        long total = countA + countB;
        int percentA = total > 0 ? (int) Math.round((countA * 100.0) / total) : 0;
        int percentB = total > 0 ? (100 - percentA) : 0;

        String userVotedOption = votes.findByPollIdAndUserId(poll.getId(), viewerId)
                .map(MomentPollVote::getSelectedOption)
                .orElse(null);

        boolean isSender = moment.getSenderId().equals(viewerId);

        return new PollDtos.PollResponse(
                poll.getId(),
                moment.getId(),
                poll.getQuestion(),
                poll.getOptionA(),
                poll.getOptionB(),
                countA,
                countB,
                total,
                percentA,
                percentB,
                userVotedOption,
                poll.getStatus(),
                isSender
        );
    }
}

