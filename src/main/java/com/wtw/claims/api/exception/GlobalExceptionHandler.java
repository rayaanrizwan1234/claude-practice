package com.wtw.claims.api.exception;

import com.wtw.claims.api.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Claims API.
 *
 * <p>This handler provides consistent error responses for all exceptions thrown
 * by the API. It maps different exception types to appropriate HTTP status codes
 * and formats error details in a standardized way.</p>
 *
 * <p><strong>Exception Mapping:</strong></p>
 * <ul>
 *   <li>IllegalArgumentException - 400 Bad Request (invalid input)</li>
 *   <li>MaxUploadSizeExceededException - 413 Payload Too Large</li>
 *   <li>MultipartException - 400 Bad Request (file upload issues)</li>
 *   <li>IOException - 500 Internal Server Error</li>
 *   <li>Other exceptions - 500 Internal Server Error</li>
 * </ul>
 *
 * @author Claims Processing Team
 * @version 2.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles IllegalArgumentException, typically thrown for invalid input data.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 400 Bad Request status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        logger.warn("Invalid input: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MaxUploadSizeExceededException when file upload exceeds configured limits.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 413 Payload Too Large status
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        logger.warn("File upload size exceeded: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("maxAllowedSize", ex.getMaxUploadSize());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "Payload Too Large",
            "The uploaded file exceeds the maximum allowed size",
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    /**
     * Handles MultipartException for general file upload issues.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 400 Bad Request status
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipartException(
            MultipartException ex,
            HttpServletRequest request
    ) {
        logger.warn("File upload error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Error processing file upload. Please ensure the file is properly formatted and does not exceed size limits.",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles IOException for I/O errors during processing.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 500 Internal Server Error status
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(
            IOException ex,
            HttpServletRequest request
    ) {
        logger.error("I/O error during processing", ex);

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An I/O error occurred while processing the request",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles JobNotFoundException when a requested job does not exist.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 404 Not Found status
     */
    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFoundException(
            JobNotFoundException ex,
            HttpServletRequest request
    ) {
        logger.warn("Job not found: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("jobId", ex.getJobId().toString());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles JobNotCompletedException when results are requested for an incomplete job.
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 400 Bad Request status
     */
    @ExceptionHandler(JobNotCompletedException.class)
    public ResponseEntity<ErrorResponse> handleJobNotCompletedException(
            JobNotCompletedException ex,
            HttpServletRequest request
    ) {
        logger.warn("Job not completed: {}", ex.getMessage());

        Map<String, Object> details = new HashMap<>();
        details.put("jobId", ex.getJobId().toString());
        details.put("currentStatus", ex.getCurrentStatus());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI(),
            details
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles IllegalStateException for state-related errors (e.g., job limit reached).
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 503 Service Unavailable status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        logger.warn("Service state error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            ex.getMessage(),
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handles all other uncaught exceptions as a fallback.
     *
     * <p>This prevents internal error details from leaking to clients while
     * still providing a structured error response.</p>
     *
     * @param ex the exception
     * @param request the HTTP request
     * @return ResponseEntity with 500 Internal Server Error status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        logger.error("Unexpected error", ex);

        ErrorResponse errorResponse = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred",
            request.getRequestURI(),
            null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}