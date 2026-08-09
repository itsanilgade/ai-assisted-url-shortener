package com.schwab.shortener.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.schwab.shortener.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RateLimitInterceptorTest {

    @Test
    void nonCreateRequestsAreNotRateLimited() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor();
        HttpServletRequest request = request("GET", "/api/v1/links", "127.0.0.1");

        assertTrue(interceptor.preHandle(request, mock(HttpServletResponse.class), new Object()));
    }

    @Test
    void sixtyCreateRequestsAreAllowedAndSixtyFirstIsRejected() {
        RateLimitInterceptor interceptor = new RateLimitInterceptor();
        HttpServletResponse response = mock(HttpServletResponse.class);

        for (int i = 0; i < 60; i++) {
            assertTrue(interceptor.preHandle(request("POST", "/api/v1/links", "10.0.0.1"), response, new Object()));
        }

        ApiException ex = assertThrows(ApiException.class,
                () -> interceptor.preHandle(request("POST", "/api/v1/links", "10.0.0.1"), response, new Object()));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    private HttpServletRequest request(String method, String uri, String remoteAddr) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getRemoteAddr()).thenReturn(remoteAddr);
        return request;
    }
}
