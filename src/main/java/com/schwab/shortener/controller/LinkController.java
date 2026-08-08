package com.schwab.shortener.controller;

import com.schwab.shortener.model.request.CreateLinkRequest;
import com.schwab.shortener.model.response.AnalyticsResponse;
import com.schwab.shortener.model.response.LinkResponse;
import com.schwab.shortener.service.LinkService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing link-management, redirect, and analytics endpoints.
 */
@RestController
public class LinkController {

    private final LinkService linkService;

    public LinkController(LinkService linkService) {
        this.linkService = linkService;
    }

    @PostMapping("/api/v1/links")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        LinkResponse response = linkService.create(request);
        return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
    }

    @GetMapping("/api/v1/links/{code}")
    public LinkResponse get(@PathVariable String code) {
        return linkService.get(code);
    }

    @GetMapping("/api/v1/links/{code}/analytics")
    public AnalyticsResponse analytics(@PathVariable String code) {
        return linkService.analytics(code);
    }

    @DeleteMapping("/api/v1/links/{code}")
    public ResponseEntity<Void> deactivate(@PathVariable String code) {
        linkService.deactivate(code);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> redirect(
            @PathVariable String code,
            @RequestHeader(value = "Referer", required = false) String referrer,
            @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String target = linkService.resolveAndRecord(code, referrer, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
