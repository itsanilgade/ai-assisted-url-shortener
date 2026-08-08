package com.schwab.shortener.repository;

import com.schwab.shortener.domain.LinkStatus;
import com.schwab.shortener.domain.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID> {
    Optional<ShortLink> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ShortLink s
           set s.accessCount = s.accessCount + 1,
               s.lastAccessedAt = :now
         where s.shortCode = :code
           and s.status = :status
           and (s.expiresAt is null or s.expiresAt > :now)
        """)
    int recordAccess(@Param("code") String code, @Param("status") LinkStatus status, @Param("now") Instant now);
}
