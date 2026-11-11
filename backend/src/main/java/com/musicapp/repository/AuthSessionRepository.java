package com.musicapp.repository;

import com.musicapp.domain.AuthSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, String> {

    @Query("select s from AuthSession s join fetch s.user where s.tokenHash = :tokenHash")
    Optional<AuthSession> findWithUser(@Param("tokenHash") String tokenHash);

    /** Logs the account out everywhere. */
    @Modifying
    @Query("delete from AuthSession s where s.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
