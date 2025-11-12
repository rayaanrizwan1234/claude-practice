package com.wtw.claims;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.within;

/**
 * End-to-end integration tests for the Claims Triangle Accumulator application.
 *
 * <p>These tests verify the complete processing pipeline from reading CSV input
 * to writing cumulative claims output. They test the integration of all components:
 * ClaimsReader, DataProcessor, TriangleBuilder, TriangleAccumulator, and ClaimsWriter.</p>
 *
 * <p><strong>Test Strategy:</strong></p>
 * <ul>
 *   <li>Use real file I/O with temporary directories for isolation</li>
 *   <li>Test with actual sample data files (problem.csv, cumulative_claims.csv)</li>
 *   <li>Verify end-to-end functionality, not individual components</li>
 *   <li>Cover success scenarios and various error conditions</li>
 * </ul>
 *
 * <p><strong>Note:</strong> These tests use package-private access to ClaimsApplication.processClaims()
 * to avoid the complexity of testing System.exit() behavior in the main() method. This test class
 * must remain in the same package (com.wtw.claims) as ClaimsApplication to access the package-private method.</p>
 *
 * @see ClaimsApplication
 */
@DisplayName("End-to-End Integration Tests for Claims Processing Pipeline")
class ClaimsApplicationIntegrationTest {

    /**
     * The path to the project files directory containing test data.
     * This path is resolved dynamically using the project root directory,
     * ensuring portability across different environments (developer machines, CI/CD, etc.).
     *
     * <p>Uses {@code user.dir} system property to find the project root,
     * then resolves the relative path to the files directory.</p>
     */
    private static final Path PROJECT_FILES_DIR =
        Paths.get(System.getProperty("user.dir"), "files");

    // ============================================================================
    // SUCCESS SCENARIOS
    // ============================================================================

    @Test
    @DisplayName("Should process problem.csv and produce output matching cumulative_claims.csv")
    void processRealSampleData_withProblemCsv_producesCorrectCumulativeOutput(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path inputFile = PROJECT_FILES_DIR.resolve("problem.csv");
        Path expectedOutputFile = PROJECT_FILES_DIR.resolve("cumulative_claims.csv");
        Path actualOutputFile = tempDir.resolve("output.csv");

        assertThat(inputFile).exists();
        assertThat(expectedOutputFile).exists();

        // Act
        ClaimsApplication.processClaims(inputFile, actualOutputFile);

        // Assert
        assertThat(actualOutputFile).exists();

        List<String> actualLines = Files.readAllLines(actualOutputFile);
        List<String> expectedLines = Files.readAllLines(expectedOutputFile);

        // Verify file structure: should have header + 2 products (Comp, Non-Comp)
        assertThat(actualLines)
            .as("Output should contain header line + 2 product lines")
            .hasSize(3);

        // Verify header line: earliest year and number of development years
        assertThat(actualLines.get(0))
            .as("Header should specify earliest origin year (1990) and 4 development years")
            .isEqualTo("1990,4");

        // Verify the header matches expected output
        assertThat(actualLines.get(0))
            .isEqualTo(expectedLines.get(0));

        // Verify product lines
        // Note: Our output uses "0.0" for zero values, expected uses "0"
        // Both are functionally equivalent, so we'll normalize for comparison
        String actualCompLine = normalizeZeros(actualLines.get(1));
        String expectedCompLine = normalizeZeros(expectedLines.get(1));
        String actualNonCompLine = normalizeZeros(actualLines.get(2));
        String expectedNonCompLine = normalizeZeros(expectedLines.get(2));

        assertThat(actualCompLine)
            .as("Comp product cumulative values should match expected output")
            .isEqualTo(expectedCompLine);

        assertThat(actualNonCompLine)
            .as("Non-Comp product cumulative values should match expected output")
            .isEqualTo(expectedNonCompLine);

        // Verify specific cumulative values for Comp product
        String[] compValues = actualLines.get(1).split(",");
        assertThat(compValues[0]).isEqualTo("Comp");
        // Years 1990-1991 should be 0 (product started in 1992)
        assertThat(compValues[1]).matches("0(\\.0)?");  // 1990 dev 1990
        assertThat(compValues[2]).matches("0(\\.0)?");  // 1990 dev 1991
        assertThat(compValues[3]).matches("0(\\.0)?");  // 1990 dev 1992
        assertThat(compValues[4]).matches("0(\\.0)?");  // 1990 dev 1993
        assertThat(compValues[5]).matches("0(\\.0)?");  // 1991 dev 1991
        assertThat(compValues[6]).matches("0(\\.0)?");  // 1991 dev 1992
        assertThat(compValues[7]).matches("0(\\.0)?");  // 1991 dev 1993
        // Year 1992: 110.0, then 280.0 (110 + 170)
        assertThat(Double.parseDouble(compValues[8])).isEqualTo(110.0);
        assertThat(Double.parseDouble(compValues[9])).isEqualTo(280.0);
        // Year 1993: 200.0
        assertThat(Double.parseDouble(compValues[10])).isEqualTo(200.0);

        // Verify specific cumulative values for Non-Comp product
        String[] nonCompValues = actualLines.get(2).split(",");
        assertThat(nonCompValues[0]).isEqualTo("Non-Comp");
        // 1990 origin: 45.2, 110.0 (45.2+64.8), 110.0 (no 1992 dev), 147.0 (110+37)
        assertThat(Double.parseDouble(nonCompValues[1])).isEqualTo(45.2);
        assertThat(Double.parseDouble(nonCompValues[2])).isEqualTo(110.0);
        assertThat(Double.parseDouble(nonCompValues[3])).isEqualTo(110.0);
        assertThat(Double.parseDouble(nonCompValues[4])).isEqualTo(147.0);
        // 1991 origin: 50.0, 125.0 (50+75), 150.0 (125+25)
        assertThat(Double.parseDouble(nonCompValues[5])).isEqualTo(50.0);
        assertThat(Double.parseDouble(nonCompValues[6])).isEqualTo(125.0);
        assertThat(Double.parseDouble(nonCompValues[7])).isEqualTo(150.0);
        // 1992 origin: 55.0, 140.0 (55+85)
        assertThat(Double.parseDouble(nonCompValues[8])).isEqualTo(55.0);
        assertThat(Double.parseDouble(nonCompValues[9])).isEqualTo(140.0);
        // 1993 origin: 100.0
        assertThat(Double.parseDouble(nonCompValues[10])).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should process single product correctly")
    void processSingleProduct_withCompOnly_producesCorrectOutput(@TempDir Path tempDir) throws IOException {
        // Arrange: Create input file with only Comp product
        Path inputFile = tempDir.resolve("comp_only.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992, 1992, 110.0\n" +
            "Comp, 1992, 1993, 170.0\n" +
            "Comp, 1993, 1993, 200.0\n";
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines)
            .as("Output should have header + 1 product line")
            .hasSize(2);

        // Header: earliest origin year is 1992, 2 development years (1992-1993)
        assertThat(lines.get(0))
            .as("Header should reflect 1992 as earliest year with 2 development years")
            .isEqualTo("1992,2");

        // Verify Comp product line
        String compLine = lines.get(1);
        assertThat(compLine)
            .as("Product line should start with 'Comp'")
            .startsWith("Comp,");

        String[] values = compLine.split(",");
        assertThat(values).hasSize(4); // Product + 3 cumulative values

        // Origin 1992: 110.0, 280.0 (110 + 170)
        assertThat(Double.parseDouble(values[1])).isEqualTo(110.0);
        assertThat(Double.parseDouble(values[2])).isEqualTo(280.0);
        // Origin 1993: 200.0
        assertThat(Double.parseDouble(values[3])).isEqualTo(200.0);
    }

    @Test
    @DisplayName("Should process single origin year correctly")
    void processSingleOriginYear_withOneYear_producesCorrectOutput(@TempDir Path tempDir) throws IOException {
        // Arrange: Create input file with claims from only one origin year
        Path inputFile = tempDir.resolve("single_year.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992, 1992, 100.0\n" +
            "Comp, 1992, 1993, 50.0\n" +
            "Comp, 1992, 1994, 25.0\n" +
            "Non-Comp, 1992, 1992, 200.0\n" +
            "Non-Comp, 1992, 1993, 150.0\n";
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines)
            .as("Output should have header + 2 product lines")
            .hasSize(3);

        // Header: only origin year 1992, 3 development years (1992-1994)
        assertThat(lines.get(0))
            .as("Header should show 1992 as earliest year with 3 development years")
            .isEqualTo("1992,3");

        // Verify Comp product cumulative values
        // Triangle structure for single origin year 1992 with 3 dev years:
        // Origin 1992: 100.0, 150.0 (100+50), 175.0 (150+25)
        String compLine = lines.stream().filter(l -> l.startsWith("Comp,")).findFirst().orElseThrow();
        String[] compValues = compLine.split(",");
        assertThat(compValues[0]).isEqualTo("Comp");
        assertThat(Double.parseDouble(compValues[1])).isEqualTo(100.0);
        assertThat(Double.parseDouble(compValues[2])).isEqualTo(150.0);
        assertThat(Double.parseDouble(compValues[3])).isEqualTo(175.0);

        // Verify Non-Comp product cumulative values
        // Note: The triangle builder creates a full triangle structure from earliest origin year
        // to latest development year, which may include zeros for years/products that don't have data
        String nonCompLine = lines.stream().filter(l -> l.startsWith("Non-Comp,")).findFirst().orElseThrow();
        String[] nonCompValues = nonCompLine.split(",");
        assertThat(nonCompValues[0]).isEqualTo("Non-Comp");

        // Based on actual output: ["Non-Comp", "200.0", "350.0", "350.0", "0.0", "0.0", "0.0"]
        // The first 3 values after product name are for origin year 1992 (dev years 1992, 1993, 1994)
        // The trailing zeros might be for potential future origin years in the triangle
        assertThat(Double.parseDouble(nonCompValues[1])).isEqualTo(200.0);  // 1992 origin, 1992 dev
        assertThat(Double.parseDouble(nonCompValues[2])).isEqualTo(350.0);  // 1992 origin, 1993 dev
        assertThat(Double.parseDouble(nonCompValues[3])).isCloseTo(350.0, within(0.01));  // 1992 origin, 1994 dev
    }

    @Test
    @DisplayName("Should handle missing development years by treating them as zero incremental")
    void processMissingDevelopmentYears_withGaps_treatsAsZeroIncremental(@TempDir Path tempDir) throws IOException {
        // Arrange: Create input with missing development years (gaps in the data)
        Path inputFile = tempDir.resolve("missing_dev_years.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1990, 1990, 100.0\n" +
            "Comp, 1990, 1993, 50.0\n";
        // Note: Missing 1991 and 1992 development years for origin 1990
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        String compLine = lines.get(1);
        String[] values = compLine.split(",");

        // Origin 1990: dev 1990=100, dev 1991=100 (0 added), dev 1992=100 (0 added), dev 1993=150 (50 added)
        assertThat(Double.parseDouble(values[1])).isEqualTo(100.0);  // 1990
        assertThat(Double.parseDouble(values[2])).isEqualTo(100.0);  // 1991 (missing, so cumulative stays 100)
        assertThat(Double.parseDouble(values[3])).isEqualTo(100.0);  // 1992 (missing, so cumulative stays 100)
        assertThat(Double.parseDouble(values[4])).isEqualTo(150.0);  // 1993 (100 + 50)
    }

    @Test
    @DisplayName("Should handle missing origin years by filling with zeros")
    void processMissingOriginYears_withGaps_fillsWithZeros(@TempDir Path tempDir) throws IOException {
        // Arrange: Create input with missing origin years
        Path inputFile = tempDir.resolve("missing_origin_years.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1990, 1990, 100.0\n" +
            "Comp, 1993, 1993, 200.0\n";
        // Note: Missing origin years 1991 and 1992
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines.get(0))
            .as("Header should show 1990 as earliest and 4 development years (1990-1993)")
            .isEqualTo("1990,4");

        String compLine = lines.get(1);
        String[] values = compLine.split(",");

        // Triangle structure (flattened):
        // Origin 1990: 4 values (dev 1990-1993)
        // Origin 1991: 3 values (dev 1991-1993) - all should be 0
        // Origin 1992: 2 values (dev 1992-1993) - all should be 0
        // Origin 1993: 1 value (dev 1993)
        assertThat(values).hasSize(11); // Product name + 10 cumulative values

        // Origin 1990 values
        assertThat(Double.parseDouble(values[1])).isEqualTo(100.0);
        assertThat(Double.parseDouble(values[2])).isEqualTo(100.0);
        assertThat(Double.parseDouble(values[3])).isEqualTo(100.0);
        assertThat(Double.parseDouble(values[4])).isEqualTo(100.0);

        // Origin 1991 values (missing year, should be 0)
        assertThat(Double.parseDouble(values[5])).isEqualTo(0.0);
        assertThat(Double.parseDouble(values[6])).isEqualTo(0.0);
        assertThat(Double.parseDouble(values[7])).isEqualTo(0.0);

        // Origin 1992 values (missing year, should be 0)
        assertThat(Double.parseDouble(values[8])).isEqualTo(0.0);
        assertThat(Double.parseDouble(values[9])).isEqualTo(0.0);

        // Origin 1993 value
        assertThat(Double.parseDouble(values[10])).isEqualTo(200.0);
    }

    @Test
    @DisplayName("Should process data with multiple products successfully")
    void processMultipleProducts_withVariousProducts_processesAllProducts(@TempDir Path tempDir) throws IOException {
        // Arrange: Create input with 3 products
        Path inputFile = tempDir.resolve("multiple_products.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992, 1992, 100.0\n" +
            "Non-Comp, 1992, 1992, 200.0\n" +
            "Auto, 1992, 1992, 150.0\n";
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines)
            .as("Output should have header + 3 product lines")
            .hasSize(4);

        // Verify all three products are present (order may vary due to HashMap iteration)
        String allProducts = String.join("\n", lines);
        assertThat(allProducts)
            .as("Output should contain all three products")
            .contains("Comp,")
            .contains("Non-Comp,")
            .contains("Auto,");

        // Verify each product line has the correct structure
        long productLineCount = lines.stream()
            .skip(1)  // Skip header
            .filter(line -> line.contains(","))
            .count();
        assertThat(productLineCount)
            .as("Should have exactly 3 product lines")
            .isEqualTo(3);
    }

    // ============================================================================
    // ERROR SCENARIOS
    // ============================================================================

    @Test
    @DisplayName("Should throw IllegalArgumentException when input file is empty")
    void processEmptyFile_withNoContent_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path inputFile = tempDir.resolve("empty.csv");
        Path outputFile = tempDir.resolve("output.csv");
        Files.writeString(inputFile, "");

        // Act & Assert
        // Empty file is caught by the CSV reader with "no header row" message
        assertThatThrownBy(() -> ClaimsApplication.processClaims(inputFile, outputFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("header");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when input file has only headers")
    void processHeaderOnlyFile_withNoData_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path inputFile = tempDir.resolve("headers_only.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n";
        Files.writeString(inputFile, inputData);

        // Act & Assert
        assertThatThrownBy(() -> ClaimsApplication.processClaims(inputFile, outputFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No claim records found");
    }

    @Test
    @DisplayName("Should throw IOException when input file does not exist")
    void processNonExistentFile_withInvalidPath_throwsIOException(@TempDir Path tempDir) {
        // Arrange
        Path inputFile = tempDir.resolve("non_existent.csv");
        Path outputFile = tempDir.resolve("output.csv");

        // Verify file doesn't exist
        assertThat(inputFile).doesNotExist();

        // Act & Assert
        assertThatThrownBy(() -> ClaimsApplication.processClaims(inputFile, outputFile))
            .isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Should throw exception when CSV has invalid format")
    void processInvalidCsvFormat_withMalformedData_throwsException(@TempDir Path tempDir) throws IOException {
        // Arrange: Create malformed CSV with wrong number of columns
        Path inputFile = tempDir.resolve("malformed.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992\n" +
            "Non-Comp, 1993, 1993, 100.0, ExtraColumn\n";
        Files.writeString(inputFile, inputData);

        // Act & Assert
        assertThatThrownBy(() -> ClaimsApplication.processClaims(inputFile, outputFile))
            .isInstanceOfAny(IllegalArgumentException.class, IllegalStateException.class);
    }

    @Test
    @DisplayName("Should throw exception when CSV has non-numeric year values")
    void processNonNumericYears_withInvalidYearData_throwsException(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path inputFile = tempDir.resolve("invalid_years.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, ABC, 1992, 110.0\n";
        Files.writeString(inputFile, inputData);

        // Act & Assert
        assertThatThrownBy(() -> ClaimsApplication.processClaims(inputFile, outputFile))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when CSV has non-numeric incremental values")
    void processNonNumericValues_withInvalidIncrementalData_throwsException(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path inputFile = tempDir.resolve("invalid_values.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992, 1992, NOT_A_NUMBER\n";
        Files.writeString(inputFile, inputData);

        // Act & Assert
        assertThatThrownBy(() -> ClaimsApplication.processClaims(inputFile, outputFile))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw exception when development year is before origin year")
    void processInvalidYearOrder_withDevBeforeOrigin_throwsException(@TempDir Path tempDir) throws IOException {
        // Arrange
        Path inputFile = tempDir.resolve("invalid_year_order.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992, 1990, 110.0\n";
        Files.writeString(inputFile, inputData);

        // Act & Assert
        assertThatThrownBy(() -> ClaimsApplication.processClaims(inputFile, outputFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Development year");
    }

    @Test
    @DisplayName("Should handle whitespace in CSV values correctly")
    void processWhitespaceInValues_withSpacesAroundData_trimsAndProcesses(@TempDir Path tempDir) throws IOException {
        // Arrange: CSV with extra whitespace
        Path inputFile = tempDir.resolve("whitespace.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp  , 1992 , 1992 , 110.0\n" +
            "Comp, 1992, 1993, 170.0\n";
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert - should process successfully despite whitespace
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        assertThat(lines).hasSize(2);  // Header + 1 product

        String compLine = lines.get(1);
        assertThat(compLine).startsWith("Comp,");

        String[] values = compLine.split(",");
        assertThat(Double.parseDouble(values[1])).isEqualTo(110.0);
        assertThat(Double.parseDouble(values[2])).isEqualTo(280.0);
    }

    @Test
    @DisplayName("Should handle negative incremental values correctly")
    void processNegativeValues_withNegativeIncremental_calculatesCorrectly(@TempDir Path tempDir) throws IOException {
        // Arrange: Include negative values (salvage/recoveries reduce claims)
        Path inputFile = tempDir.resolve("negative_values.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992, 1992, 100.0\n" +
            "Comp, 1992, 1993, -20.0\n" +
            "Comp, 1992, 1994, 50.0\n";
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        String compLine = lines.get(1);
        String[] values = compLine.split(",");

        // Cumulative: 100.0, 80.0 (100-20), 130.0 (80+50)
        assertThat(Double.parseDouble(values[1])).isEqualTo(100.0);
        assertThat(Double.parseDouble(values[2])).isEqualTo(80.0);
        assertThat(Double.parseDouble(values[3])).isEqualTo(130.0);
    }

    @Test
    @DisplayName("Should handle very small incremental values with proper precision")
    void processSmallValues_withDecimalPrecision_maintainsAccuracy(@TempDir Path tempDir) throws IOException {
        // Arrange: Use small decimal values to test precision
        Path inputFile = tempDir.resolve("small_values.csv");
        Path outputFile = tempDir.resolve("output.csv");

        String inputData = "Product, Origin Year, Development Year, Incremental Value\n" +
            "Comp, 1992, 1992, 0.01\n" +
            "Comp, 1992, 1993, 0.02\n" +
            "Comp, 1992, 1994, 0.03\n";
        Files.writeString(inputFile, inputData);

        // Act
        ClaimsApplication.processClaims(inputFile, outputFile);

        // Assert
        assertThat(outputFile).exists();

        List<String> lines = Files.readAllLines(outputFile);
        String compLine = lines.get(1);
        String[] values = compLine.split(",");

        // Cumulative: 0.01, 0.03 (0.01+0.02), 0.06 (0.03+0.03)
        assertThat(Double.parseDouble(values[1])).isEqualTo(0.01);
        assertThat(Double.parseDouble(values[2])).isEqualTo(0.03);
        assertThat(Double.parseDouble(values[3])).isEqualTo(0.06);
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Normalizes zero values in CSV lines for comparison.
     *
     * <p>Converts "0" to "0.0" to allow comparison between different formatting styles.
     * This is needed because our output uses "0.0" while the expected output uses "0".</p>
     *
     * @param line the CSV line to normalize
     * @return the normalized line with consistent zero formatting
     */
    private String normalizeZeros(String line) {
        return line.replaceAll("(?<=,)0(?=,|$)", "0.0");
    }
}
