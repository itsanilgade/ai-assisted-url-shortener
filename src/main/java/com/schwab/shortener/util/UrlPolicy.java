package com.schwab.shortener.util;

import com.schwab.shortener.exception.ApiException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Central URL normalization and safety validation utility. */
@Component
public class UrlPolicy {

    public String normalizeAndValidate(String value) {
        try {
            URI uri = URI.create(value.trim());
            boolean supportedScheme = uri.getScheme() != null
                    && (uri.getScheme().equalsIgnoreCase("http") || uri.getScheme().equalsIgnoreCase("https"));
            boolean validHost = uri.getHost() != null && !uri.getHost().isBlank();
            boolean credentialsAbsent = uri.getUserInfo() == null;

            if (!supportedScheme || !validHost || !credentialsAbsent) {
                throw invalidUrl();
            }
            return uri.normalize().toString();
        } catch (IllegalArgumentException ex) {
            throw invalidUrl();
        }
    }

    private ApiException invalidUrl() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "Only valid http/https URLs with a host and no embedded credentials are allowed");
    }
}
