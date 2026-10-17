package com.taktak.server.friend;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {
    interface Participants {
        UUID getSenderId();
        UUID getReceiverId();
    }

    // Projection tránh cache một entity có status cũ trước khi chờ khóa cặp users.
    @Query("select r.senderId as senderId, r.receiverId as receiverId from FriendRequest r where r.id=:id")
    java.util.Optional<Participants> participants(@Param("id") UUID id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from FriendRequest r where r.id=:id")
    java.util.Optional<FriendRequest> forResponse(@Param("id") UUID id);

    @Modifying
    @Query(value = """
            insert into friendships(user_low_id,user_high_id,created_at)
            values(least(:first,:second),greatest(:first,:second),:now)
            on conflict (user_low_id,user_high_id) do nothing
            """, nativeQuery = true)
    int addFriendship(@Param("first") UUID first, @Param("second") UUID second, @Param("now") java.time.Instant now);

    @Modifying
    @Query(value = "delete from friendships where user_low_id=least(:first,:second) and user_high_id=greatest(:first,:second)", nativeQuery = true)
    int removeFriendship(@Param("first") UUID first, @Param("second") UUID second);

    @Modifying
    @Query(value = """
            insert into blocks(blocker_id,blocked_id,created_at) values(:actor,:target,:now)
            on conflict (blocker_id,blocked_id) do nothing
            """, nativeQuery = true)
    int addBlock(@Param("actor") UUID actor, @Param("target") UUID target, @Param("now") java.time.Instant now);

    @Modifying
    @Query(value = "delete from blocks where blocker_id=:actor and blocked_id=:target", nativeQuery = true)
    int removeOwnBlock(@Param("actor") UUID actor, @Param("target") UUID target);

    @Modifying
    @Query(value = """
            update friend_requests set status='rejected', responded_at=:now where status='pending' and
            ((sender_id=:first and receiver_id=:second) or (sender_id=:second and receiver_id=:first))
            """, nativeQuery = true)
    int rejectPendingPair(@Param("first") UUID first, @Param("second") UUID second, @Param("now") java.time.Instant now);

    @Query(value = """
            select u.* from friendships f join users u on
                u.id=case when f.user_low_id=:actor then f.user_high_id else f.user_low_id end
            where (f.user_low_id=:actor or f.user_high_id=:actor)
            and not exists (select 1 from blocks b where
                (b.blocker_id=:actor and b.blocked_id=u.id) or (b.blocker_id=u.id and b.blocked_id=:actor))
            order by u.username limit 21 offset :offset
            """, nativeQuery = true)
    List<com.taktak.server.user.User> friends(@Param("actor") UUID actor, @Param("offset") int offset);

    @Query(value = """
            select exists (select 1 from blocks where
                (blocker_id=:first and blocked_id=:second) or (blocker_id=:second and blocked_id=:first))
            """, nativeQuery = true)
    boolean blocked(@Param("first") UUID first, @Param("second") UUID second);

    @Query(value = """
            select exists (select 1 from friendships where
                user_low_id=least(:first,:second) and user_high_id=greatest(:first,:second))
            """, nativeQuery = true)
    boolean alreadyFriends(@Param("first") UUID first, @Param("second") UUID second);

    @Query(value = """
            select exists (select 1 from friend_requests where status='pending' and
                ((sender_id=:first and receiver_id=:second) or (sender_id=:second and receiver_id=:first)))
            """, nativeQuery = true)
    boolean pending(@Param("first") UUID first, @Param("second") UUID second);

    @Query(value = """
            select r.* from friend_requests r where receiver_id=:actor and status='pending'
            and not exists (select 1 from blocks b where
                (b.blocker_id=:actor and b.blocked_id=r.sender_id)
                or (b.blocker_id=r.sender_id and b.blocked_id=:actor))
            order by r.created_at desc, r.id desc limit 21 offset :offset
            """, nativeQuery = true)
    List<FriendRequest> incoming(@Param("actor") UUID actor, @Param("offset") int offset);
}
