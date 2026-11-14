package com.taktak.server.moment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MomentPollRepository extends JpaRepository<MomentPoll, UUID> {
    Optional<MomentPoll> findByMomentId(UUID momentId);
}

