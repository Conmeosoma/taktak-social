package com.taktak.server.moment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MomentPollVoteRepository extends JpaRepository<MomentPollVote, UUID> {
    Optional<MomentPollVote> findByPollIdAndUserId(UUID pollId, UUID userId);
    long countByPollIdAndSelectedOption(UUID pollId, String selectedOption);
    long countByPollId(UUID pollId);
}

