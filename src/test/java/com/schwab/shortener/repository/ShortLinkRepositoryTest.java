package com.schwab.shortener.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.schwab.shortener.domain.LinkStatus;
import com.schwab.shortener.domain.ShortLink;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ShortLinkRepositoryTest {

    @Autowired
    private ShortLinkRepository repository;

    @Test
    void findByShortCodeReturnsSavedLink() {
        repository.saveAndFlush(link("lookup01", null));
        assertTrue(repository.findByShortCode("lookup01").isPresent());
        assertTrue(repository.existsByShortCode("lookup01"));
    }

    @Test
    void duplicateShortCodeViolatesUniqueConstraint() {
        repository.saveAndFlush(link("unique01", null));
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAndFlush(link("unique01", null)));
    }

    @Test
    void recordAccessAtomicallyIncrementsCountAndTimestamp() {
        Instant now = Instant.parse("2026-08-08T12:00:00Z");
        repository.saveAndFlush(link("atomic01", now.plusSeconds(60)));

        assertEquals(1, repository.recordAccess("atomic01", LinkStatus.ACTIVE, now));
        repository.flush();
        ShortLink updated = repository.findByShortCode("atomic01").orElseThrow();

        assertEquals(1, updated.getAccessCount());
        assertEquals(now, updated.getLastAccessedAt());
    }

    @Test
    void recordAccessRejectsExpiredLink() {
        Instant now = Instant.parse("2026-08-08T12:00:00Z");
        repository.saveAndFlush(link("expired2", now));

        assertEquals(0, repository.recordAccess("expired2", LinkStatus.ACTIVE, now));
    }

    @Test
    void recordAccessRejectsInactiveLink() {
        Instant now = Instant.parse("2026-08-08T12:00:00Z");
        ShortLink link = link("inactive", null);
        link.deactivate();
        repository.saveAndFlush(link);

        assertEquals(0, repository.recordAccess("inactive", LinkStatus.ACTIVE, now));
    }

    private ShortLink link(String code, Instant expiresAt) {
        return new ShortLink(
                UUID.randomUUID(),
                code,
                "https://example.com",
                Instant.parse("2026-08-08T11:00:00Z"),
                expiresAt);
    }
}
