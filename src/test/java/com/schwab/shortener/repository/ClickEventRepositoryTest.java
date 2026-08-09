package com.schwab.shortener.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.schwab.shortener.domain.ClickEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ClickEventRepositoryTest {

    @Autowired
    private ClickEventRepository repository;

    @Test
    void recentClicksAreReturnedNewestFirst() {
        repository.save(new ClickEvent(UUID.randomUUID(), "stats001", Instant.parse("2026-08-08T10:00:00Z"), "old", "agent"));
        repository.save(new ClickEvent(UUID.randomUUID(), "stats001", Instant.parse("2026-08-08T11:00:00Z"), "new", "agent"));
        repository.save(new ClickEvent(UUID.randomUUID(), "other001", Instant.parse("2026-08-08T12:00:00Z"), "other", "agent"));
        repository.flush();

        var results = repository.findTop100ByShortCodeOrderByOccurredAtDesc("stats001");

        assertEquals(2, results.size());
        assertEquals("new", results.get(0).getReferrer());
        assertEquals("old", results.get(1).getReferrer());
    }
}
