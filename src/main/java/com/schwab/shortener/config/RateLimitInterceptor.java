package com.schwab.shortener.config;

import com.schwab.shortener.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Prototype per-JVM fixed-window rate limiter for link creation. */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int CREATE_LIMIT_PER_MINUTE = 60;
    private static final String CREATE_PATH = "/api/v1/links";

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"POST".equals(request.getMethod()) || !CREATE_PATH.equals(request.getRequestURI())) {
            return true;
        }

        String key = request.getRemoteAddr();
        long currentMinute = Instant.now().getEpochSecond() / 60;
        Window window = windows.compute(
                key,
                (ignored, existing) -> existing == null || existing.minute() != currentMinute
                        ? new Window(currentMinute, 1)
                        : new Window(currentMinute, existing.count() + 1));

        if (window.count() > CREATE_LIMIT_PER_MINUTE) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Create-link rate limit exceeded");
        }
        return true;
    }

    private record Window(long minute, int count) {
    }
}
