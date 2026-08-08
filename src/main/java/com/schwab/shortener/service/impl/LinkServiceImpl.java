package com.schwab.shortener.service.impl;

import com.schwab.shortener.domain.ClickEvent;
import com.schwab.shortener.domain.LinkStatus;
import com.schwab.shortener.domain.ShortLink;
import com.schwab.shortener.exception.ApiException;
import com.schwab.shortener.model.request.CreateLinkRequest;
import com.schwab.shortener.model.response.AnalyticsResponse;
import com.schwab.shortener.model.response.LinkResponse;
import com.schwab.shortener.repository.ClickEventRepository;
import com.schwab.shortener.repository.ShortLinkRepository;
import com.schwab.shortener.service.LinkService;
import com.schwab.shortener.util.ShortCodeGenerator;
import com.schwab.shortener.util.UrlPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional implementation of all URL-shortener business use cases.
 */
@Service
public class LinkServiceImpl implements LinkService {

    private static final Set<String> RESERVED_ALIASES = Set.of("actuator", "api", "health", "metrics");
    private static final int GENERATED_CODE_ATTEMPTS = 5;
    private static final int MAX_TRACKING_HEADER_LENGTH = 512;

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlPolicy urlPolicy;
    private final Clock clock;
    private final String baseUrl;

    public LinkServiceImpl(
            ShortLinkRepository shortLinkRepository,
            ClickEventRepository clickEventRepository,
            ShortCodeGenerator shortCodeGenerator,
            UrlPolicy urlPolicy,
            Clock clock,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickEventRepository = clickEventRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlPolicy = urlPolicy;
        this.clock = clock;
        this.baseUrl = baseUrl.replaceAll("/$", "");
    }

    @Override
    @Transactional
    public LinkResponse create(CreateLinkRequest request) {
        String normalizedUrl = urlPolicy.normalizeAndValidate(request.url());
        String customAlias = request.customAlias();

        if (customAlias != null) {
            validateAlias(customAlias);
            if (shortLinkRepository.existsByShortCode(customAlias)) {
                throw new ApiException(HttpStatus.CONFLICT, "Custom alias is already in use");
            }
        }

        int attempts = customAlias == null ? GENERATED_CODE_ATTEMPTS : 1;
        for (int attempt = 0; attempt < attempts; attempt++) {
            String code = customAlias != null ? customAlias : shortCodeGenerator.next();
            try {
                ShortLink saved = shortLinkRepository.saveAndFlush(
                        new ShortLink(UUID.randomUUID(), code, normalizedUrl, clock.instant(), request.expiresAt()));
                return LinkResponse.from(saved, baseUrl);
            } catch (DataIntegrityViolationException ex) {
                if (customAlias != null || attempt == attempts - 1) {
                    throw new ApiException(HttpStatus.CONFLICT, "Unable to allocate a unique short code");
                }
            }
        }
        throw new ApiException(HttpStatus.CONFLICT, "Unable to allocate a unique short code");
    }

    @Override
    @Transactional(readOnly = true)
    public LinkResponse get(String code) {
        return LinkResponse.from(requireLink(code), baseUrl);
    }

    @Override
    @Transactional
    public String resolveAndRecord(String code, String referrer, String userAgent) {
        ShortLink link = requireLink(code);
        Instant now = clock.instant();

        if (!link.isActive(now)) {
            throw new ApiException(HttpStatus.GONE, "Short link is inactive or expired");
        }

        // The database performs the increment atomically so concurrent redirects do not lose clicks.
        if (shortLinkRepository.recordAccess(code, LinkStatus.ACTIVE, now) != 1) {
            throw new ApiException(HttpStatus.GONE, "Short link became inactive or expired");
        }

        clickEventRepository.save(new ClickEvent(
                UUID.randomUUID(),
                code,
                now,
                truncate(referrer),
                truncate(userAgent)));
        return link.getOriginalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(String code) {
        ShortLink link = requireLink(code);
        var recentClicks = clickEventRepository.findTop100ByShortCodeOrderByOccurredAtDesc(code).stream()
                .map(click -> new AnalyticsResponse.RecentClick(click.getOccurredAt(), click.getReferrer()))
                .toList();
        return new AnalyticsResponse(code, link.getAccessCount(), link.getLastAccessedAt(), recentClicks);
    }

    @Override
    @Transactional
    public void deactivate(String code) {
        ShortLink link = requireLink(code);
        link.deactivate();
    }

    private ShortLink requireLink(String code) {
        return shortLinkRepository.findByShortCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Short link not found"));
    }

    private void validateAlias(String alias) {
        if (RESERVED_ALIASES.contains(alias.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Custom alias is reserved by the service");
        }
    }

    private String truncate(String value) {
        return value == null ? null : value.substring(0, Math.min(value.length(), MAX_TRACKING_HEADER_LENGTH));
    }
}
