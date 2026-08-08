package com.schwab.shortener.model.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request payload used to create a generated or custom short URL. */
public record CreateLinkRequest(
        @NotBlank @Size(max = 2048) String url,
        @Pattern(
                regexp = "^[A-Za-z0-9_-]{4,32}$",
                message = "customAlias must be 4-32 URL-safe characters")
        String customAlias,
        @Future Instant expiresAt) {
}
