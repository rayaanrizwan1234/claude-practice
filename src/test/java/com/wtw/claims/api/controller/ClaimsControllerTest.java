package com.wtw.claims.api.controller;

import com.wtw.claims.api.dto.response.ProcessingMetadata;
import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import com.wtw.claims.api.dto.response.ProductResult;
import com.wtw.claims.api.exception.GlobalExceptionHandler;
import com.wtw.claims.api.service.ClaimsProcessingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive tests for ClaimsController REST endpoints.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Successful file upload and processing</li>
 *   <li>File validation (empty file, non-CSV file)</li>
 *   <li>Response structure and content</li>
 *   <li>Error handling propagation</li>
 * </ul>
 *
 * <p>Uses @WebMvcTest for lightweight web layer testing with mocked service.</p>
 */
@WebMvcTest(ClaimsController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ClaimsController Web Layer Tests")
class ClaimsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimsProcessingService processingService;

    private static final String PROCESS_ENDPOINT = "/api/v1/claims/process";

    @Nested
    @DisplayName("POST /api/v1/claims/process - Successful Processing")
    class SuccessfulProcessing {

        @Test
        @DisplayName("Should return 200 OK with valid CSV file")
        void processValidCsvFile_returns200WithResponse() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                """;

            MockMultipartFile file = new MockMultipartFile(
                "file",
                "claims.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
            );

            ProcessingResultResponse mockResponse = createMockResponse(
                1992, 1, 1, List.of("Comp"), 10L,
                List.of(new ProductResult("Comp", List.of(110.0))),
                "1992, 1\nComp, 110"
            );

            when(processingService.processClaims(any(InputStream.class))).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.metadata").exists())
                .andExpect(jsonPath("$.results").exists())
                .andExpect(jsonPath("$.csvOutput").exists());

            verify(processingService, times(1)).processClaims(any(InputStream.class));
        }

        @Test
        @DisplayName("Should return correct metadata in response")
        void processValidCsvFile_returnsCorrectMetadata() throws Exception {
            // Arrange
            MockMultipartFile file = createValidCsvFile();

            ProcessingResultResponse mockResponse = createMockResponse(
                1990, 4, 100, List.of("Comp", "Non-Comp"), 250L,
                List.of(
                    new ProductResult("Comp", List.of(110.0, 280.0)),
                    new ProductResult("Non-Comp", List.of(45.2, 110.0))
                ),
                "1990, 4\nComp, 110, 280\nNon-Comp, 45.2, 110"
            );

            when(processingService.processClaims(any(InputStream.class))).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.earliestOriginYear").value(1990))
                .andExpect(jsonPath("$.metadata.numberOfDevelopmentYears").value(4))
                .andExpect(jsonPath("$.metadata.totalRecords").value(100))
                .andExpect(jsonPath("$.metadata.productsProcessed", hasSize(2)))
                .andExpect(jsonPath("$.metadata.productsProcessed[0]").value("Comp"))
                .andExpect(jsonPath("$.metadata.productsProcessed[1]").value("Non-Comp"))
                .andExpect(jsonPath("$.metadata.processingTimeMs").value(250));
        }

        @Test
        @DisplayName("Should return correct product results in response")
        void processValidCsvFile_returnsCorrectProductResults() throws Exception {
            // Arrange
            MockMultipartFile file = createValidCsvFile();

            ProcessingResultResponse mockResponse = createMockResponse(
                1992, 2, 5, List.of("Comp"), 15L,
                List.of(new ProductResult("Comp", List.of(110.0, 280.0, 200.0))),
                "1992, 2\nComp, 110, 280, 200"
            );

            when(processingService.processClaims(any(InputStream.class))).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].product").value("Comp"))
                .andExpect(jsonPath("$.results[0].cumulativeValues", hasSize(3)))
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(110.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[1]").value(280.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[2]").value(200.0));
        }

        @Test
        @DisplayName("Should return CSV output string in response")
        void processValidCsvFile_returnsCsvOutputString() throws Exception {
            // Arrange
            MockMultipartFile file = createValidCsvFile();
            String expectedCsvOutput = "1992, 2\nComp, 110, 280";

            ProcessingResultResponse mockResponse = createMockResponse(
                1992, 2, 3, List.of("Comp"), 20L,
                List.of(new ProductResult("Comp", List.of(110.0, 280.0))),
                expectedCsvOutput
            );

            when(processingService.processClaims(any(InputStream.class))).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.csvOutput").value(expectedCsvOutput));
        }

        @Test
        @DisplayName("Should handle multiple products in response")
        void processMultipleProducts_returnsAllInResponse() throws Exception {
            // Arrange
            MockMultipartFile file = createValidCsvFile();

            ProcessingResultResponse mockResponse = createMockResponse(
                1990, 4, 12, List.of("Alpha", "Beta", "Gamma"), 100L,
                List.of(
                    new ProductResult("Alpha", List.of(100.0)),
                    new ProductResult("Beta", List.of(200.0)),
                    new ProductResult("Gamma", List.of(300.0))
                ),
                "1990, 4\nAlpha, 100\nBeta, 200\nGamma, 300"
            );

            when(processingService.processClaims(any(InputStream.class))).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results", hasSize(3)))
                .andExpect(jsonPath("$.metadata.productsProcessed", hasSize(3)));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/claims/process - File Validation Errors")
    class FileValidationErrors {

        @Test
        @DisplayName("Should return 400 Bad Request for empty file")
        void processEmptyFile_returns400BadRequest() throws Exception {
            // Arrange
            MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
            );

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(emptyFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"))
                .andExpect(jsonPath("$.path").value(PROCESS_ENDPOINT));

            verify(processingService, never()).processClaims(any(InputStream.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for non-CSV file extension")
        void processNonCsvFile_returns400BadRequest() throws Exception {
            // Arrange
            MockMultipartFile txtFile = new MockMultipartFile(
                "file",
                "data.txt",
                "text/plain",
                "some content".getBytes(StandardCharsets.UTF_8)
            );

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(txtFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("File must be a CSV file"));

            verify(processingService, never()).processClaims(any(InputStream.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for XML file")
        void processXmlFile_returns400BadRequest() throws Exception {
            // Arrange
            MockMultipartFile xmlFile = new MockMultipartFile(
                "file",
                "data.xml",
                "application/xml",
                "<data>test</data>".getBytes(StandardCharsets.UTF_8)
            );

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(xmlFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File must be a CSV file"));

            verify(processingService, never()).processClaims(any(InputStream.class));
        }

        @Test
        @DisplayName("Should return 400 Bad Request for JSON file")
        void processJsonFile_returns400BadRequest() throws Exception {
            // Arrange
            MockMultipartFile jsonFile = new MockMultipartFile(
                "file",
                "data.json",
                "application/json",
                "{\"test\": \"data\"}".getBytes(StandardCharsets.UTF_8)
            );

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(jsonFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File must be a CSV file"));
        }

        @Test
        @DisplayName("Should accept CSV file with uppercase extension")
        void processCsvWithUppercaseExtension_accepts() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "claims.CSV",
                "text/csv",
                createValidCsvContent().getBytes(StandardCharsets.UTF_8)
            );

            ProcessingResultResponse mockResponse = createSimpleMockResponse();
            when(processingService.processClaims(any(InputStream.class))).thenReturn(mockResponse);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());

            verify(processingService, times(1)).processClaims(any(InputStream.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/claims/process - Service Error Handling")
    class ServiceErrorHandling {

        @Test
        @DisplayName("Should return 400 Bad Request when service throws IllegalArgumentException")
        void serviceThrowsIllegalArgumentException_returns400() throws Exception {
            // Arrange
            MockMultipartFile file = createValidCsvFile();

            when(processingService.processClaims(any(InputStream.class)))
                .thenThrow(new IllegalArgumentException("Invalid CSV format: missing required columns"));

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid CSV format: missing required columns"));
        }

        @Test
        @DisplayName("Should return 500 Internal Server Error when service throws IOException")
        void serviceThrowsIOException_returns500() throws Exception {
            // Arrange
            MockMultipartFile file = createValidCsvFile();

            when(processingService.processClaims(any(InputStream.class)))
                .thenThrow(new IOException("Error reading input stream"));

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An I/O error occurred while processing the request"));
        }

        @Test
        @DisplayName("Should return 500 Internal Server Error when service throws unexpected exception")
        void serviceThrowsUnexpectedException_returns500() throws Exception {
            // Arrange
            MockMultipartFile file = createValidCsvFile();

            when(processingService.processClaims(any(InputStream.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/claims/process - Content Type and Request Validation")
    class RequestValidation {

        @Test
        @DisplayName("Should return 400 when no file is provided")
        void noFileProvided_returns400() throws Exception {
            // Act & Assert - Spring will reject the request when required param is missing
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle file with null filename gracefully")
        void fileWithNullFilename_accepts() throws Exception {
            // Arrange - null filename should be handled by the controller
            // The controller checks filename != null before validating extension
            MockMultipartFile file = new MockMultipartFile(
                "file",
                null, // null filename
                "text/csv",
                createValidCsvContent().getBytes(StandardCharsets.UTF_8)
            );

            ProcessingResultResponse mockResponse = createSimpleMockResponse();
            when(processingService.processClaims(any(InputStream.class))).thenReturn(mockResponse);

            // Act & Assert
            // When filename is null, controller should not validate extension
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
        }
    }

    // Helper methods for creating test data

    private MockMultipartFile createValidCsvFile() {
        return new MockMultipartFile(
            "file",
            "claims.csv",
            "text/csv",
            createValidCsvContent().getBytes(StandardCharsets.UTF_8)
        );
    }

    private String createValidCsvContent() {
        return """
            Product, Origin Year, Development Year, Incremental Value
            Comp, 1992, 1992, 110.0
            Comp, 1992, 1993, 170.0
            """;
    }

    private ProcessingResultResponse createMockResponse(
            int earliestOriginYear,
            int numberOfDevelopmentYears,
            int totalRecords,
            List<String> productsProcessed,
            long processingTimeMs,
            List<ProductResult> results,
            String csvOutput
    ) {
        ProcessingMetadata metadata = new ProcessingMetadata(
            earliestOriginYear,
            numberOfDevelopmentYears,
            totalRecords,
            productsProcessed,
            processingTimeMs
        );
        return new ProcessingResultResponse(metadata, results, csvOutput);
    }

    private ProcessingResultResponse createSimpleMockResponse() {
        return createMockResponse(
            1992, 1, 1, List.of("Comp"), 10L,
            List.of(new ProductResult("Comp", List.of(100.0))),
            "1992, 1\nComp, 100"
        );
    }
}
