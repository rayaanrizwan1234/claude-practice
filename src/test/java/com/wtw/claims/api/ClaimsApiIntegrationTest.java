package com.wtw.claims.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the Claims API that test the full application stack.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>End-to-end processing from file upload to response</li>
 *   <li>Correct cumulative calculations using real service implementation</li>
 *   <li>Response format and structure with actual data</li>
 *   <li>Error handling across the entire stack</li>
 * </ul>
 *
 * <p>Uses @SpringBootTest for full application context loading.</p>
 */
@SpringBootTest(classes = ClaimsApiApplication.class)
@AutoConfigureMockMvc
@DisplayName("Claims API Integration Tests")
class ClaimsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PROCESS_ENDPOINT = "/api/v1/claims/process";

    @Nested
    @DisplayName("End-to-End Processing")
    class EndToEndProcessing {

        @Test
        @DisplayName("Should process single product claims end-to-end correctly")
        void processSingleProduct_endToEnd_calculatesCorrectCumulativeValues() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                Comp, 1992, 1993, 170.0
                Comp, 1993, 1993, 200.0
                """;

            MockMultipartFile file = createCsvFile("single_product.csv", csvContent);

            // Act & Assert
            MvcResult result = mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.metadata.earliestOriginYear").value(1992))
                .andExpect(jsonPath("$.metadata.numberOfDevelopmentYears").value(2))
                .andExpect(jsonPath("$.metadata.totalRecords").value(3))
                .andExpect(jsonPath("$.results", hasSize(1)))
                .andExpect(jsonPath("$.results[0].product").value("Comp"))
                .andExpect(jsonPath("$.results[0].cumulativeValues", hasSize(3)))
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(110.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[1]").value(280.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[2]").value(200.0))
                .andReturn();

            // Verify full response can be deserialized
            ProcessingResultResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProcessingResultResponse.class
            );
            assertThat(response.csvOutput()).contains("1992, 2");
            assertThat(response.csvOutput()).contains("Comp");
        }

        @Test
        @DisplayName("Should process multiple products with complete triangle data")
        void processMultipleProducts_endToEnd_calculatesCorrectlyForAllProducts() throws Exception {
            // Arrange - Using the example from CLAUDE.md
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                Comp, 1992, 1993, 170.0
                Comp, 1993, 1993, 200.0
                Non-Comp, 1990, 1990, 45.2
                Non-Comp, 1990, 1991, 64.8
                Non-Comp, 1990, 1993, 37.0
                Non-Comp, 1991, 1991, 50.0
                Non-Comp, 1991, 1992, 75.0
                Non-Comp, 1991, 1993, 25.0
                Non-Comp, 1992, 1992, 55.0
                Non-Comp, 1992, 1993, 85.0
                Non-Comp, 1993, 1993, 100.0
                """;

            MockMultipartFile file = createCsvFile("multiple_products.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.earliestOriginYear").value(1990))
                .andExpect(jsonPath("$.metadata.numberOfDevelopmentYears").value(4))
                .andExpect(jsonPath("$.metadata.totalRecords").value(12))
                .andExpect(jsonPath("$.results", hasSize(2)))
                // Comp - no data for 1990, 1991, starts from 1992
                .andExpect(jsonPath("$.results[0].product").value("Comp"))
                .andExpect(jsonPath("$.results[0].cumulativeValues")
                    .value(contains(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 110.0, 280.0, 200.0)))
                // Non-Comp - full data
                .andExpect(jsonPath("$.results[1].product").value("Non-Comp"))
                .andExpect(jsonPath("$.results[1].cumulativeValues")
                    .value(contains(45.2, 110.0, 110.0, 147.0, 50.0, 125.0, 150.0, 55.0, 140.0, 100.0)));
        }

        @Test
        @DisplayName("Should handle missing incremental values as zero correctly")
        void processMissingValues_endToEnd_treatsAsZero() throws Exception {
            // Arrange - Gap in development years
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1990, 1990, 100.0
                Comp, 1990, 1992, 50.0
                """;

            MockMultipartFile file = createCsvFile("missing_values.csv", csvContent);

            // Act & Assert
            // Origin 1990: 100, 100 (1991 missing), 150
            // Origin 1991: 0, 0, 0
            // Origin 1992: 0
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].cumulativeValues")
                    .value(contains(100.0, 100.0, 150.0, 0.0, 0.0, 0.0)));
        }

        @Test
        @DisplayName("Should handle empty incremental value fields")
        void processEmptyIncrementalValues_endToEnd_treatsAsZero() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 100.0
                Comp, 1992, 1993,
                Comp, 1993, 1993, 50.0
                """;

            MockMultipartFile file = createCsvFile("empty_values.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(100.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[1]").value(100.0)) // Empty = 0
                .andExpect(jsonPath("$.results[0].cumulativeValues[2]").value(50.0));
        }

        @Test
        @DisplayName("Should generate correct CSV output format")
        void processValidData_generatesCorrectCsvOutput() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                Comp, 1992, 1993, 170.0
                """;

            MockMultipartFile file = createCsvFile("test.csv", csvContent);

            // Act
            MvcResult result = mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

            ProcessingResultResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProcessingResultResponse.class
            );

            // Assert
            String csvOutput = response.csvOutput();
            String[] lines = csvOutput.split("\n");

            // First line should be: earliestOriginYear, numberOfDevYears
            assertThat(lines[0].trim()).isEqualTo("1992, 2");

            // Second line should contain Comp data
            assertThat(lines[1]).contains("Comp");
            assertThat(lines[1]).contains("110");
            assertThat(lines[1]).contains("280");
        }

        @Test
        @DisplayName("Should track processing time in metadata")
        void processValidData_recordsProcessingTime() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                """;

            MockMultipartFile file = createCsvFile("test.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.processingTimeMs").isNumber())
                .andExpect(jsonPath("$.metadata.processingTimeMs").value(greaterThanOrEqualTo(0)));
        }

        @Test
        @DisplayName("Should list all products in metadata alphabetically")
        void processMultipleProducts_listsAllProductsAlphabetically() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Zebra, 1992, 1992, 100.0
                Alpha, 1992, 1992, 200.0
                Mango, 1992, 1992, 150.0
                """;

            MockMultipartFile file = createCsvFile("test.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.productsProcessed", hasSize(3)))
                .andExpect(jsonPath("$.metadata.productsProcessed[0]").value("Alpha"))
                .andExpect(jsonPath("$.metadata.productsProcessed[1]").value("Mango"))
                .andExpect(jsonPath("$.metadata.productsProcessed[2]").value("Zebra"));
        }
    }

    @Nested
    @DisplayName("Error Handling End-to-End")
    class ErrorHandlingEndToEnd {

        @Test
        @DisplayName("Should return 400 for invalid CSV header")
        void processInvalidHeader_returns400WithErrorDetails() throws Exception {
            // Arrange
            String csvContent = """
                Invalid, Wrong, Headers
                Data, 1992, 100.0
                """;

            MockMultipartFile file = createCsvFile("invalid.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("Invalid CSV header")))
                .andExpect(jsonPath("$.path").value(PROCESS_ENDPOINT))
                .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should return 400 for non-numeric year values")
        void processNonNumericYear_returns400() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, not-a-year, 1992, 100.0
                """;

            MockMultipartFile file = createCsvFile("invalid.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Origin Year")));
        }

        @Test
        @DisplayName("Should return 400 for development year before origin year")
        void processInvalidYearRelation_returns400() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1993, 1992, 100.0
                """;

            MockMultipartFile file = createCsvFile("invalid.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("cannot be before origin year")));
        }

        @Test
        @DisplayName("Should return 400 for CSV with only header (no data)")
        void processHeaderOnly_returns400() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                """;

            MockMultipartFile file = createCsvFile("header_only.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("No claim records found")));
        }

        @Test
        @DisplayName("Should return 400 for empty file")
        void processEmptyFile_returns400() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
            );

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"));
        }

        @Test
        @DisplayName("Should return 400 for non-CSV file extension")
        void processNonCsvExtension_returns400() throws Exception {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.txt",
                "text/plain",
                "some content".getBytes(StandardCharsets.UTF_8)
            );

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File must be a CSV file"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle single record correctly")
        void processSingleRecord_succeeds() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 2020, 2020, 1000.0
                """;

            MockMultipartFile file = createCsvFile("single.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.numberOfDevelopmentYears").value(1))
                .andExpect(jsonPath("$.results[0].cumulativeValues", hasSize(1)))
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(1000.0));
        }

        @Test
        @DisplayName("Should handle negative incremental values")
        void processNegativeValues_succeeds() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 100.0
                Comp, 1992, 1993, -30.0
                """;

            MockMultipartFile file = createCsvFile("negative.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(100.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[1]").value(70.0)); // 100 + (-30)
        }

        @Test
        @DisplayName("Should handle very large values")
        void processLargeValues_succeeds() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 999999999.99
                Comp, 1992, 1993, 0.01
                """;

            MockMultipartFile file = createCsvFile("large.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(closeTo(999999999.99, 0.01)))
                .andExpect(jsonPath("$.results[0].cumulativeValues[1]").value(closeTo(1000000000.0, 0.01)));
        }

        @Test
        @DisplayName("Should handle whitespace in product names")
        void processWhitespaceProductNames_trimsCorrectly() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                  Comp  , 1992, 1992, 100.0
                """;

            MockMultipartFile file = createCsvFile("whitespace.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].product").value("Comp"));
        }

        @Test
        @DisplayName("Should handle products with special characters")
        void processSpecialCharacters_handlesCorrectly() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                "Comp-A (Type-1)", 1992, 1992, 100.0
                """;

            MockMultipartFile file = createCsvFile("special.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].product").value("Comp-A (Type-1)"));
        }

        @Test
        @DisplayName("Should handle large year gaps correctly")
        void processLargeYearGaps_calculatesCorrectly() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 2000, 2000, 100.0
                Comp, 2000, 2005, 50.0
                """;

            MockMultipartFile file = createCsvFile("gap.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.numberOfDevelopmentYears").value(6)) // 2000-2005 inclusive
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(100.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[1]").value(100.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[2]").value(100.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[3]").value(100.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[4]").value(100.0))
                .andExpect(jsonPath("$.results[0].cumulativeValues[5]").value(150.0)); // 100 + 50
        }

        @Test
        @DisplayName("Should preserve decimal precision")
        void processDecimalPrecision_preservesCorrectly() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 45.2
                Comp, 1992, 1993, 64.8
                """;

            MockMultipartFile file = createCsvFile("decimal.csv", csvContent);

            // Act & Assert
            mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].cumulativeValues[0]").value(closeTo(45.2, 0.001)))
                .andExpect(jsonPath("$.results[0].cumulativeValues[1]").value(closeTo(110.0, 0.001)));
        }
    }

    @Nested
    @DisplayName("Response Deserialization")
    class ResponseDeserialization {

        @Test
        @DisplayName("Should deserialize complete response correctly")
        void deserializeCompleteResponse_succeeds() throws Exception {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                Comp, 1992, 1993, 170.0
                """;

            MockMultipartFile file = createCsvFile("test.csv", csvContent);

            // Act
            MvcResult result = mockMvc.perform(multipart(PROCESS_ENDPOINT)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andReturn();

            // Assert - Deserialize and verify structure
            ProcessingResultResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProcessingResultResponse.class
            );

            assertThat(response).isNotNull();
            assertThat(response.metadata()).isNotNull();
            assertThat(response.results()).isNotEmpty();
            assertThat(response.csvOutput()).isNotBlank();

            // Verify metadata
            assertThat(response.metadata().earliestOriginYear()).isEqualTo(1992);
            assertThat(response.metadata().numberOfDevelopmentYears()).isEqualTo(2);
            assertThat(response.metadata().totalRecords()).isEqualTo(2);
            assertThat(response.metadata().productsProcessed()).containsExactly("Comp");
            assertThat(response.metadata().processingTimeMs()).isGreaterThanOrEqualTo(0);

            // Verify results
            assertThat(response.results()).hasSize(1);
            assertThat(response.results().get(0).product()).isEqualTo("Comp");
            assertThat(response.results().get(0).cumulativeValues())
                .containsExactly(110.0, 280.0);
        }
    }

    // Helper methods

    private MockMultipartFile createCsvFile(String filename, String content) {
        return new MockMultipartFile(
            "file",
            filename,
            "text/csv",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
