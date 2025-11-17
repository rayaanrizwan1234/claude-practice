package com.wtw.claims.api.dto.response;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response for the API.
 *
 * @param timestamp when the error occurred
 * @param status HTTP status code
 * @param error error type/category
 * @param message human-readable error message
 * @param path the request path that caused the error
 * @param details additional error details (optional)
 */
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, Object> details
) {
    public ErrorResponse {
        details = details != null ? Map.copyOf(details) : null;
    }
}