package com.schwab.shortener.model.response;

import java.time.Instant;
import java.util.List;

/** Aggregated click analytics returned for a short link. */
public record AnalyticsResponse(
        String code,
        long totalClicks,
        Instant lastAccessedAt,
        List<RecentClick> recentClicks) {

    public record RecentClick(Instant occurredAt, String referrer) {
    }
}
