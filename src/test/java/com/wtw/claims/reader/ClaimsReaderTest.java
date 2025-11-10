package com.wtw.claims.reader;

import com.wtw.claims.model.ClaimRecord;
import com.wtw.claims.validator.ClaimRecordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for ClaimsReader class.
 *
 * <p>This test suite covers:
 * <ul>
 *   <li>Constructor validation</li>
 *   <li>Valid CSV file parsing</li>
 *   <li>CSV header validation</li>
 *   <li>Empty incremental value handling (business rule: treat as 0.0)</li>
 *   <li>Malformed data handling</li>
 *   <li>Edge cases (empty files, null paths, missing files)</li>
 *   <li>Integration with ClaimRecordValidator</li>
 * </ul>
 *
 * <p>Test resource files used:
 * <ul>
 *   <li>valid_claims.csv - Properly formatted CSV with 4 valid records</li>
 *   <li>empty_incremental_values.csv - CSV with empty incremental values (should default to 0.0)</li>
 *   <li>invalid_header.csv - CSV with incorrect column headers</li>
 *   <li>malformed_data.csv - CSV with various data format errors</li>
 *   <li>empty.csv - Empty file (0 bytes)</li>
 * </ul>
 *
 * @author Claims Triangle Accumulator Test Team
 * @version 1.0.0
 */
@DisplayName("ClaimsReader Tests")
class ClaimsReaderTest {

    private ClaimsReader reader;
    private Path testResourcesPath;

    @BeforeEach
    void setUp() {
        reader = new ClaimsReader();
        testResourcesPath = Paths.get("src/test/resources");
    }

    // ========================================================================
    // Constructor Tests
    // ========================================================================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create ClaimsReader with default validator")
        void createClaimsReader_withDefaultConstructor_succeeds() {
            // Act
            ClaimsReader newReader = new ClaimsReader();

            // Assert
            assertThat(newReader).isNotNull();
        }

        @Test
        @DisplayName("Should create ClaimsReader with custom validator")
        void createClaimsReader_withCustomValidator_succeeds() {
            // Arrange
            ClaimRecordValidator customValidator = new ClaimRecordValidator();

            // Act
            ClaimsReader newReader = new ClaimsReader(customValidator);

            // Assert
            assertThat(newReader).isNotNull();
        }

        @Test
        @DisplayName("Should throw NullPointerException when validator is null")
        void createClaimsReader_withNullValidator_throwsNullPointerException() {
            // Act & Assert
            assertThatThrownBy(() -> new ClaimsReader(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Validator cannot be null");
        }
    }

    // ========================================================================
    // Valid CSV Parsing Tests
    // ========================================================================

    @Nested
    @DisplayName("Valid CSV Parsing Tests")
    class ValidCsvParsingTests {

        @Test
        @DisplayName("Should successfully read valid CSV file with all fields present")
        void readClaims_withValidCsv_returnsAllRecords() throws IOException {
            // Arrange
            Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");

            // Act
            List<ClaimRecord> records = reader.readClaims(validCsvPath);

            // Assert
            assertThat(records)
                .isNotNull()
                .hasSize(4)
                .extracting(ClaimRecord::getProduct)
                .containsExactly("Comp", "Comp", "Non-Comp", "Non-Comp");
        }

        @Test
        @DisplayName("Should parse first record correctly with all field values")
        void readClaims_withValidCsv_parsesFirstRecordCorrectly() throws IOException {
            // Arrange
            Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");

            // Act
            List<ClaimRecord> records = reader.readClaims(validCsvPath);

            // Assert
            ClaimRecord firstRecord = records.get(0);
            assertThat(firstRecord.getProduct()).isEqualTo("Comp");
            assertThat(firstRecord.getOriginYear()).isEqualTo(1992);
            assertThat(firstRecord.getDevelopmentYear()).isEqualTo(1992);
            assertThat(firstRecord.getIncrementalValue()).isEqualTo(110.0);
        }

        @Test
        @DisplayName("Should parse all numeric values correctly")
        void readClaims_withValidCsv_parsesAllNumericFieldsCorrectly() throws IOException {
            // Arrange
            Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");

            // Act
            List<ClaimRecord> records = reader.readClaims(validCsvPath);

            // Assert
            assertThat(records)
                .extracting(ClaimRecord::getOriginYear)
                .containsExactly(1992, 1992, 1990, 1990);

            assertThat(records)
                .extracting(ClaimRecord::getDevelopmentYear)
                .containsExactly(1992, 1993, 1990, 1991);

            assertThat(records)
                .extracting(ClaimRecord::getIncrementalValue)
                .containsExactly(110.0, 170.0, 45.2, 64.8);
        }

        @Test
        @DisplayName("Should trim whitespace from product names")
        void readClaims_withWhitespaceInProductNames_trimsCorrectly() throws IOException {
            // Arrange
            Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");

            // Act
            List<ClaimRecord> records = reader.readClaims(validCsvPath);

            // Assert - CSV parser already trims, but verify no leading/trailing spaces
            assertThat(records)
                .extracting(ClaimRecord::getProduct)
                .allMatch(product -> product.equals(product.trim()));
        }
    }

    // ========================================================================
    // Empty Incremental Value Tests (Business Rule)
    // ========================================================================

    @Nested
    @DisplayName("Empty Incremental Value Tests")
    class EmptyIncrementalValueTests {

        @Test
        @DisplayName("Should treat empty incremental value as 0.0 (business rule)")
        void readClaims_withEmptyIncrementalValue_defaultsToZero() throws IOException {
            // Arrange
            Path csvPath = testResourcesPath.resolve("empty_incremental_values.csv");

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(4);

            // Second record has empty incremental value
            ClaimRecord recordWithEmptyValue = records.get(1);
            assertThat(recordWithEmptyValue.getProduct()).isEqualTo("Comp");
            assertThat(recordWithEmptyValue.getOriginYear()).isEqualTo(1992);
            assertThat(recordWithEmptyValue.getDevelopmentYear()).isEqualTo(1993);
            assertThat(recordWithEmptyValue.getIncrementalValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle multiple empty incremental values in same file")
        void readClaims_withMultipleEmptyIncrementalValues_defaultsAllToZero() throws IOException {
            // Arrange
            Path csvPath = testResourcesPath.resolve("empty_incremental_values.csv");

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(4);

            // Records with empty incremental values (indices 1 and 2)
            assertThat(records.get(1).getIncrementalValue()).isEqualTo(0.0);
            assertThat(records.get(2).getIncrementalValue()).isEqualTo(0.0);

            // Records with explicit values remain unchanged
            assertThat(records.get(0).getIncrementalValue()).isEqualTo(110.0);
            assertThat(records.get(3).getIncrementalValue()).isEqualTo(64.8);
        }

        @Test
        @DisplayName("Should treat whitespace-only incremental value as 0.0")
        void readClaims_withWhitespaceOnlyIncrementalValue_defaultsToZero(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("whitespace_incremental.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992,    \n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getIncrementalValue()).isEqualTo(0.0);
        }
    }

    // ========================================================================
    // CSV Header Validation Tests
    // ========================================================================

    @Nested
    @DisplayName("CSV Header Validation Tests")
    class HeaderValidationTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid header column names")
        void readClaims_withInvalidHeaderNames_throwsIllegalArgumentException() {
            // Arrange
            Path invalidHeaderPath = testResourcesPath.resolve("invalid_header.csv");

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(invalidHeaderPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CSV header")
                .hasMessageContaining("expected")
                .hasMessageContaining("but found");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when header has wrong number of columns")
        void readClaims_withWrongNumberOfHeaderColumns_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("wrong_column_count.csv");
            String csvContent = "Product, Origin Year, Development Year\n" +
                               "Comp, 1992, 1992\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected 4 columns but found 3");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for missing header row")
        void readClaims_withMissingHeader_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("no_header.csv");
            Files.writeString(csvPath, "");

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no header row");
        }

        @Test
        @DisplayName("Should accept header with different case (case-insensitive)")
        void readClaims_withDifferentCaseHeader_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("case_insensitive_header.csv");
            String csvContent = "product, origin year, development year, incremental value\n" +
                               "Comp, 1992, 1992, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getProduct()).isEqualTo("Comp");
        }
    }

    // ========================================================================
    // Malformed Data Tests
    // ========================================================================

    @Nested
    @DisplayName("Malformed Data Tests")
    class MalformedDataTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException for non-numeric origin year")
        void readClaims_withNonNumericOriginYear_throwsIllegalArgumentException() {
            // Arrange
            Path malformedPath = testResourcesPath.resolve("malformed_data.csv");

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(malformedPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error parsing line")
                .hasMessageContaining("Invalid numeric value");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for incomplete row (missing columns)")
        void readClaims_withIncompleteRow_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("incomplete_row.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected 4 columns but found 2");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty product name")
        void readClaims_withEmptyProductName_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("empty_product.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               ", 1992, 1992, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name must not be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty origin year")
        void readClaims_withEmptyOriginYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("empty_origin_year.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, , 1992, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin Year cannot be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty development year")
        void readClaims_withEmptyDevelopmentYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("empty_dev_year.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, , 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Development Year cannot be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for non-numeric incremental value")
        void readClaims_withNonNumericIncrementalValue_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("non_numeric_incremental.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, NotANumber\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incremental Value must be a number");
        }

        @Test
        @DisplayName("Should include line number in error message for malformed data")
        void readClaims_withMalformedData_includesLineNumberInErrorMessage(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("error_on_line_3.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, 110.0\n" +
                               "Comp, ABC, 1993, 170.0\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Error parsing line 3");
        }
    }

    // ========================================================================
    // Empty File and Edge Case Tests
    // ========================================================================

    @Nested
    @DisplayName("Empty File and Edge Case Tests")
    class EmptyFileAndEdgeCaseTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException for empty file")
        void readClaims_withEmptyFile_throwsIllegalArgumentException() {
            // Arrange
            Path emptyPath = testResourcesPath.resolve("empty.csv");

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(emptyPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no header row");
        }

        @Test
        @DisplayName("Should return empty list for CSV with only header and no data rows")
        void readClaims_withOnlyHeaderNoData_returnsEmptyList(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("header_only.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).isEmpty();
        }

        @Test
        @DisplayName("Should throw NullPointerException when file path is null")
        void readClaims_withNullFilePath_throwsNullPointerException() {
            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("File path cannot be null");
        }

        @Test
        @DisplayName("Should throw IOException when file does not exist")
        void readClaims_withNonExistentFile_throwsIOException() {
            // Arrange
            Path nonExistentPath = testResourcesPath.resolve("does_not_exist.csv");

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(nonExistentPath))
                .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("Should handle single record CSV file")
        void readClaims_withSingleRecord_returnsOneRecord(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("single_record.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getProduct()).isEqualTo("Comp");
        }
    }

    // ========================================================================
    // Validator Integration Tests
    // ========================================================================

    @Nested
    @DisplayName("Validator Integration Tests")
    class ValidatorIntegrationTests {

        @Test
        @DisplayName("Should reject record when development year is before origin year")
        void readClaims_withDevelopmentYearBeforeOriginYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("invalid_dev_year.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1995, 1990, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Development year")
                .hasMessageContaining("cannot be before origin year");
        }

        @Test
        @DisplayName("Should reject record when origin year is below valid range")
        void readClaims_withOriginYearBelowRange_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("origin_year_too_low.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1800, 1800, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin year")
                .hasMessageContaining("outside valid range");
        }

        @Test
        @DisplayName("Should reject record when development year is above valid range")
        void readClaims_withDevelopmentYearAboveRange_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("dev_year_too_high.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 2000, 2150, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Development year")
                .hasMessageContaining("outside valid range");
        }

        @Test
        @DisplayName("Should reject record with infinite incremental value")
        void readClaims_withInfiniteIncrementalValue_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("infinite_value.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, Infinity\n";
            Files.writeString(csvPath, csvContent);

            // Act & Assert - Will fail at parsing stage, not validation
            assertThatThrownBy(() -> reader.readClaims(csvPath))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should accept record with negative incremental value (salvage recoveries)")
        void readClaims_withNegativeIncrementalValue_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("negative_value.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, -50.0\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getIncrementalValue()).isEqualTo(-50.0);
        }

        @Test
        @DisplayName("Should accept record with zero incremental value")
        void readClaims_withZeroIncrementalValue_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("zero_value.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, 0.0\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getIncrementalValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should validate all records in file with custom validator")
        void readClaims_withCustomValidator_validatesAllRecords(@TempDir Path tempDir) throws IOException {
            // Arrange
            ClaimRecordValidator customValidator = new ClaimRecordValidator();
            ClaimsReader customReader = new ClaimsReader(customValidator);

            Path csvPath = tempDir.resolve("multiple_records.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, 110.0\n" +
                               "Comp, 1992, 1993, 170.0\n" +
                               "Non-Comp, 1990, 1990, 45.2\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = customReader.readClaims(csvPath);

            // Assert - all records should pass validation
            assertThat(records).hasSize(3);
        }
    }

    // ========================================================================
    // Large Dataset and Performance Tests
    // ========================================================================

    @Nested
    @DisplayName("Large Dataset Tests")
    class LargeDatasetTests {

        @Test
        @DisplayName("Should handle CSV with many records efficiently")
        void readClaims_withLargeNumberOfRecords_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("large_dataset.csv");
            StringBuilder csvContent = new StringBuilder("Product, Origin Year, Development Year, Incremental Value\n");

            // Generate 1000 records
            for (int i = 0; i < 1000; i++) {
                csvContent.append(String.format("Product%d, 2000, 2001, %d.5\n", i % 10, i));
            }
            Files.writeString(csvPath, csvContent.toString());

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle CSV with very long product names")
        void readClaims_withLongProductNames_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("long_product_name.csv");
            String longProductName = "A".repeat(500); // 500 character product name
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               longProductName + ", 1992, 1992, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getProduct()).isEqualTo(longProductName);
        }

        @Test
        @DisplayName("Should handle CSV with many decimal places in incremental value")
        void readClaims_withHighPrecisionIncrementalValue_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("high_precision.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, 110.123456789123456789\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            // Note: Double precision will limit actual stored precision
            assertThat(records.get(0).getIncrementalValue()).isCloseTo(110.123456789, within(0.000001));
        }
    }

    // ========================================================================
    // Special Characters and Encoding Tests
    // ========================================================================

    @Nested
    @DisplayName("Special Characters and Encoding Tests")
    class SpecialCharactersTests {

        @Test
        @DisplayName("Should handle product names with spaces")
        void readClaims_withProductNamesContainingSpaces_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("spaces_in_product.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comprehensive Coverage, 1992, 1992, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getProduct()).isEqualTo("Comprehensive Coverage");
        }

        @Test
        @DisplayName("Should handle product names with hyphens")
        void readClaims_withProductNamesContainingHyphens_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("hyphens_in_product.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Non-Comp, 1992, 1992, 110.0\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getProduct()).isEqualTo("Non-Comp");
        }

        @Test
        @DisplayName("Should handle scientific notation in incremental value")
        void readClaims_withScientificNotation_succeeds(@TempDir Path tempDir) throws IOException {
            // Arrange
            Path csvPath = tempDir.resolve("scientific_notation.csv");
            String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                               "Comp, 1992, 1992, 1.1e2\n";
            Files.writeString(csvPath, csvContent);

            // Act
            List<ClaimRecord> records = reader.readClaims(csvPath);

            // Assert
            assertThat(records).hasSize(1);
            assertThat(records.get(0).getIncrementalValue()).isEqualTo(110.0);
        }
    }
}
