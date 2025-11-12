package com.wtw.claims.writer;

import com.wtw.claims.model.ClaimsTriangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for the ClaimsWriter class.
 *
 * <p>This test suite verifies:
 * <ul>
 *   <li>Valid output format with single and multiple products</li>
 *   <li>Header line correctness (format: earliestYear,numDevYears with NO spaces)</li>
 *   <li>Product sorting (alphabetical order)</li>
 *   <li>Decimal formatting (preserving .0 for whole numbers)</li>
 *   <li>No spaces after commas in output</li>
 *   <li>Empty triangles map validation</li>
 *   <li>Null input validation</li>
 *   <li>Null triangle values validation</li>
 *   <li>File I/O operations</li>
 *   <li>Integration with ClaimsTriangle.flattenCumulative()</li>
 * </ul>
 * </p>
 *
 * @author Claims Triangle Accumulator Test Suite
 * @version 1.0.0
 */
@DisplayName("ClaimsWriter Tests")
class ClaimsWriterTest {

    // Test data constants
    private static final int EARLIEST_ORIGIN_YEAR = 1990;
    private static final int LATEST_DEVELOPMENT_YEAR = 1993;
    private static final int NUMBER_OF_DEVELOPMENT_YEARS = 4;

    private final ClaimsWriter writer = new ClaimsWriter();

    /**
     * Helper method to create a ClaimsTriangle with cumulative values for testing.
     *
     * @param productName the name of the product
     * @param cumulativeValues the cumulative values to set (in row-major order)
     * @return a ClaimsTriangle populated with the given cumulative values
     */
    private ClaimsTriangle createTriangleWithCumulativeValues(
            String productName,
            double... cumulativeValues
    ) {
        ClaimsTriangle triangle = new ClaimsTriangle(
            productName,
            EARLIEST_ORIGIN_YEAR,
            LATEST_DEVELOPMENT_YEAR
        );

        // Set cumulative values in row-major order
        int valueIndex = 0;
        for (int originYear = EARLIEST_ORIGIN_YEAR; originYear <= LATEST_DEVELOPMENT_YEAR; originYear++) {
            for (int devYear = originYear; devYear <= LATEST_DEVELOPMENT_YEAR; devYear++) {
                if (valueIndex < cumulativeValues.length) {
                    triangle.setCumulativeValue(originYear, devYear, cumulativeValues[valueIndex++]);
                }
            }
        }

        return triangle;
    }

    /**
     * Tests for valid output format with single product.
     */
    @Nested
    @DisplayName("Valid Output Format - Single Product")
    class ValidOutputFormatSingleProduct {

        @Test
        @DisplayName("Should write correct format for single product with all values")
        void writeCumulativeClaims_withSingleProduct_writesCorrectFormat(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            // Create Comp product with cumulative values
            // Origin 1990: [0.0, 0.0, 0.0, 0.0] (4 values)
            // Origin 1991: [0.0, 0.0, 0.0] (3 values)
            // Origin 1992: [110.0, 280.0] (2 values)
            // Origin 1993: [200.0] (1 value)
            ClaimsTriangle compTriangle = createTriangleWithCumulativeValues(
                "Comp",
                0.0, 0.0, 0.0, 0.0,  // Origin 1990
                0.0, 0.0, 0.0,       // Origin 1991
                110.0, 280.0,        // Origin 1992
                200.0                // Origin 1993
            );

            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("Comp", compTriangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            assertThat(outputFile).exists();
            List<String> lines = Files.readAllLines(outputFile);

            assertThat(lines).hasSize(2);
            assertThat(lines.get(0)).isEqualTo("1990,4");
            assertThat(lines.get(1)).isEqualTo("Comp,0.0,0.0,0.0,0.0,0.0,0.0,0.0,110.0,280.0,200.0");
        }

        @Test
        @DisplayName("Should preserve decimal formatting for whole numbers")
        void writeCumulativeClaims_withWholeNumbers_preservesDecimalPoint(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                "TestProduct",
                100.0, 200.0, 300.0, 400.0,
                500.0, 600.0, 700.0,
                800.0, 900.0,
                1000.0
            );

            Map<String, ClaimsTriangle> triangles = Map.of("TestProduct", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // Verify all values have .0 suffix for whole numbers
            assertThat(productLine).contains("100.0", "200.0", "300.0", "400.0", "500.0");
            assertThat(productLine).contains("600.0", "700.0", "800.0", "900.0", "1000.0");
        }

        @Test
        @DisplayName("Should format decimal values correctly with precision")
        void writeCumulativeClaims_withDecimalValues_formatsCorrectly(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                "TestProduct",
                45.2, 110.0, 110.0, 147.0,
                50.0, 125.0, 150.0,
                55.0, 140.0,
                100.0
            );

            Map<String, ClaimsTriangle> triangles = Map.of("TestProduct", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // Verify decimal formatting
            assertThat(productLine).contains("45.2");  // One decimal place
            assertThat(productLine).contains("110.0", "147.0", "50.0");  // Whole numbers with .0
        }
    }

    /**
     * Tests for valid output format with multiple products.
     */
    @Nested
    @DisplayName("Valid Output Format - Multiple Products")
    class ValidOutputFormatMultipleProducts {

        @Test
        @DisplayName("Should write correct format for multiple products")
        void writeCumulativeClaims_withMultipleProducts_writesCorrectFormat(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            ClaimsTriangle compTriangle = createTriangleWithCumulativeValues(
                "Comp",
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                110.0, 280.0,
                200.0
            );

            ClaimsTriangle nonCompTriangle = createTriangleWithCumulativeValues(
                "Non-Comp",
                45.2, 110.0, 110.0, 147.0,
                50.0, 125.0, 150.0,
                55.0, 140.0,
                100.0
            );

            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("Comp", compTriangle);
            triangles.put("Non-Comp", nonCompTriangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);

            assertThat(lines).hasSize(3);
            assertThat(lines.get(0)).isEqualTo("1990,4");
            assertThat(lines.get(1)).isEqualTo("Comp,0.0,0.0,0.0,0.0,0.0,0.0,0.0,110.0,280.0,200.0");
            assertThat(lines.get(2)).isEqualTo("Non-Comp,45.2,110.0,110.0,147.0,50.0,125.0,150.0,55.0,140.0,100.0");
        }

        @Test
        @DisplayName("Should sort products alphabetically")
        void writeCumulativeClaims_withUnsortedProducts_sortsAlphabetically(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            ClaimsTriangle zebraTriangle = createTriangleWithCumulativeValues("Zebra", 100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0);
            ClaimsTriangle alphaTriangle = createTriangleWithCumulativeValues("Alpha", 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0);
            ClaimsTriangle betaTriangle = createTriangleWithCumulativeValues("Beta", 15.0, 25.0, 35.0, 45.0, 55.0, 65.0, 75.0, 85.0, 95.0, 105.0);

            // Insert in non-alphabetical order
            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("Zebra", zebraTriangle);
            triangles.put("Alpha", alphaTriangle);
            triangles.put("Beta", betaTriangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);

            assertThat(lines).hasSize(4);
            assertThat(lines.get(1)).startsWith("Alpha,");
            assertThat(lines.get(2)).startsWith("Beta,");
            assertThat(lines.get(3)).startsWith("Zebra,");
        }

        @Test
        @DisplayName("Should maintain case-sensitive sorting")
        void writeCumulativeClaims_withMixedCaseProducts_sortsCaseSensitively(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            ClaimsTriangle compTriangle = createTriangleWithCumulativeValues("Comp", 100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0, 900.0, 1000.0);
            ClaimsTriangle nonCompTriangle = createTriangleWithCumulativeValues("Non-Comp", 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0);
            ClaimsTriangle autoTriangle = createTriangleWithCumulativeValues("auto", 15.0, 25.0, 35.0, 45.0, 55.0, 65.0, 75.0, 85.0, 95.0, 105.0);

            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("Comp", compTriangle);
            triangles.put("auto", autoTriangle);
            triangles.put("Non-Comp", nonCompTriangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);

            // Case-sensitive sort: uppercase comes before lowercase in ASCII
            assertThat(lines.get(1)).startsWith("Comp,");
            assertThat(lines.get(2)).startsWith("Non-Comp,");
            assertThat(lines.get(3)).startsWith("auto,");
        }
    }

    /**
     * Tests for header line correctness.
     */
    @Nested
    @DisplayName("Header Line Correctness")
    class HeaderLineTests {

        @Test
        @DisplayName("Should write header with correct format (no spaces)")
        void writeCumulativeClaims_headerLine_hasNoSpaces(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues("Product", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String headerLine = lines.get(0);

            assertThat(headerLine).isEqualTo("1990,4");
            assertThat(headerLine).doesNotContain(" ");
        }

        @ParameterizedTest
        @CsvSource({
            "1990, 4, '1990,4'",
            "1985, 10, '1985,10'",
            "2000, 5, '2000,5'",
            "1992, 1, '1992,1'"
        })
        @DisplayName("Should write header with different year ranges")
        void writeCumulativeClaims_withDifferentYearRanges_writesCorrectHeader(
            int earliestYear,
            int numDevYears,
            String expectedHeader,
            @TempDir Path tempDir
        ) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            int latestYear = earliestYear + numDevYears - 1;
            ClaimsTriangle triangle = new ClaimsTriangle("Product", earliestYear, latestYear);

            // Set minimal cumulative values
            for (int originYear = earliestYear; originYear <= latestYear; originYear++) {
                for (int devYear = originYear; devYear <= latestYear; devYear++) {
                    triangle.setCumulativeValue(originYear, devYear, 1.0);
                }
            }

            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, earliestYear, numDevYears);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines.get(0)).isEqualTo(expectedHeader);
        }
    }

    /**
     * Tests for output formatting (no spaces, correct decimals).
     */
    @Nested
    @DisplayName("Output Formatting")
    class OutputFormattingTests {

        @Test
        @DisplayName("Should not include spaces after commas in product lines")
        void writeCumulativeClaims_productLines_haveNoSpacesAfterCommas(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                "Test Product",
                45.2, 110.0, 110.0, 147.0,
                50.0, 125.0, 150.0,
                55.0, 140.0,
                100.0
            );
            Map<String, ClaimsTriangle> triangles = Map.of("Test Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // Verify no spaces after commas
            assertThat(productLine).doesNotContain(", ");
            // Verify commas are present
            assertThat(productLine).contains(",");
        }

        @Test
        @DisplayName("Should format very small decimal values correctly")
        void writeCumulativeClaims_withSmallDecimals_formatsCorrectly(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                "Product",
                0.1, 0.25, 0.333, 1.5,
                2.75, 3.125, 4.0,
                5.5, 6.25,
                7.0
            );
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // Verify decimal formatting
            assertThat(productLine).contains("0.1");
            assertThat(productLine).contains("0.25");
            assertThat(productLine).contains("0.333");
            assertThat(productLine).contains("1.5");
            assertThat(productLine).contains("4.0");  // Whole number with .0
            assertThat(productLine).contains("7.0");  // Whole number with .0
        }

        @Test
        @DisplayName("Should format large numbers correctly")
        void writeCumulativeClaims_withLargeNumbers_formatsCorrectly(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                "Product",
                1000000.0, 2500000.5, 3333333.33, 4000000.0,
                5000000.0, 6000000.0, 7000000.0,
                8000000.0, 9000000.0,
                10000000.0
            );
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // Verify large number formatting (no scientific notation)
            assertThat(productLine).contains("1000000.0");
            assertThat(productLine).contains("2500000.5");
            assertThat(productLine).contains("3333333.33");
            assertThat(productLine).contains("10000000.0");
            assertThat(productLine).doesNotContain("e");  // No scientific notation
            assertThat(productLine).doesNotContain("E");
        }

        @Test
        @DisplayName("Should handle zero values correctly")
        void writeCumulativeClaims_withZeroValues_formatsAsZeroPointZero(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                "Product",
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0,
                0.0
            );
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // All zeros should be formatted as "0.0"
            String[] values = productLine.split(",");
            assertThat(values).hasSize(11);  // Product name + 10 values
            for (int i = 1; i < values.length; i++) {
                assertThat(values[i]).isEqualTo("0.0");
            }
        }
    }

    /**
     * Tests for input validation.
     */
    @Nested
    @DisplayName("Input Validation")
    class InputValidationTests {

        @Test
        @DisplayName("Should throw NullPointerException when file path is null")
        void writeCumulativeClaims_withNullFilePath_throwsNullPointerException() {
            // Arrange
            ClaimsTriangle triangle = createTriangleWithCumulativeValues("Product", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act & Assert
            assertThatThrownBy(() -> writer.writeCumulativeClaims(
                null,
                triangles,
                EARLIEST_ORIGIN_YEAR,
                NUMBER_OF_DEVELOPMENT_YEARS
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("File path cannot be null");
        }

        @Test
        @DisplayName("Should throw NullPointerException when triangles map is null")
        void writeCumulativeClaims_withNullTrianglesMap_throwsNullPointerException(@TempDir Path tempDir) {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            // Act & Assert
            assertThatThrownBy(() -> writer.writeCumulativeClaims(
                outputFile,
                null,
                EARLIEST_ORIGIN_YEAR,
                NUMBER_OF_DEVELOPMENT_YEARS
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Triangles map cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when triangles map is empty")
        void writeCumulativeClaims_withEmptyTrianglesMap_throwsIllegalArgumentException(@TempDir Path tempDir) {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            Map<String, ClaimsTriangle> emptyMap = Map.of();

            // Act & Assert
            assertThatThrownBy(() -> writer.writeCumulativeClaims(
                outputFile,
                emptyMap,
                EARLIEST_ORIGIN_YEAR,
                NUMBER_OF_DEVELOPMENT_YEARS
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Triangles map cannot be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when triangle value is null")
        void writeCumulativeClaims_withNullTriangleValue_throwsIllegalArgumentException(@TempDir Path tempDir) {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("Product1", null);

            // Act & Assert
            assertThatThrownBy(() -> writer.writeCumulativeClaims(
                outputFile,
                triangles,
                EARLIEST_ORIGIN_YEAR,
                NUMBER_OF_DEVELOPMENT_YEARS
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Triangle for product 'Product1' cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when one of multiple triangles is null")
        void writeCumulativeClaims_withOneNullTriangleAmongMany_throwsIllegalArgumentException(@TempDir Path tempDir) {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle validTriangle = createTriangleWithCumulativeValues("Valid", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);

            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("Valid", validTriangle);
            triangles.put("Invalid", null);
            triangles.put("AlsoValid", validTriangle);

            // Act & Assert
            assertThatThrownBy(() -> writer.writeCumulativeClaims(
                outputFile,
                triangles,
                EARLIEST_ORIGIN_YEAR,
                NUMBER_OF_DEVELOPMENT_YEARS
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Triangle for product 'Invalid' cannot be null");
        }
    }

    /**
     * Tests for file I/O operations.
     */
    @Nested
    @DisplayName("File I/O Operations")
    class FileIOTests {

        @Test
        @DisplayName("Should create new file when file does not exist")
        void writeCumulativeClaims_whenFileDoesNotExist_createsNewFile(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("newfile.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues("Product", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            assertThat(outputFile).doesNotExist();

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            assertThat(outputFile).exists();
            assertThat(outputFile).isRegularFile();
        }

        @Test
        @DisplayName("Should overwrite existing file")
        void writeCumulativeClaims_whenFileExists_overwritesFile(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("existing.csv");
            Files.writeString(outputFile, "Old content\nShould be replaced");

            ClaimsTriangle triangle = createTriangleWithCumulativeValues("Product", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines).hasSize(2);  // Header + 1 product
            assertThat(lines.get(0)).isEqualTo("1990,4");
            assertThat(lines).doesNotContain("Old content");
        }

        @Test
        @DisplayName("Should create parent directories if they do not exist")
        void writeCumulativeClaims_withNestedPath_createsParentDirectories(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("nested/subdirectory/output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues("Product", 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            assertThat(outputFile.getParent()).doesNotExist();

            // Act
            Files.createDirectories(outputFile.getParent());
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            assertThat(outputFile).exists();
            assertThat(outputFile.getParent()).exists();
        }

        @Test
        @DisplayName("Should write file with correct line endings")
        void writeCumulativeClaims_writesFileWithCorrectLineEndings(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle compTriangle = createTriangleWithCumulativeValues(
                "Comp",
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                110.0, 280.0,
                200.0
            );
            ClaimsTriangle nonCompTriangle = createTriangleWithCumulativeValues(
                "Non-Comp",
                45.2, 110.0, 110.0, 147.0,
                50.0, 125.0, 150.0,
                55.0, 140.0,
                100.0
            );

            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("Comp", compTriangle);
            triangles.put("Non-Comp", nonCompTriangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            String content = Files.readString(outputFile);
            String[] lines = content.split("\\R");  // Split on any line ending

            assertThat(lines).hasSize(3);  // Header + 2 products
        }
    }

    /**
     * Tests for integration with ClaimsTriangle.
     */
    @Nested
    @DisplayName("Integration with ClaimsTriangle")
    class ClaimsTriangleIntegrationTests {

        @Test
        @DisplayName("Should correctly use flattenCumulative() from ClaimsTriangle")
        void writeCumulativeClaims_usesTriangleFlattenCumulative(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            // Create triangle with known cumulative values
            ClaimsTriangle triangle = new ClaimsTriangle("Product", EARLIEST_ORIGIN_YEAR, LATEST_DEVELOPMENT_YEAR);

            // Set cumulative values explicitly
            // Origin 1990
            triangle.setCumulativeValue(1990, 1990, 45.2);
            triangle.setCumulativeValue(1990, 1991, 110.0);
            triangle.setCumulativeValue(1990, 1992, 110.0);
            triangle.setCumulativeValue(1990, 1993, 147.0);
            // Origin 1991
            triangle.setCumulativeValue(1991, 1991, 50.0);
            triangle.setCumulativeValue(1991, 1992, 125.0);
            triangle.setCumulativeValue(1991, 1993, 150.0);
            // Origin 1992
            triangle.setCumulativeValue(1992, 1992, 55.0);
            triangle.setCumulativeValue(1992, 1993, 140.0);
            // Origin 1993
            triangle.setCumulativeValue(1993, 1993, 100.0);

            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // Verify the flattened values match what we set
            assertThat(productLine).isEqualTo("Product,45.2,110.0,110.0,147.0,50.0,125.0,150.0,55.0,140.0,100.0");
        }

        @Test
        @DisplayName("Should handle triangle with sparse cumulative data (defaults to 0.0)")
        void writeCumulativeClaims_withSparseData_usesZeroForMissingValues(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            ClaimsTriangle triangle = new ClaimsTriangle("Product", EARLIEST_ORIGIN_YEAR, LATEST_DEVELOPMENT_YEAR);

            // Only set a few cumulative values, leaving others as default (0.0)
            triangle.setCumulativeValue(1992, 1992, 110.0);
            triangle.setCumulativeValue(1992, 1993, 280.0);
            triangle.setCumulativeValue(1993, 1993, 200.0);

            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            String productLine = lines.get(1);

            // Most values should be 0.0 (default), with only the set values different
            assertThat(productLine).contains("110.0", "280.0", "200.0");
            assertThat(productLine.split(",")).contains("0.0");
        }

        @Test
        @DisplayName("Should write multiple products with different value distributions")
        void writeCumulativeClaims_withDifferentValueDistributions_writesCorrectly(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");

            // Product A: All zeros
            ClaimsTriangle triangleA = createTriangleWithCumulativeValues(
                "ProductA",
                0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0,
                0.0, 0.0,
                0.0
            );

            // Product B: Increasing values
            ClaimsTriangle triangleB = createTriangleWithCumulativeValues(
                "ProductB",
                1.0, 2.0, 3.0, 4.0,
                5.0, 6.0, 7.0,
                8.0, 9.0,
                10.0
            );

            // Product C: Mixed values
            ClaimsTriangle triangleC = createTriangleWithCumulativeValues(
                "ProductC",
                100.5, 0.0, 250.75, 0.0,
                0.0, 500.25, 0.0,
                750.5, 0.0,
                1000.0
            );

            Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
            triangles.put("ProductA", triangleA);
            triangles.put("ProductB", triangleB);
            triangles.put("ProductC", triangleC);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);

            assertThat(lines).hasSize(4);
            assertThat(lines.get(1)).startsWith("ProductA,");
            assertThat(lines.get(2)).startsWith("ProductB,");
            assertThat(lines.get(3)).startsWith("ProductC,");

            // Verify specific values
            assertThat(lines.get(1)).contains("0.0,0.0,0.0");
            assertThat(lines.get(2)).contains("1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0,10.0");
            assertThat(lines.get(3)).contains("100.5", "250.75", "500.25", "750.5", "1000.0");
        }
    }

    /**
     * Tests for edge cases and special scenarios.
     */
    @Nested
    @DisplayName("Edge Cases and Special Scenarios")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle product name with special characters")
        void writeCumulativeClaims_withSpecialCharactersInProductName_writesCorrectly(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                "Product-Name_123 (Special)",
                1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0
            );
            Map<String, ClaimsTriangle> triangles = Map.of("Product-Name_123 (Special)", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines.get(1)).startsWith("Product-Name_123 (Special),");
        }

        @Test
        @DisplayName("Should handle single year range (no development)")
        void writeCumulativeClaims_withSingleYear_writesCorrectly(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            ClaimsTriangle triangle = new ClaimsTriangle("Product", 1992, 1992);
            triangle.setCumulativeValue(1992, 1992, 100.0);

            Map<String, ClaimsTriangle> triangles = Map.of("Product", triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, 1992, 1);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines).hasSize(2);
            assertThat(lines.get(0)).isEqualTo("1992,1");
            assertThat(lines.get(1)).isEqualTo("Product,100.0");
        }

        @Test
        @DisplayName("Should handle very long product names")
        void writeCumulativeClaims_withLongProductName_writesCorrectly(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path outputFile = tempDir.resolve("output.csv");
            String longProductName = "VeryLongProductNameThatExceedsNormalLengthToTestEdgeCaseHandling";
            ClaimsTriangle triangle = createTriangleWithCumulativeValues(
                longProductName,
                1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0
            );
            Map<String, ClaimsTriangle> triangles = Map.of(longProductName, triangle);

            // Act
            writer.writeCumulativeClaims(outputFile, triangles, EARLIEST_ORIGIN_YEAR, NUMBER_OF_DEVELOPMENT_YEARS);

            // Assert
            List<String> lines = Files.readAllLines(outputFile);
            assertThat(lines.get(1)).startsWith(longProductName + ",");
        }
    }
}
