package com.schwab.shortener.model.response;

import java.time.Instant;
import java.util.List;

/** Consistent JSON error response returned by the global exception handler. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<String> details) {
}
