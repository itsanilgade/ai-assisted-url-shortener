package com.schwab.shortener.service;

import com.schwab.shortener.model.request.CreateLinkRequest;
import com.schwab.shortener.model.response.AnalyticsResponse;
import com.schwab.shortener.model.response.LinkResponse;

/**
 * Application service contract for URL-shortener use cases.
 * Controllers depend on this abstraction rather than an implementation.
 */
public interface LinkService {

    LinkResponse create(CreateLinkRequest request);

    LinkResponse get(String code);

    String resolveAndRecord(String code, String referrer, String userAgent);

    AnalyticsResponse analytics(String code);

    void deactivate(String code);
}
