package com.schwab.shortener.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.schwab.shortener.exception.ApiException;
import org.junit.jupiter.api.Test;

class UrlPolicyTest {

    private final UrlPolicy policy = new UrlPolicy();

    @Test
    void acceptsHttpAndHttps() {
        assertEquals("https://example.com/a", policy.normalizeAndValidate("https://example.com/a"));
        assertEquals("http://example.com", policy.normalizeAndValidate("http://example.com"));
    }

    @Test
    void rejectsUnsafeSchemes() {
        assertThrows(ApiException.class, () -> policy.normalizeAndValidate("javascript:alert(1)"));
        assertThrows(ApiException.class, () -> policy.normalizeAndValidate("ftp://example.com"));
    }

    @Test
    void rejectsEmbeddedCredentials() {
        assertThrows(ApiException.class, () -> policy.normalizeAndValidate("https://user:pass@example.com"));
    }
}
