package com.taktak.server.user;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    @Query(value = "select * from users where id in (:first, :second) order by id for update", nativeQuery = true)
    java.util.List<User> lockPair(@Param("first") UUID first, @Param("second") UUID second);

    @Query(value = """
            select u.* from users u where u.id <> :actor and u.username like :prefix escape '!'
            and not exists (select 1 from blocks b where
                (b.blocker_id = :actor and b.blocked_id = u.id)
                or (b.blocker_id = u.id and b.blocked_id = :actor))
            order by u.username limit 20
            """, nativeQuery = true)
    java.util.List<User> searchVisible(@Param("actor") UUID actor, @Param("prefix") String prefix);

    // Cùng phép lower(email) với unique index của schema V1.
    @Query("select user from User user where lower(user.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCase(@Param("email") String email);
}
