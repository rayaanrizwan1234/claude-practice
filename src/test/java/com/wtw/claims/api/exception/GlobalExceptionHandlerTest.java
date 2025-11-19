package com.wtw.claims.api.exception;

import com.wtw.claims.api.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * <p>Tests verify that each exception type is:
 * <ul>
 *   <li>Mapped to the correct HTTP status code</li>
 *   <li>Formatted with the appropriate error message</li>
 *   <li>Contains the correct error response structure</li>
 * </ul>
 */
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private HttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getRequestURI()).thenReturn("/api/v1/claims/process");
    }

    @Nested
    @DisplayName("IllegalArgumentException Handling")
    class IllegalArgumentExceptionHandling {

        @Test
        @DisplayName("Should return 400 Bad Request status")
        void handleIllegalArgumentException_returns400Status() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("Invalid input data");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should include exception message in response")
        void handleIllegalArgumentException_includesExceptionMessage() {
            // Arrange
            String errorMessage = "CSV header is invalid";
            IllegalArgumentException ex = new IllegalArgumentException(errorMessage);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, mockRequest);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("Should include correct error details in response")
        void handleIllegalArgumentException_hasCorrectErrorDetails() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("Test error");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, mockRequest);

            // Assert
            ErrorResponse errorResponse = response.getBody();
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.status()).isEqualTo(400);
            assertThat(errorResponse.error()).isEqualTo("Bad Request");
            assertThat(errorResponse.path()).isEqualTo("/api/v1/claims/process");
            assertThat(errorResponse.timestamp()).isNotNull();
            assertThat(errorResponse.timestamp()).isBeforeOrEqualTo(Instant.now());
            assertThat(errorResponse.details()).isNull();
        }

        @Test
        @DisplayName("Should handle empty error message")
        void handleIllegalArgumentException_withEmptyMessage_handlesGracefully() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException("");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null error message")
        void handleIllegalArgumentException_withNullMessage_handlesGracefully() {
            // Arrange
            IllegalArgumentException ex = new IllegalArgumentException((String) null);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().message()).isNull();
        }
    }

    @Nested
    @DisplayName("MaxUploadSizeExceededException Handling")
    class MaxUploadSizeExceededExceptionHandling {

        @Test
        @DisplayName("Should return 413 Payload Too Large status")
        void handleMaxUploadSizeExceededException_returns413Status() {
            // Arrange
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50 * 1024 * 1024);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSizeExceededException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        }

        @Test
        @DisplayName("Should include user-friendly message")
        void handleMaxUploadSizeExceededException_includesUserFriendlyMessage() {
            // Arrange
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50 * 1024 * 1024);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSizeExceededException(ex, mockRequest);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).contains("exceeds the maximum allowed size");
        }

        @Test
        @DisplayName("Should include max allowed size in details")
        void handleMaxUploadSizeExceededException_includesMaxSizeInDetails() {
            // Arrange
            long maxSize = 50 * 1024 * 1024; // 50MB
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(maxSize);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSizeExceededException(ex, mockRequest);

            // Assert
            ErrorResponse errorResponse = response.getBody();
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.details()).isNotNull();
            assertThat(errorResponse.details()).containsKey("maxAllowedSize");
            assertThat(errorResponse.details().get("maxAllowedSize")).isEqualTo(maxSize);
        }

        @Test
        @DisplayName("Should include correct error type")
        void handleMaxUploadSizeExceededException_hasCorrectErrorType() {
            // Arrange
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(100);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMaxUploadSizeExceededException(ex, mockRequest);

            // Assert
            ErrorResponse errorResponse = response.getBody();
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.status()).isEqualTo(413);
            assertThat(errorResponse.error()).isEqualTo("Payload Too Large");
        }
    }

    @Nested
    @DisplayName("MultipartException Handling")
    class MultipartExceptionHandling {

        @Test
        @DisplayName("Should return 400 Bad Request status")
        void handleMultipartException_returns400Status() {
            // Arrange
            MultipartException ex = new MultipartException("File upload failed");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMultipartException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Should include generic sanitized message in response")
        void handleMultipartException_includesExceptionMessage() {
            // Arrange
            String errorMessage = "Failed to parse multipart request";
            MultipartException ex = new MultipartException(errorMessage);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMultipartException(ex, mockRequest);

            // Assert - Should use sanitized generic message, not leak exception details
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo(
                "Error processing file upload. Please ensure the file is properly formatted and does not exceed size limits."
            );
        }

        @Test
        @DisplayName("Should use sanitized message for security")
        void handleMultipartException_prefixesMessage() {
            // Arrange
            MultipartException ex = new MultipartException("Invalid file");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMultipartException(ex, mockRequest);

            // Assert - Should use generic message for security (don't leak implementation details)
            assertThat(response.getBody().message()).isEqualTo(
                "Error processing file upload. Please ensure the file is properly formatted and does not exceed size limits."
            );
        }

        @Test
        @DisplayName("Should have correct error structure")
        void handleMultipartException_hasCorrectErrorStructure() {
            // Arrange
            MultipartException ex = new MultipartException("Test error");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleMultipartException(ex, mockRequest);

            // Assert
            ErrorResponse errorResponse = response.getBody();
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.status()).isEqualTo(400);
            assertThat(errorResponse.error()).isEqualTo("Bad Request");
            assertThat(errorResponse.path()).isEqualTo("/api/v1/claims/process");
            assertThat(errorResponse.details()).isNull();
        }
    }

    @Nested
    @DisplayName("IOException Handling")
    class IOExceptionHandling {

        @Test
        @DisplayName("Should return 500 Internal Server Error status")
        void handleIOException_returns500Status() {
            // Arrange
            IOException ex = new IOException("Error reading file");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should include generic I/O error message")
        void handleIOException_includesGenericMessage() {
            // Arrange
            IOException ex = new IOException("Specific error details");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(ex, mockRequest);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("An I/O error occurred while processing the request");
        }

        @Test
        @DisplayName("Should not expose internal error details")
        void handleIOException_doesNotExposeInternalDetails() {
            // Arrange
            IOException ex = new IOException("Internal filesystem error at /sensitive/path");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(ex, mockRequest);

            // Assert
            assertThat(response.getBody().message()).doesNotContain("Internal filesystem");
            assertThat(response.getBody().message()).doesNotContain("/sensitive/path");
        }

        @Test
        @DisplayName("Should have correct error structure")
        void handleIOException_hasCorrectErrorStructure() {
            // Arrange
            IOException ex = new IOException("Test IO error");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleIOException(ex, mockRequest);

            // Assert
            ErrorResponse errorResponse = response.getBody();
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.status()).isEqualTo(500);
            assertThat(errorResponse.error()).isEqualTo("Internal Server Error");
            assertThat(errorResponse.path()).isEqualTo("/api/v1/claims/process");
            assertThat(errorResponse.details()).isNull();
        }
    }

    @Nested
    @DisplayName("Generic Exception Handling")
    class GenericExceptionHandling {

        @Test
        @DisplayName("Should return 500 Internal Server Error status")
        void handleGenericException_returns500Status() {
            // Arrange
            Exception ex = new Exception("Unexpected error");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should include generic error message")
        void handleGenericException_includesGenericMessage() {
            // Arrange
            Exception ex = new Exception("Specific error details that should not be exposed");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, mockRequest);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        }

        @Test
        @DisplayName("Should handle RuntimeException")
        void handleGenericException_handlesRuntimeException() {
            // Arrange
            RuntimeException ex = new RuntimeException("Runtime error");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("Should handle NullPointerException")
        void handleGenericException_handlesNullPointerException() {
            // Arrange
            NullPointerException ex = new NullPointerException("Null pointer");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, mockRequest);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred");
        }

        @Test
        @DisplayName("Should not expose sensitive information")
        void handleGenericException_doesNotExposeSensitiveInfo() {
            // Arrange
            Exception ex = new Exception("Error connecting to database at db.internal.com with password=secret123");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, mockRequest);

            // Assert
            assertThat(response.getBody().message()).doesNotContain("password");
            assertThat(response.getBody().message()).doesNotContain("secret123");
            assertThat(response.getBody().message()).doesNotContain("db.internal.com");
        }

        @Test
        @DisplayName("Should have correct error structure")
        void handleGenericException_hasCorrectErrorStructure() {
            // Arrange
            Exception ex = new Exception("Test error");

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, mockRequest);

            // Assert
            ErrorResponse errorResponse = response.getBody();
            assertThat(errorResponse).isNotNull();
            assertThat(errorResponse.status()).isEqualTo(500);
            assertThat(errorResponse.error()).isEqualTo("Internal Server Error");
            assertThat(errorResponse.timestamp()).isNotNull();
            assertThat(errorResponse.details()).isNull();
        }
    }

    @Nested
    @DisplayName("Error Response Structure Validation")
    class ErrorResponseStructureValidation {

        @Test
        @DisplayName("Should always include timestamp")
        void allHandlers_includeTimestamp() {
            // Arrange
            Instant beforeTest = Instant.now();

            // Act
            ResponseEntity<ErrorResponse> response1 = exceptionHandler
                .handleIllegalArgumentException(new IllegalArgumentException("test"), mockRequest);
            ResponseEntity<ErrorResponse> response2 = exceptionHandler
                .handleIOException(new IOException("test"), mockRequest);
            ResponseEntity<ErrorResponse> response3 = exceptionHandler
                .handleGenericException(new Exception("test"), mockRequest);

            Instant afterTest = Instant.now();

            // Assert
            assertThat(response1.getBody().timestamp())
                .isAfterOrEqualTo(beforeTest)
                .isBeforeOrEqualTo(afterTest);
            assertThat(response2.getBody().timestamp())
                .isAfterOrEqualTo(beforeTest)
                .isBeforeOrEqualTo(afterTest);
            assertThat(response3.getBody().timestamp())
                .isAfterOrEqualTo(beforeTest)
                .isBeforeOrEqualTo(afterTest);
        }

        @Test
        @DisplayName("Should always include request path")
        void allHandlers_includeRequestPath() {
            // Arrange
            String testPath = "/api/v1/test/endpoint";
            when(mockRequest.getRequestURI()).thenReturn(testPath);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler
                .handleIllegalArgumentException(new IllegalArgumentException("test"), mockRequest);

            // Assert
            assertThat(response.getBody().path()).isEqualTo(testPath);
        }

        @Test
        @DisplayName("Should handle different request paths correctly")
        void allHandlers_handleDifferentPaths() {
            // Arrange
            String customPath = "/custom/path/to/resource";
            when(mockRequest.getRequestURI()).thenReturn(customPath);

            // Act
            ResponseEntity<ErrorResponse> response = exceptionHandler
                .handleIOException(new IOException("test"), mockRequest);

            // Assert
            assertThat(response.getBody().path()).isEqualTo(customPath);
        }
    }
}
