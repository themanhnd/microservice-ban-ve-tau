package com.xxxx.user.repository;

import com.xxxx.user.repository.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshTokenEntity t
            set t.revokedAt = :revokedAt
            where t.user.id = :userId and t.revokedAt is null
            """)
    int revokeActiveTokensForUser(@Param("userId") Long userId, @Param("revokedAt") LocalDateTime revokedAt);
}
