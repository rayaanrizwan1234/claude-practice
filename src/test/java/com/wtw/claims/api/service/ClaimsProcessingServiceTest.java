package com.wtw.claims.api.service;

import com.wtw.claims.api.dto.response.ProcessingMetadata;
import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import com.wtw.claims.api.dto.response.ProductResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Comprehensive unit tests for ClaimsProcessingService.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Valid CSV processing with single and multiple products</li>
 *   <li>Metadata generation (year range, record count, processing time)</li>
 *   <li>Cumulative value calculation correctness</li>
 *   <li>Error handling (null input, invalid CSV, empty data)</li>
 *   <li>Edge cases (missing values, gaps in years)</li>
 * </ul>
 */
@DisplayName("ClaimsProcessingService Unit Tests")
class ClaimsProcessingServiceTest {

    private ClaimsProcessingService service;

    @BeforeEach
    void setUp() {
        service = new ClaimsProcessingService();
    }

    @Nested
    @DisplayName("Valid CSV Processing")
    class ValidCsvProcessing {

        @Test
        @DisplayName("Should process single product with consecutive years correctly")
        void processSingleProduct_withConsecutiveYears_returnsCorrectCumulative() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                Comp, 1992, 1993, 170.0
                Comp, 1993, 1993, 200.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.results()).hasSize(1);

            ProductResult compResult = response.results().get(0);
            assertThat(compResult.product()).isEqualTo("Comp");

            // Expected cumulative values:
            // Origin 1992: Year 1 = 110.0, Year 2 = 110.0 + 170.0 = 280.0
            // Origin 1993: Year 1 = 200.0
            // Flattened: [110.0, 280.0, 200.0]
            List<Double> expectedValues = List.of(110.0, 280.0, 200.0);
            assertThat(compResult.cumulativeValues()).containsExactlyElementsOf(expectedValues);
        }

        @Test
        @DisplayName("Should process multiple products correctly")
        void processMultipleProducts_returnsCorrectCumulativeForEach() throws IOException {
            // Arrange
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

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response.results()).hasSize(2);

            // Verify products are sorted alphabetically (TreeMap in service)
            assertThat(response.results().get(0).product()).isEqualTo("Comp");
            assertThat(response.results().get(1).product()).isEqualTo("Non-Comp");

            // Verify Comp cumulative values
            // Origin 1990: 0, 0, 0, 0 (no data for Comp in 1990)
            // Origin 1991: 0, 0, 0 (no data for Comp in 1991)
            // Origin 1992: 110.0, 280.0
            // Origin 1993: 200.0
            // Flattened: [0, 0, 0, 0, 0, 0, 0, 110.0, 280.0, 200.0]
            ProductResult compResult = response.results().get(0);
            assertThat(compResult.cumulativeValues())
                .containsExactly(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 110.0, 280.0, 200.0);

            // Verify Non-Comp cumulative values
            // Origin 1990: 45.2, 110.0, 110.0, 147.0
            // Origin 1991: 50.0, 125.0, 150.0
            // Origin 1992: 55.0, 140.0
            // Origin 1993: 100.0
            ProductResult nonCompResult = response.results().get(1);
            assertThat(nonCompResult.cumulativeValues())
                .containsExactly(45.2, 110.0, 110.0, 147.0, 50.0, 125.0, 150.0, 55.0, 140.0, 100.0);
        }

        @Test
        @DisplayName("Should handle missing incremental values as zero")
        void processClaimsWithMissingValues_treatsAsZero() throws IOException {
            // Arrange - Note: missing values in CSV are treated as 0 by ClaimsReader
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1990, 1990, 100.0
                Comp, 1990, 1992, 50.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            // Origin 1990: Year 1 = 100.0, Year 2 = 100.0 (no value for 1991), Year 3 = 150.0
            // Origin 1991: Year 1 = 0.0, Year 2 = 0.0 (no data for 1991)
            // Origin 1992: Year 1 = 0.0
            ProductResult result = response.results().get(0);
            assertThat(result.cumulativeValues())
                .containsExactly(100.0, 100.0, 150.0, 0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("Should process single record correctly")
        void processSingleRecord_returnsCorrectResult() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                TestProd, 2000, 2000, 500.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response.results()).hasSize(1);
            ProductResult result = response.results().get(0);
            assertThat(result.product()).isEqualTo("TestProd");
            assertThat(result.cumulativeValues()).containsExactly(500.0);
        }

        @Test
        @DisplayName("Should handle decimal precision correctly")
        void processClaimsWithDecimals_maintainsPrecision() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1990, 1990, 45.2
                Comp, 1990, 1991, 64.8
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            ProductResult result = response.results().get(0);
            assertThat(result.cumulativeValues().get(0)).isCloseTo(45.2, within(0.001));
            assertThat(result.cumulativeValues().get(1)).isCloseTo(110.0, within(0.001));
        }

        @Test
        @DisplayName("Should process InputStream correctly")
        void processInputStream_returnsValidResponse() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 100.0
                """;
            InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

            // Act
            ProcessingResultResponse response = service.processClaims(inputStream);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.results()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Metadata Generation")
    class MetadataGeneration {

        @Test
        @DisplayName("Should generate correct year range metadata")
        void processClaimsGeneratesCorrectYearRange() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1990, 1990, 100.0
                Comp, 1990, 1993, 50.0
                Comp, 1991, 1991, 75.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);
            ProcessingMetadata metadata = response.metadata();

            // Assert
            assertThat(metadata.earliestOriginYear()).isEqualTo(1990);
            assertThat(metadata.numberOfDevelopmentYears()).isEqualTo(4); // 1990 to 1993
        }

        @Test
        @DisplayName("Should generate correct record count")
        void processClaimsGeneratesCorrectRecordCount() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                Comp, 1992, 1993, 170.0
                Comp, 1993, 1993, 200.0
                Non-Comp, 1992, 1992, 55.0
                Non-Comp, 1992, 1993, 85.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response.metadata().totalRecords()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should list all products processed")
        void processClaimsListsAllProducts() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Alpha, 1992, 1992, 100.0
                Beta, 1992, 1992, 200.0
                Gamma, 1992, 1992, 300.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response.metadata().productsProcessed())
                .containsExactly("Alpha", "Beta", "Gamma");
        }

        @Test
        @DisplayName("Should record positive processing time")
        void processClaimsRecordsPositiveProcessingTime() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 100.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response.metadata().processingTimeMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should handle single year range correctly")
        void processClaimsWithSingleYear_calculatesCorrectRange() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 2020, 2020, 1000.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            ProcessingMetadata metadata = response.metadata();
            assertThat(metadata.earliestOriginYear()).isEqualTo(2020);
            assertThat(metadata.numberOfDevelopmentYears()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("CSV Output Generation")
    class CsvOutputGeneration {

        @Test
        @DisplayName("Should generate CSV output with correct header line")
        void generatesCsvOutputWithCorrectHeader() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1990, 1992, 100.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            String csvOutput = response.csvOutput();
            String[] lines = csvOutput.split("\n");
            assertThat(lines[0]).isEqualTo("1990, 3");
        }

        @Test
        @DisplayName("Should generate CSV output with product data lines")
        void generatesCsvOutputWithProductData() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 110.0
                Comp, 1992, 1993, 170.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            String csvOutput = response.csvOutput();
            assertThat(csvOutput).contains("Comp");
            assertThat(csvOutput).contains("110");
            assertThat(csvOutput).contains("280");
        }

        @Test
        @DisplayName("Should order products alphabetically in CSV output")
        void generatesCsvOutputWithAlphabeticalOrder() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Zebra, 1992, 1992, 100.0
                Alpha, 1992, 1992, 200.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            String csvOutput = response.csvOutput();
            int alphaPos = csvOutput.indexOf("Alpha");
            int zebraPos = csvOutput.indexOf("Zebra");
            assertThat(alphaPos).isLessThan(zebraPos);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw NullPointerException for null InputStream")
        void processNullInputStream_throwsNullPointerException() {
            // Act & Assert
            assertThatThrownBy(() -> service.processClaims((InputStream) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("InputStream cannot be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException for null String content")
        void processNullString_throwsNullPointerException() {
            // Act & Assert
            assertThatThrownBy(() -> service.processClaims((String) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("CSV content cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid CSV header")
        void processInvalidHeader_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                InvalidHeader, Wrong, Format
                Data, 1992, 100.0
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CSV header");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for missing header columns")
        void processMissingHeaderColumns_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year
                Comp, 1992
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CSV header");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for non-numeric origin year")
        void processNonNumericOriginYear_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, not-a-year, 1992, 100.0
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin Year");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for non-numeric development year")
        void processNonNumericDevYear_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, invalid, 100.0
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Development Year");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for non-numeric incremental value")
        void processNonNumericIncrementalValue_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1993, not-a-number
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incremental Value");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty CSV (header only)")
        void processEmptyCsv_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No claim records found");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for development year before origin year")
        void processDevYearBeforeOriginYear_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1993, 1992, 100.0
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Development year")
                .hasMessageContaining("cannot be before origin year");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty product name")
        void processEmptyProductName_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                , 1992, 1993, 100.0
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for wrong number of columns")
        void processWrongColumnCount_throwsIllegalArgumentException() {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1993
                """;

            // Act & Assert
            assertThatThrownBy(() -> service.processClaims(csvContent))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("columns");
        }
    }

    @Nested
    @DisplayName("Constructor Validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Should throw NullPointerException for null ClaimsReader")
        void constructWithNullReader_throwsNullPointerException() {
            // Act & Assert
            assertThatThrownBy(() -> new ClaimsProcessingService(null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ClaimsReader cannot be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException for null ClaimsWriter")
        void constructWithNullWriter_throwsNullPointerException() {
            // Arrange
            com.wtw.claims.reader.ClaimsReader reader = new com.wtw.claims.reader.ClaimsReader();

            // Act & Assert
            assertThatThrownBy(() -> new ClaimsProcessingService(reader, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ClaimsWriter cannot be null");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle whitespace in product names")
        void processWhitespaceInProductNames_trimsCorrectly() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                  Comp  , 1992, 1992, 100.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response.results().get(0).product()).isEqualTo("Comp");
        }

        @Test
        @DisplayName("Should handle empty incremental value as zero")
        void processEmptyIncrementalValue_treatsAsZero() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 100.0
                Comp, 1992, 1993,
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            // Origin 1992: 100.0, 100.0 (empty treated as 0)
            // Origin 1993: 0.0
            ProductResult result = response.results().get(0);
            assertThat(result.cumulativeValues()).containsExactly(100.0, 100.0, 0.0);
        }

        @Test
        @DisplayName("Should handle large year gaps correctly")
        void processLargeYearGaps_handlesCorrectly() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1990, 1990, 100.0
                Comp, 1990, 2000, 50.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            ProcessingMetadata metadata = response.metadata();
            assertThat(metadata.numberOfDevelopmentYears()).isEqualTo(11); // 1990 to 2000
        }

        @Test
        @DisplayName("Should handle negative incremental values")
        void processNegativeIncrementalValues_handlesCorrectly() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 100.0
                Comp, 1992, 1993, -50.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            ProductResult result = response.results().get(0);
            assertThat(result.cumulativeValues().get(0)).isEqualTo(100.0);
            assertThat(result.cumulativeValues().get(1)).isEqualTo(50.0); // 100 + (-50) = 50
        }

        @Test
        @DisplayName("Should handle very large incremental values")
        void processLargeIncrementalValues_handlesCorrectly() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                Comp, 1992, 1992, 1000000000.50
                Comp, 1992, 1993, 2000000000.25
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            ProductResult result = response.results().get(0);
            assertThat(result.cumulativeValues().get(0)).isCloseTo(1000000000.50, within(0.01));
            assertThat(result.cumulativeValues().get(1)).isCloseTo(3000000000.75, within(0.01));
        }

        @Test
        @DisplayName("Should handle products with special characters")
        void processProductsWithSpecialCharacters_handlesCorrectly() throws IOException {
            // Arrange
            String csvContent = """
                Product, Origin Year, Development Year, Incremental Value
                "Comp-A (Type-1)", 1992, 1992, 100.0
                """;

            // Act
            ProcessingResultResponse response = service.processClaims(csvContent);

            // Assert
            assertThat(response.results().get(0).product()).isEqualTo("Comp-A (Type-1)");
        }
    }
}
