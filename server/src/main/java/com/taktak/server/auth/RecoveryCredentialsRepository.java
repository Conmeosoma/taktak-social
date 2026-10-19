package com.taktak.server.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecoveryCredentialsRepository extends JpaRepository<RecoveryCredentials, UUID> {
    @org.springframework.data.jpa.repository.Query("select c.userId from RecoveryCredentials c where c.resetHash = :hash")
    Optional<UUID> findUserIdByResetHash(@org.springframework.data.repository.query.Param("hash") String hash);
}
