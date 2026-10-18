package com.taktak.server.moment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentRepository extends JpaRepository<Moment, UUID> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "insert into moment_recipients(moment_id,user_id,created_at) values(:moment,:recipient,:created)", nativeQuery = true)
    int addRecipient(@org.springframework.data.repository.query.Param("moment") UUID moment,
            @org.springframework.data.repository.query.Param("recipient") UUID recipient,
            @org.springframework.data.repository.query.Param("created") java.time.Instant created);

    @org.springframework.data.jpa.repository.Query(value = "select exists(select 1 from moment_recipients where moment_id=:moment and user_id=:viewer)", nativeQuery = true)
    boolean isRecipient(@org.springframework.data.repository.query.Param("moment") UUID moment,
            @org.springframework.data.repository.query.Param("viewer") UUID viewer);

    @org.springframework.data.jpa.repository.Query(value = "select user_id from moment_recipients where moment_id = :momentId", nativeQuery = true)
    java.util.List<UUID> findRecipientIds(@org.springframework.data.repository.query.Param("momentId") UUID momentId);

    java.util.Optional<Moment> findBySenderIdAndIdempotencyKey(UUID senderId, UUID idempotencyKey);

    @org.springframework.data.jpa.repository.Query(value = """
            select m.* from moments m
            join moment_recipients mr on m.id = mr.moment_id
            where mr.user_id = :viewerId
              and m.deleted_at is null
            order by mr.created_at desc, m.id desc
            limit 1
            """, nativeQuery = true)
    java.util.Optional<Moment> findLatestReceivedMoment(@org.springframework.data.repository.query.Param("viewerId") UUID viewerId);

    @org.springframework.data.jpa.repository.Query(value = """
            select m.* from moments m
            join moment_recipients mr on m.id = mr.moment_id
            where mr.user_id = :viewerId
              and m.deleted_at is null
            order by mr.created_at desc, m.id desc
            limit 21 offset :offset
            """, nativeQuery = true)
    java.util.List<Moment> receivedMoments(@org.springframework.data.repository.query.Param("viewerId") UUID viewerId,
            @org.springframework.data.repository.query.Param("offset") int offset);

    @org.springframework.data.jpa.repository.Query(value = """
            select m.* from moments m
            where m.sender_id = :senderId
              and m.deleted_at is null
            order by m.created_at desc, m.id desc
            limit 21 offset :offset
            """, nativeQuery = true)
    java.util.List<Moment> sentMoments(@org.springframework.data.repository.query.Param("senderId") UUID senderId,
            @org.springframework.data.repository.query.Param("offset") int offset);
}
