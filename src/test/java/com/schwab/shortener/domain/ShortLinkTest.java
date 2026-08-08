package com.schwab.shortener.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShortLinkTest {

    @Test
    void activeLinkBecomesExpiredAtExpirationBoundary() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-01-02T00:00:00Z");
        ShortLink link = new ShortLink(UUID.randomUUID(), "abc12345", "https://example.com", createdAt, expiresAt);

        assertTrue(link.isActive(Instant.parse("2026-01-01T23:59:59Z")));
        assertFalse(link.isActive(expiresAt));
    }

    @Test
    void deactivatedLinkIsNotActive() {
        ShortLink link = new ShortLink(
                UUID.randomUUID(),
                "abc12345",
                "https://example.com",
                Instant.parse("2026-01-01T00:00:00Z"),
                null);

        link.deactivate();

        assertFalse(link.isActive(Instant.parse("2026-01-01T01:00:00Z")));
    }
}
