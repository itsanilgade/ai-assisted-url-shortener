package com.schwab.shortener.model.response;

import com.schwab.shortener.domain.ShortLink;
import java.time.Instant;

/** API representation of a short link and its current state. */
public record LinkResponse(
        String code,
        String shortUrl,
        String originalUrl,
        String status,
        Instant createdAt,
        Instant expiresAt,
        long accessCount,
        Instant lastAccessedAt) {

    public static LinkResponse from(ShortLink link, String baseUrl) {
        return new LinkResponse(
                link.getShortCode(),
                baseUrl + "/" + link.getShortCode(),
                link.getOriginalUrl(),
                link.getStatus().name(),
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.getAccessCount(),
                link.getLastAccessedAt());
    }
}
