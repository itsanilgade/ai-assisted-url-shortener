package com.schwab.shortener.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GlobalExceptionHandlerTest {

    @Test
    void apiExceptionIsMappedToStructuredError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleApiException(new ApiException(HttpStatus.NOT_FOUND, "missing"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("missing", response.getBody().message());
    }
}
