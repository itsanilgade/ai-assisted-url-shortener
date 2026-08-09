package com.schwab.shortener.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.schwab.shortener.domain.ClickEvent;
import com.schwab.shortener.domain.LinkStatus;
import com.schwab.shortener.domain.ShortLink;
import com.schwab.shortener.exception.ApiException;
import com.schwab.shortener.model.request.CreateLinkRequest;
import com.schwab.shortener.repository.ClickEventRepository;
import com.schwab.shortener.repository.ShortLinkRepository;
import com.schwab.shortener.util.ShortCodeGenerator;
import com.schwab.shortener.util.UrlPolicy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

class LinkServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-08-08T12:00:00Z");
    private ShortLinkRepository links;
    private ClickEventRepository clicks;
    private ShortCodeGenerator generator;
    private UrlPolicy urlPolicy;
    private LinkServiceImpl service;

    @BeforeEach
    void setUp() {
        links = mock(ShortLinkRepository.class);
        clicks = mock(ClickEventRepository.class);
        generator = mock(ShortCodeGenerator.class);
        urlPolicy = mock(UrlPolicy.class);
        service = new LinkServiceImpl(
                links,
                clicks,
                generator,
                urlPolicy,
                Clock.fixed(NOW, ZoneOffset.UTC),
                "http://localhost:8080/");
    }

    @Test
    void createWithCustomAliasPersistsNormalizedUrl() {
        CreateLinkRequest request = new CreateLinkRequest(" https://example.com/a/../b ", "custom01", null);
        when(urlPolicy.normalizeAndValidate(request.url())).thenReturn("https://example.com/b");
        when(links.existsByShortCode("custom01")).thenReturn(false);
        when(links.saveAndFlush(any(ShortLink.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request);

        assertEquals("custom01", response.code());
        assertEquals("http://localhost:8080/custom01", response.shortUrl());
        assertEquals("https://example.com/b", response.originalUrl());
        assertEquals("ACTIVE", response.status());
        verify(generator, never()).next();
    }

    @Test
    void duplicateCustomAliasReturnsConflict() {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "taken123", null);
        when(urlPolicy.normalizeAndValidate(request.url())).thenReturn(request.url());
        when(links.existsByShortCode("taken123")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(links, never()).saveAndFlush(any());
    }

    @Test
    void reservedAliasIsRejectedCaseInsensitively() {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "AcTuAtOr", null);
        when(urlPolicy.normalizeAndValidate(request.url())).thenReturn(request.url());

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(links, never()).existsByShortCode(any());
    }

    @Test
    void generatedCodeRetriesAfterCollisionAndSucceeds() {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", null, null);
        when(urlPolicy.normalizeAndValidate(request.url())).thenReturn(request.url());
        when(generator.next()).thenReturn("AAAA2222", "BBBB3333");
        when(links.saveAndFlush(any(ShortLink.class)))
                .thenThrow(new DataIntegrityViolationException("collision"))
                .thenAnswer(inv -> inv.getArgument(0));

        var response = service.create(request);

        assertEquals("BBBB3333", response.code());
        verify(generator, times(2)).next();
        verify(links, times(2)).saveAndFlush(any(ShortLink.class));
    }

    @Test
    void generatedCodeReturnsConflictAfterAllRetriesFail() {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", null, null);
        when(urlPolicy.normalizeAndValidate(request.url())).thenReturn(request.url());
        when(generator.next()).thenReturn("AAAA2222");
        when(links.saveAndFlush(any(ShortLink.class)))
                .thenThrow(new DataIntegrityViolationException("collision"));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(generator, times(5)).next();
    }

    @Test
    void getReturnsLinkMetadata() {
        ShortLink link = link("meta1234", null);
        when(links.findByShortCode("meta1234")).thenReturn(Optional.of(link));

        var response = service.get("meta1234");

        assertEquals("meta1234", response.code());
        assertEquals("https://example.com", response.originalUrl());
    }

    @Test
    void missingLinkReturnsNotFound() {
        when(links.findByShortCode("missing1")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.get("missing1"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void activeRedirectRecordsAtomicAccessAndClickEvent() {
        ShortLink link = link("active12", NOW.plusSeconds(60));
        when(links.findByShortCode("active12")).thenReturn(Optional.of(link));
        when(links.recordAccess("active12", LinkStatus.ACTIVE, NOW)).thenReturn(1);

        String longReferrer = "r".repeat(600);
        String longAgent = "u".repeat(700);
        String target = service.resolveAndRecord("active12", longReferrer, longAgent);

        assertEquals("https://example.com", target);
        ArgumentCaptor<ClickEvent> eventCaptor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clicks).save(eventCaptor.capture());
        assertEquals(512, eventCaptor.getValue().getReferrer().length());
        verify(links).recordAccess("active12", LinkStatus.ACTIVE, NOW);
    }

    @Test
    void expiredLinkReturnsGoneWithoutRecordingClick() {
        ShortLink link = link("expired1", NOW);
        when(links.findByShortCode("expired1")).thenReturn(Optional.of(link));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.resolveAndRecord("expired1", null, null));

        assertEquals(HttpStatus.GONE, ex.getStatus());
        verify(links, never()).recordAccess(anyString(), any(), any());
        verify(clicks, never()).save(any());
    }

    @Test
    void redirectReturnsGoneIfLinkChangesStateDuringAtomicUpdate() {
        ShortLink link = link("race1234", NOW.plusSeconds(60));
        when(links.findByShortCode("race1234")).thenReturn(Optional.of(link));
        when(links.recordAccess("race1234", LinkStatus.ACTIVE, NOW)).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.resolveAndRecord("race1234", null, null));

        assertEquals(HttpStatus.GONE, ex.getStatus());
        verify(clicks, never()).save(any());
    }

    @Test
    void analyticsMapsRecentEvents() {
        ShortLink link = link("stats123", null);
        when(links.findByShortCode("stats123")).thenReturn(Optional.of(link));
        when(clicks.findTop100ByShortCodeOrderByOccurredAtDesc("stats123"))
                .thenReturn(List.of(new ClickEvent(UUID.randomUUID(), "stats123", NOW, "https://ref", "agent")));

        var analytics = service.analytics("stats123");

        assertEquals("stats123", analytics.code());
        assertEquals(1, analytics.recentClicks().size());
        assertEquals("https://ref", analytics.recentClicks().get(0).referrer());
    }

    @Test
    void deactivateChangesLinkStatus() {
        ShortLink link = link("off12345", null);
        when(links.findByShortCode("off12345")).thenReturn(Optional.of(link));

        service.deactivate("off12345");

        assertEquals(LinkStatus.INACTIVE, link.getStatus());
    }

    private ShortLink link(String code, Instant expiresAt) {
        return new ShortLink(UUID.randomUUID(), code, "https://example.com", NOW.minusSeconds(60), expiresAt);
    }
}
