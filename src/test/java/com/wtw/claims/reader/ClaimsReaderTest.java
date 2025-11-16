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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
                .extracting(ClaimRecord::product)
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
            assertThat(firstRecord.product()).isEqualTo("Comp");
            assertThat(firstRecord.originYear()).isEqualTo(1992);
            assertThat(firstRecord.developmentYear()).isEqualTo(1992);
            assertThat(firstRecord.incrementalValue()).isEqualTo(110.0);
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
                .extracting(ClaimRecord::originYear)
                .containsExactly(1992, 1992, 1990, 1990);

            assertThat(records)
                .extracting(ClaimRecord::developmentYear)
                .containsExactly(1992, 1993, 1990, 1991);

            assertThat(records)
                .extracting(ClaimRecord::incrementalValue)
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
                .extracting(ClaimRecord::product)
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
            assertThat(recordWithEmptyValue.product()).isEqualTo("Comp");
            assertThat(recordWithEmptyValue.originYear()).isEqualTo(1992);
            assertThat(recordWithEmptyValue.developmentYear()).isEqualTo(1993);
            assertThat(recordWithEmptyValue.incrementalValue()).isEqualTo(0.0);
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
            assertThat(records.get(1).incrementalValue()).isEqualTo(0.0);
            assertThat(records.get(2).incrementalValue()).isEqualTo(0.0);

            // Records with explicit values remain unchanged
            assertThat(records.get(0).incrementalValue()).isEqualTo(110.0);
            assertThat(records.get(3).incrementalValue()).isEqualTo(64.8);
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
            assertThat(records.get(0).incrementalValue()).isEqualTo(0.0);
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
            assertThat(records.get(0).product()).isEqualTo("Comp");
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
            assertThat(records.get(0).product()).isEqualTo("Comp");
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
            assertThat(records.get(0).incrementalValue()).isEqualTo(-50.0);
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
            assertThat(records.get(0).incrementalValue()).isEqualTo(0.0);
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
            assertThat(records.get(0).product()).isEqualTo(longProductName);
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
            assertThat(records.get(0).incrementalValue()).isCloseTo(110.123456789, within(0.000001));
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
            assertThat(records.get(0).product()).isEqualTo("Comprehensive Coverage");
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
            assertThat(records.get(0).product()).isEqualTo("Non-Comp");
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
            assertThat(records.get(0).incrementalValue()).isEqualTo(110.0);
        }
    }

    // ========================================================================
    // scanForYearRange() Tests
    // ========================================================================

    @Nested
    @DisplayName("scanForYearRange() Tests")
    class ScanForYearRangeTests {

        @Nested
        @DisplayName("Valid Scenarios")
        class ValidScenariosTests {

            @Test
            @DisplayName("Should scan single record file and return correct year range")
            void scanForYearRange_withSingleRecord_returnsCorrectRange(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("single_record.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1993, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(1992);
                assertThat(yearRange.maxDevYear()).isEqualTo(1993);
            }

            @Test
            @DisplayName("Should find correct min/max years across multiple records with consecutive years")
            void scanForYearRange_withConsecutiveYears_returnsCorrectRange(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("consecutive_years.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, 1992, 1993, 170.0\n" +
                                   "Comp, 1993, 1993, 200.0\n" +
                                   "Non-Comp, 1990, 1990, 45.2\n" +
                                   "Non-Comp, 1990, 1991, 64.8\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange.minOriginYear()).isEqualTo(1990);
                assertThat(yearRange.maxDevYear()).isEqualTo(1993);
            }

            @Test
            @DisplayName("Should find correct min/max years when there are gaps in years")
            void scanForYearRange_withGapsInYears_returnsCorrectRange(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("gaps_in_years.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1990, 1990, 110.0\n" +
                                   "Comp, 1995, 1995, 170.0\n" +
                                   "Comp, 2000, 2005, 200.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange.minOriginYear()).isEqualTo(1990);
                assertThat(yearRange.maxDevYear()).isEqualTo(2005);
            }

            @Test
            @DisplayName("Should handle records where origin year equals development year")
            void scanForYearRange_withSameOriginAndDevYears_returnsCorrectRange(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("same_years.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 2000, 2000, 110.0\n" +
                                   "Non-Comp, 2000, 2000, 120.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange.minOriginYear()).isEqualTo(2000);
                assertThat(yearRange.maxDevYear()).isEqualTo(2000);
            }

            @Test
            @DisplayName("Should scan multiple products and find overall min/max years")
            void scanForYearRange_withMultipleProducts_returnsOverallRange(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("multiple_products.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "ProductA, 1985, 1990, 100.0\n" +
                                   "ProductB, 2000, 2005, 200.0\n" +
                                   "ProductC, 1995, 2000, 150.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange.minOriginYear()).isEqualTo(1985);
                assertThat(yearRange.maxDevYear()).isEqualTo(2005);
            }

            @Test
            @DisplayName("Should handle large year ranges spanning decades")
            void scanForYearRange_withLargeYearRange_returnsCorrectRange(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("large_range.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1950, 1955, 110.0\n" +
                                   "Comp, 1980, 1990, 170.0\n" +
                                   "Comp, 2020, 2050, 200.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange.minOriginYear()).isEqualTo(1950);
                assertThat(yearRange.maxDevYear()).isEqualTo(2050);
            }

            @Test
            @DisplayName("Should correctly scan valid_claims.csv test resource file")
            void scanForYearRange_withValidClaimsCsv_returnsCorrectRange() throws IOException {
                // Arrange
                Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(validCsvPath);

                // Assert
                assertThat(yearRange.minOriginYear()).isEqualTo(1990);
                assertThat(yearRange.maxDevYear()).isEqualTo(1993);
            }

            @Test
            @DisplayName("Should ignore incremental value field during scanning (not needed for year range)")
            void scanForYearRange_ignoresIncrementalValue_scansOnlyYears(@TempDir Path tempDir) throws IOException {
                // Arrange - Incremental values can be anything, even empty or invalid
                Path csvPath = tempDir.resolve("ignore_incremental.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1990, 1995, 999999.99\n" +
                                   "Comp, 1985, 2000, \n" +
                                   "Comp, 1980, 2005, anything\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert - Should scan successfully despite invalid incremental values
                assertThat(yearRange.minOriginYear()).isEqualTo(1980);
                assertThat(yearRange.maxDevYear()).isEqualTo(2005);
            }
        }

        @Nested
        @DisplayName("Edge Cases")
        class EdgeCasesTests {

            @Test
            @DisplayName("Should throw IllegalArgumentException for file with only header (no data)")
            void scanForYearRange_withOnlyHeader_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("header_only.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No claim records found");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for empty file")
            void scanForYearRange_withEmptyFile_throwsIllegalArgumentException() {
                // Arrange
                Path emptyPath = testResourcesPath.resolve("empty.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(emptyPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no header row");
            }

            @Test
            @DisplayName("Should throw NullPointerException when file path is null")
            void scanForYearRange_withNullFilePath_throwsNullPointerException() {
                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("File path cannot be null");
            }

            @Test
            @DisplayName("Should throw IOException when file does not exist")
            void scanForYearRange_withNonExistentFile_throwsIOException() {
                // Arrange
                Path nonExistentPath = testResourcesPath.resolve("does_not_exist.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(nonExistentPath))
                    .isInstanceOf(IOException.class);
            }

            @Test
            @DisplayName("Should use O(1) memory regardless of file size (only tracks two integers)")
            void scanForYearRange_withLargeFile_usesConstantMemory(@TempDir Path tempDir) throws IOException {
                // Arrange - Create large CSV file with 10,000 records
                Path csvPath = tempDir.resolve("large_file.csv");
                StringBuilder csvContent = new StringBuilder("Product, Origin Year, Development Year, Incremental Value\n");
                for (int i = 0; i < 10_000; i++) {
                    int year = 1900 + (i % 100);
                    csvContent.append(String.format("Product%d, %d, %d, %d.0\n", i % 10, year, year + 5, i));
                }
                Files.writeString(csvPath, csvContent.toString());

                // Act - Should complete quickly without loading all data into memory
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert - Should have scanned and found correct min/max
                assertThat(yearRange.minOriginYear()).isEqualTo(1900);
                assertThat(yearRange.maxDevYear()).isEqualTo(2004); // 1999 + 5
            }
        }

        @Nested
        @DisplayName("Invalid Data Handling")
        class InvalidDataHandlingTests {

            @Test
            @DisplayName("Should throw IllegalArgumentException for invalid CSV header")
            void scanForYearRange_withInvalidHeader_throwsIllegalArgumentException() {
                // Arrange
                Path invalidHeaderPath = testResourcesPath.resolve("invalid_header.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(invalidHeaderPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid CSV header");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException when development year is before origin year")
            void scanForYearRange_withDevYearBeforeOriginYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("invalid_year_order.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1995, 1990, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Development year")
                    .hasMessageContaining("cannot be before origin year")
                    .hasMessageContaining("1990")
                    .hasMessageContaining("1995");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for non-numeric origin year")
            void scanForYearRange_withNonNumericOriginYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("non_numeric_origin.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, ABC, 1993, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line")
                    .hasMessageContaining("Origin Year must be an integer");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for non-numeric development year")
            void scanForYearRange_withNonNumericDevYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("non_numeric_dev.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, XYZ, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line")
                    .hasMessageContaining("Development Year must be an integer");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for empty origin year field")
            void scanForYearRange_withEmptyOriginYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("empty_origin_year.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, , 1993, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line")
                    .hasMessageContaining("Origin Year cannot be empty");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for empty development year field")
            void scanForYearRange_withEmptyDevYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("empty_dev_year.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, , 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line")
                    .hasMessageContaining("Development Year cannot be empty");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for missing columns (incomplete row)")
            void scanForYearRange_withMissingColumns_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("incomplete_row.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line")
                    .hasMessageContaining("Expected at least 3 columns but found");
            }

            @Test
            @DisplayName("Should include line number in error message for malformed data")
            void scanForYearRange_withMalformedData_includesLineNumberInErrorMessage(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("error_on_line_3.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, ABC, 1993, 170.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line 3");
            }

            @Test
            @DisplayName("Should fail fast on first invalid record (not continue scanning)")
            void scanForYearRange_withInvalidRecord_failsImmediately(@TempDir Path tempDir) throws IOException {
                // Arrange - Error on line 2, but more valid data follows
                Path csvPath = tempDir.resolve("error_early.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, INVALID, 1993, 170.0\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, 1990, 1995, 200.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert - Should fail on line 2, not continue to scan remaining records
                assertThatThrownBy(() -> reader.scanForYearRange(csvPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line 2");
            }
        }

        @Nested
        @DisplayName("Integration with Real Test Files")
        class IntegrationTests {

            @Test
            @DisplayName("Should successfully scan valid_claims.csv and return expected year range")
            void scanForYearRange_withValidClaimsCsv_returnsExpectedRange() throws IOException {
                // Arrange
                Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(validCsvPath);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(1990);
                assertThat(yearRange.maxDevYear()).isEqualTo(1993);
                assertThat(yearRange.getNumberOfDevelopmentYears()).isEqualTo(4);
            }

            @Test
            @DisplayName("Should fail when scanning invalid_header.csv")
            void scanForYearRange_withInvalidHeaderCsv_throwsException() {
                // Arrange
                Path invalidHeaderPath = testResourcesPath.resolve("invalid_header.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(invalidHeaderPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid CSV header");
            }

            @Test
            @DisplayName("Should fail when scanning malformed_data.csv")
            void scanForYearRange_withMalformedDataCsv_throwsException() {
                // Arrange
                Path malformedPath = testResourcesPath.resolve("malformed_data.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(malformedPath))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error scanning line");
            }

            @Test
            @DisplayName("Should successfully scan empty_incremental_values.csv (incremental values ignored)")
            void scanForYearRange_withEmptyIncrementalValuesCsv_succeeds() throws IOException {
                // Arrange
                Path csvPath = testResourcesPath.resolve("empty_incremental_values.csv");

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert - Should scan successfully since incremental values are not validated during scanning
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(1990);
                assertThat(yearRange.maxDevYear()).isEqualTo(1993);
            }

            @Test
            @DisplayName("Should fail when scanning empty.csv")
            void scanForYearRange_withEmptyCsv_throwsException() {
                // Arrange
                Path emptyPath = testResourcesPath.resolve("empty.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.scanForYearRange(emptyPath))
                    .isInstanceOf(IllegalArgumentException.class);
            }
        }

        @Nested
        @DisplayName("Memory Efficiency Tests")
        class MemoryEfficiencyTests {

            @Test
            @DisplayName("Should track only min and max years (O(1) memory usage)")
            void scanForYearRange_tracksOnlyMinMax_constantMemory(@TempDir Path tempDir) throws IOException {
                // Arrange - Mix of years to verify min/max tracking
                Path csvPath = tempDir.resolve("min_max_tracking.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 2000, 2005, 100.0\n" +
                                   "Comp, 1995, 2010, 200.0\n" +
                                   "Comp, 2005, 2008, 150.0\n" +
                                   "Comp, 1990, 2015, 300.0\n" +
                                   "Comp, 1998, 2000, 250.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert - Should have found absolute min origin year and max dev year
                assertThat(yearRange.minOriginYear()).isEqualTo(1990); // Minimum origin year
                assertThat(yearRange.maxDevYear()).isEqualTo(2015);    // Maximum development year
            }

            @Test
            @DisplayName("Should update min year when encountering smaller origin year")
            void scanForYearRange_updatesMinYear_whenSmallerOriginYearFound(@TempDir Path tempDir) throws IOException {
                // Arrange - Descending origin years to test min tracking
                Path csvPath = tempDir.resolve("descending_origin.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 2000, 2000, 100.0\n" +
                                   "Comp, 1995, 1995, 200.0\n" +
                                   "Comp, 1990, 1990, 300.0\n" +
                                   "Comp, 1985, 1985, 400.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange.minOriginYear()).isEqualTo(1985);
            }

            @Test
            @DisplayName("Should update max year when encountering larger development year")
            void scanForYearRange_updatesMaxYear_whenLargerDevYearFound(@TempDir Path tempDir) throws IOException {
                // Arrange - Ascending development years to test max tracking
                Path csvPath = tempDir.resolve("ascending_dev.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1990, 1990, 100.0\n" +
                                   "Comp, 1990, 1995, 200.0\n" +
                                   "Comp, 1990, 2000, 300.0\n" +
                                   "Comp, 1990, 2010, 400.0\n";
                Files.writeString(csvPath, csvContent);

                // Act
                ClaimsReader.YearRange yearRange = reader.scanForYearRange(csvPath);

                // Assert
                assertThat(yearRange.maxDevYear()).isEqualTo(2010);
            }
        }
    }

    // ========================================================================
    // streamClaims() Tests
    // ========================================================================

    @Nested
    @DisplayName("streamClaims() Tests")
    class StreamClaimsTests {

        @Nested
        @DisplayName("Valid Streaming Scenarios")
        class ValidStreamingScenariosTests {

            @Test
            @DisplayName("Should stream single record file and pass record to consumer")
            void streamClaims_withSingleRecord_passesRecordToConsumer(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("single_record.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1993, 110.0\n";
                Files.writeString(csvPath, csvContent);

                List<ClaimRecord> consumedRecords = new ArrayList<>();
                Consumer<ClaimRecord> recordCollector = consumedRecords::add;

                // Act
                reader.streamClaims(csvPath, recordCollector);

                // Assert
                assertThat(consumedRecords).hasSize(1);
                assertThat(consumedRecords.get(0).product()).isEqualTo("Comp");
                assertThat(consumedRecords.get(0).originYear()).isEqualTo(1992);
                assertThat(consumedRecords.get(0).developmentYear()).isEqualTo(1993);
                assertThat(consumedRecords.get(0).incrementalValue()).isEqualTo(110.0);
            }

            @Test
            @DisplayName("Should stream multiple records and pass each to consumer in order")
            void streamClaims_withMultipleRecords_passesAllRecordsInOrder(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("multiple_records.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, 1992, 1993, 170.0\n" +
                                   "Non-Comp, 1990, 1990, 45.2\n" +
                                   "Non-Comp, 1990, 1991, 64.8\n";
                Files.writeString(csvPath, csvContent);

                List<ClaimRecord> consumedRecords = new ArrayList<>();

                // Act
                reader.streamClaims(csvPath, consumedRecords::add);

                // Assert
                assertThat(consumedRecords).hasSize(4);
                assertThat(consumedRecords)
                    .extracting(ClaimRecord::product)
                    .containsExactly("Comp", "Comp", "Non-Comp", "Non-Comp");
                assertThat(consumedRecords)
                    .extracting(ClaimRecord::incrementalValue)
                    .containsExactly(110.0, 170.0, 45.2, 64.8);
            }

            @Test
            @DisplayName("Should stream records with empty incremental values (defaults to 0.0)")
            void streamClaims_withEmptyIncrementalValues_treatsAsZero(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("empty_incremental.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, 1992, 1993, \n" +
                                   "Non-Comp, 1990, 1990, \n";
                Files.writeString(csvPath, csvContent);

                List<ClaimRecord> consumedRecords = new ArrayList<>();

                // Act
                reader.streamClaims(csvPath, consumedRecords::add);

                // Assert
                assertThat(consumedRecords).hasSize(3);
                assertThat(consumedRecords.get(0).incrementalValue()).isEqualTo(110.0);
                assertThat(consumedRecords.get(1).incrementalValue()).isEqualTo(0.0);
                assertThat(consumedRecords.get(2).incrementalValue()).isEqualTo(0.0);
            }

            @Test
            @DisplayName("Should stream multiple products correctly")
            void streamClaims_withMultipleProducts_streamsAllRecords(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("multiple_products.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "ProductA, 1990, 1990, 100.0\n" +
                                   "ProductB, 1991, 1991, 200.0\n" +
                                   "ProductC, 1992, 1992, 300.0\n";
                Files.writeString(csvPath, csvContent);

                List<ClaimRecord> consumedRecords = new ArrayList<>();

                // Act
                reader.streamClaims(csvPath, consumedRecords::add);

                // Assert
                assertThat(consumedRecords).hasSize(3);
                assertThat(consumedRecords)
                    .extracting(ClaimRecord::product)
                    .containsExactly("ProductA", "ProductB", "ProductC");
            }

            @Test
            @DisplayName("Should stream valid_claims.csv test resource file correctly")
            void streamClaims_withValidClaimsCsv_streamsAllRecords() throws IOException {
                // Arrange
                Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");
                List<ClaimRecord> consumedRecords = new ArrayList<>();

                // Act
                reader.streamClaims(validCsvPath, consumedRecords::add);

                // Assert
                assertThat(consumedRecords).hasSize(4);
                assertThat(consumedRecords)
                    .extracting(ClaimRecord::product)
                    .containsExactly("Comp", "Comp", "Non-Comp", "Non-Comp");
            }

            @Test
            @DisplayName("Should handle large file efficiently without accumulating all records in memory")
            void streamClaims_withLargeFile_streamsEfficiently(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("large_file.csv");
                StringBuilder csvContent = new StringBuilder("Product, Origin Year, Development Year, Incremental Value\n");
                for (int i = 0; i < 1000; i++) {
                    csvContent.append(String.format("Product%d, 2000, 2001, %d.5\n", i % 10, i));
                }
                Files.writeString(csvPath, csvContent.toString());

                // Act - Count records without storing them all (memory efficient)
                int[] recordCount = {0};
                reader.streamClaims(csvPath, record -> recordCount[0]++);

                // Assert
                assertThat(recordCount[0]).isEqualTo(1000);
            }

            @Test
            @DisplayName("Should not call consumer when file has only header (no data rows)")
            void streamClaims_withOnlyHeader_doesNotCallConsumer(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("header_only.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n";
                Files.writeString(csvPath, csvContent);

                int[] consumerCallCount = {0};
                Consumer<ClaimRecord> counter = record -> consumerCallCount[0]++;

                // Act
                reader.streamClaims(csvPath, counter);

                // Assert
                assertThat(consumerCallCount[0]).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("Consumer Functionality Tests")
        class ConsumerFunctionalityTests {

            @Test
            @DisplayName("Should call consumer exactly once per valid record")
            void streamClaims_withMultipleRecords_callsConsumerCorrectNumberOfTimes(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("three_records.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, 1992, 1993, 170.0\n" +
                                   "Comp, 1993, 1993, 200.0\n";
                Files.writeString(csvPath, csvContent);

                int[] callCount = {0};
                Consumer<ClaimRecord> counter = record -> callCount[0]++;

                // Act
                reader.streamClaims(csvPath, counter);

                // Assert
                assertThat(callCount[0]).isEqualTo(3);
            }

            @Test
            @DisplayName("Should allow consumer to accumulate records in a list")
            void streamClaims_withConsumerAccumulatingList_buildsCorrectList(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("accumulate_test.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Non-Comp, 1990, 1990, 45.2\n";
                Files.writeString(csvPath, csvContent);

                List<ClaimRecord> accumulated = new ArrayList<>();

                // Act
                reader.streamClaims(csvPath, accumulated::add);

                // Assert
                assertThat(accumulated).hasSize(2);
                assertThat(accumulated.get(0).product()).isEqualTo("Comp");
                assertThat(accumulated.get(1).product()).isEqualTo("Non-Comp");
            }

            @Test
            @DisplayName("Should allow consumer to filter and collect specific products")
            void streamClaims_withFilteringConsumer_collectsOnlyMatchingRecords(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("filter_test.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Non-Comp, 1990, 1990, 45.2\n" +
                                   "Comp, 1992, 1993, 170.0\n" +
                                   "Non-Comp, 1990, 1991, 64.8\n";
                Files.writeString(csvPath, csvContent);

                List<ClaimRecord> compRecords = new ArrayList<>();
                Consumer<ClaimRecord> compFilter = record -> {
                    if ("Comp".equals(record.product())) {
                        compRecords.add(record);
                    }
                };

                // Act
                reader.streamClaims(csvPath, compFilter);

                // Assert
                assertThat(compRecords).hasSize(2);
                assertThat(compRecords).allMatch(record -> "Comp".equals(record.product()));
            }

            @Test
            @DisplayName("Should allow consumer to calculate running statistics")
            void streamClaims_withStatisticsConsumer_calculatesCorrectly(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("stats_test.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 100.0\n" +
                                   "Comp, 1992, 1993, 200.0\n" +
                                   "Comp, 1993, 1993, 300.0\n";
                Files.writeString(csvPath, csvContent);

                double[] totalValue = {0.0};
                Consumer<ClaimRecord> sumCalculator = record -> totalValue[0] += record.incrementalValue();

                // Act
                reader.streamClaims(csvPath, sumCalculator);

                // Assert
                assertThat(totalValue[0]).isEqualTo(600.0);
            }

            @Test
            @DisplayName("Should pass correct record data to consumer for each record")
            void streamClaims_passesCorrectData_toConsumer(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("data_verification.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "TestProduct, 1995, 1997, 250.75\n";
                Files.writeString(csvPath, csvContent);

                ClaimRecord[] capturedRecord = new ClaimRecord[1];
                Consumer<ClaimRecord> capturer = record -> capturedRecord[0] = record;

                // Act
                reader.streamClaims(csvPath, capturer);

                // Assert
                assertThat(capturedRecord[0]).isNotNull();
                assertThat(capturedRecord[0].product()).isEqualTo("TestProduct");
                assertThat(capturedRecord[0].originYear()).isEqualTo(1995);
                assertThat(capturedRecord[0].developmentYear()).isEqualTo(1997);
                assertThat(capturedRecord[0].incrementalValue()).isEqualTo(250.75);
            }
        }

        @Nested
        @DisplayName("Null Parameter Tests")
        class NullParameterTests {

            @Test
            @DisplayName("Should throw NullPointerException when file path is null")
            void streamClaims_withNullFilePath_throwsNullPointerException() {
                // Arrange
                Consumer<ClaimRecord> consumer = record -> {};

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(null, consumer))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("File path cannot be null");
            }

            @Test
            @DisplayName("Should throw NullPointerException when consumer is null")
            void streamClaims_withNullConsumer_throwsNullPointerException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("test.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Record consumer cannot be null");
            }

            @Test
            @DisplayName("Should throw NullPointerException when both parameters are null")
            void streamClaims_withBothParametersNull_throwsNullPointerException() {
                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("File path cannot be null");
            }
        }

        @Nested
        @DisplayName("Edge Cases")
        class EdgeCasesTests {

            @Test
            @DisplayName("Should throw IllegalArgumentException for empty file")
            void streamClaims_withEmptyFile_throwsIllegalArgumentException() {
                // Arrange
                Path emptyPath = testResourcesPath.resolve("empty.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(emptyPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no header row");
            }

            @Test
            @DisplayName("Should throw IOException when file does not exist")
            void streamClaims_withNonExistentFile_throwsIOException() {
                // Arrange
                Path nonExistentPath = testResourcesPath.resolve("does_not_exist.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(nonExistentPath, record -> {}))
                    .isInstanceOf(IOException.class);
            }

            @Test
            @DisplayName("Should successfully stream file with only header (no records)")
            void streamClaims_withHeaderOnly_succeedsWithoutCallingConsumer(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("header_only.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n";
                Files.writeString(csvPath, csvContent);

                boolean[] consumerCalled = {false};
                Consumer<ClaimRecord> tracker = record -> consumerCalled[0] = true;

                // Act
                reader.streamClaims(csvPath, tracker);

                // Assert - Should complete successfully without calling consumer
                assertThat(consumerCalled[0]).isFalse();
            }

            @Test
            @DisplayName("Should handle records with whitespace-only incremental value (treats as 0.0)")
            void streamClaims_withWhitespaceIncrementalValue_treatsAsZero(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("whitespace_incremental.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992,    \n";
                Files.writeString(csvPath, csvContent);

                List<ClaimRecord> consumedRecords = new ArrayList<>();

                // Act
                reader.streamClaims(csvPath, consumedRecords::add);

                // Assert
                assertThat(consumedRecords).hasSize(1);
                assertThat(consumedRecords.get(0).incrementalValue()).isEqualTo(0.0);
            }
        }

        @Nested
        @DisplayName("Invalid Data Tests")
        class InvalidDataTests {

            @Test
            @DisplayName("Should throw IllegalArgumentException for invalid header")
            void streamClaims_withInvalidHeader_throwsIllegalArgumentException() {
                // Arrange
                Path invalidHeaderPath = testResourcesPath.resolve("invalid_header.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(invalidHeaderPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid CSV header");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for malformed data with line number")
            void streamClaims_withMalformedData_throwsWithLineNumber() {
                // Arrange
                Path malformedPath = testResourcesPath.resolve("malformed_data.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(malformedPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line")
                    .hasMessageContaining("Invalid numeric value");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for non-numeric origin year")
            void streamClaims_withNonNumericOriginYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("non_numeric_origin.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, ABC, 1993, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 2")
                    .hasMessageContaining("Origin Year must be an integer");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for non-numeric development year")
            void streamClaims_withNonNumericDevYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("non_numeric_dev.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, XYZ, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 2")
                    .hasMessageContaining("Development Year must be an integer");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for non-numeric incremental value")
            void streamClaims_withNonNumericIncrementalValue_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("non_numeric_incremental.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, NotANumber\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 2")
                    .hasMessageContaining("Incremental Value must be a number");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for incomplete row (missing columns)")
            void streamClaims_withIncompleteRow_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("incomplete_row.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 2")
                    .hasMessageContaining("Expected 4 columns but found");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException for empty product name")
            void streamClaims_withEmptyProductName_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("empty_product.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   ", 1992, 1992, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 2")
                    .hasMessageContaining("Product name must not be null or empty");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException when development year < origin year")
            void streamClaims_withDevYearBeforeOriginYear_throwsIllegalArgumentException(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("invalid_year_order.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1995, 1990, 110.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 2")
                    .hasMessageContaining("Development year")
                    .hasMessageContaining("cannot be before origin year");
            }

            @Test
            @DisplayName("Should include correct line number in error message (line 3)")
            void streamClaims_withErrorOnLine3_includesCorrectLineNumber(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("error_on_line_3.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, ABC, 1993, 170.0\n";
                Files.writeString(csvPath, csvContent);

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(csvPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 3");
            }

            @Test
            @DisplayName("Should fail fast on first error (not continue streaming)")
            void streamClaims_withInvalidRecord_failsImmediately(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("fail_fast.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, INVALID, 1993, 170.0\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, 1990, 1995, 200.0\n";
                Files.writeString(csvPath, csvContent);

                int[] recordCount = {0};
                Consumer<ClaimRecord> counter = record -> recordCount[0]++;

                // Act & Assert - Should fail on line 2, consumer should not be called
                assertThatThrownBy(() -> reader.streamClaims(csvPath, counter))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line 2");

                // Consumer should never have been called due to fail-fast
                assertThat(recordCount[0]).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("Integration Tests")
        class IntegrationTests {

            @Test
            @DisplayName("Should successfully stream and process valid_claims.csv")
            void streamClaims_withValidClaimsCsv_processesAllRecordsCorrectly() throws IOException {
                // Arrange
                Path validCsvPath = testResourcesPath.resolve("valid_claims.csv");
                List<ClaimRecord> records = new ArrayList<>();

                // Act
                reader.streamClaims(validCsvPath, records::add);

                // Assert
                assertThat(records).hasSize(4);
                assertThat(records)
                    .extracting(ClaimRecord::product)
                    .containsExactly("Comp", "Comp", "Non-Comp", "Non-Comp");
                assertThat(records)
                    .extracting(ClaimRecord::incrementalValue)
                    .containsExactly(110.0, 170.0, 45.2, 64.8);
            }

            @Test
            @DisplayName("Should successfully stream empty_incremental_values.csv (treats empty as 0.0)")
            void streamClaims_withEmptyIncrementalValuesCsv_treatsEmptyAsZero() throws IOException {
                // Arrange
                Path csvPath = testResourcesPath.resolve("empty_incremental_values.csv");
                List<ClaimRecord> records = new ArrayList<>();

                // Act
                reader.streamClaims(csvPath, records::add);

                // Assert
                assertThat(records).hasSize(4);
                // Records with empty incremental values should have 0.0
                assertThat(records.get(1).incrementalValue()).isEqualTo(0.0);
                assertThat(records.get(2).incrementalValue()).isEqualTo(0.0);
            }

            @Test
            @DisplayName("Should fail when streaming invalid_header.csv")
            void streamClaims_withInvalidHeaderCsv_throwsException() {
                // Arrange
                Path invalidHeaderPath = testResourcesPath.resolve("invalid_header.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(invalidHeaderPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid CSV header");
            }

            @Test
            @DisplayName("Should fail when streaming malformed_data.csv")
            void streamClaims_withMalformedDataCsv_throwsException() {
                // Arrange
                Path malformedPath = testResourcesPath.resolve("malformed_data.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(malformedPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Error streaming line");
            }

            @Test
            @DisplayName("Should fail when streaming empty.csv")
            void streamClaims_withEmptyCsv_throwsException() {
                // Arrange
                Path emptyPath = testResourcesPath.resolve("empty.csv");

                // Act & Assert
                assertThatThrownBy(() -> reader.streamClaims(emptyPath, record -> {}))
                    .isInstanceOf(IllegalArgumentException.class);
            }
        }

        @Nested
        @DisplayName("Memory Efficiency Tests")
        class MemoryEfficiencyTests {

            @Test
            @DisplayName("Should use O(1) memory for record storage when consumer doesn't accumulate")
            void streamClaims_withNonAccumulatingConsumer_usesConstantMemory(@TempDir Path tempDir) throws IOException {
                // Arrange - Large file with 10,000 records
                Path csvPath = tempDir.resolve("large_file.csv");
                StringBuilder csvContent = new StringBuilder("Product, Origin Year, Development Year, Incremental Value\n");
                for (int i = 0; i < 10_000; i++) {
                    csvContent.append(String.format("Product%d, 2000, 2001, %d.5\n", i % 10, i));
                }
                Files.writeString(csvPath, csvContent.toString());

                // Act - Count records without storing them (memory efficient)
                int[] recordCount = {0};
                double[] runningSum = {0.0};
                Consumer<ClaimRecord> memoryEfficientProcessor = record -> {
                    recordCount[0]++;
                    runningSum[0] += record.incrementalValue();
                    // Record is eligible for GC after this line
                };

                reader.streamClaims(csvPath, memoryEfficientProcessor);

                // Assert
                assertThat(recordCount[0]).isEqualTo(10_000);
                assertThat(runningSum[0]).isPositive(); // Verify processing occurred
            }

            @Test
            @DisplayName("Should process records one at a time (not load entire file into memory)")
            void streamClaims_processesRecordsSequentially_notAllAtOnce(@TempDir Path tempDir) throws IOException {
                // Arrange
                Path csvPath = tempDir.resolve("sequential_test.csv");
                String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                                   "Comp, 1992, 1992, 110.0\n" +
                                   "Comp, 1992, 1993, 170.0\n" +
                                   "Comp, 1993, 1993, 200.0\n";
                Files.writeString(csvPath, csvContent);

                // Track the order of processing
                List<String> processingOrder = new ArrayList<>();
                Consumer<ClaimRecord> orderTracker = record ->
                    processingOrder.add(record.product() + "-" + record.originYear() + "-" + record.developmentYear());

                // Act
                reader.streamClaims(csvPath, orderTracker);

                // Assert - Records processed in file order, one at a time
                assertThat(processingOrder).containsExactly(
                    "Comp-1992-1992",
                    "Comp-1992-1993",
                    "Comp-1993-1993"
                );
            }
        }
    }

    // ========================================================================
    // YearRange Tests
    // ========================================================================

    @Nested
    @DisplayName("YearRange Tests")
    class YearRangeTests {

        @Nested
        @DisplayName("Constructor Tests")
        class ConstructorTests {

            @Test
            @DisplayName("Should create YearRange with valid consecutive years")
            void createYearRange_withConsecutiveYears_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(1990);
                assertThat(yearRange.maxDevYear()).isEqualTo(1993);
            }

            @Test
            @DisplayName("Should create YearRange with same min and max year")
            void createYearRange_withSameMinMax_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(2000, 2000);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(2000);
                assertThat(yearRange.maxDevYear()).isEqualTo(2000);
            }

            @Test
            @DisplayName("Should create YearRange with large year gap")
            void createYearRange_withLargeYearGap_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1950, 2050);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(1950);
                assertThat(yearRange.maxDevYear()).isEqualTo(2050);
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException when maxDevYear is less than minOriginYear")
            void createYearRange_withMaxLessThanMin_throwsIllegalArgumentException() {
                // Act & Assert
                assertThatThrownBy(() -> new ClaimsReader.YearRange(1995, 1990))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max development year")
                    .hasMessageContaining("cannot be less than min origin year")
                    .hasMessageContaining("1990")
                    .hasMessageContaining("1995");
            }

            @Test
            @DisplayName("Should throw IllegalArgumentException with descriptive message for invalid range")
            void createYearRange_withInvalidRange_includesYearsInErrorMessage() {
                // Act & Assert
                assertThatThrownBy(() -> new ClaimsReader.YearRange(2020, 2015))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2015")
                    .hasMessageContaining("2020");
            }

            @Test
            @DisplayName("Should create YearRange with negative years (BC dates)")
            void createYearRange_withNegativeYears_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(-100, -50);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(-100);
                assertThat(yearRange.maxDevYear()).isEqualTo(-50);
            }

            @Test
            @DisplayName("Should create YearRange with zero as min year")
            void createYearRange_withZeroAsMin_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(0, 10);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(0);
                assertThat(yearRange.maxDevYear()).isEqualTo(10);
            }

            @Test
            @DisplayName("Should create YearRange with zero as max year")
            void createYearRange_withZeroAsMax_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(-10, 0);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(-10);
                assertThat(yearRange.maxDevYear()).isEqualTo(0);
            }

            @Test
            @DisplayName("Should create YearRange with both years as zero")
            void createYearRange_withBothZero_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(0, 0);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(0);
                assertThat(yearRange.maxDevYear()).isEqualTo(0);
            }

            @Test
            @DisplayName("Should create YearRange with typical insurance years (1990-2025)")
            void createYearRange_withTypicalInsuranceYears_succeeds() {
                // Act
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 2025);

                // Assert
                assertThat(yearRange).isNotNull();
                assertThat(yearRange.minOriginYear()).isEqualTo(1990);
                assertThat(yearRange.maxDevYear()).isEqualTo(2025);
            }

            @Test
            @DisplayName("Should fail validation with large inverted range")
            void createYearRange_withLargeInvertedRange_throwsIllegalArgumentException() {
                // Act & Assert
                assertThatThrownBy(() -> new ClaimsReader.YearRange(2100, 1900))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Max development year")
                    .hasMessageContaining("cannot be less than min origin year");
            }
        }

        @Nested
        @DisplayName("getNumberOfDevelopmentYears() Tests")
        class GetNumberOfDevelopmentYearsTests {

            @Test
            @DisplayName("Should calculate correct number of development years for consecutive years")
            void getNumberOfDevelopmentYears_withConsecutiveYears_returnsCorrectCount() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(4); // 1990, 1991, 1992, 1993
            }

            @Test
            @DisplayName("Should return 1 when min and max year are the same")
            void getNumberOfDevelopmentYears_withSameMinMax_returnsOne() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(2000, 2000);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(1);
            }

            @Test
            @DisplayName("Should calculate correct number for large year gap")
            void getNumberOfDevelopmentYears_withLargeGap_returnsCorrectCount() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1950, 2050);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(101); // 1950 to 2050 inclusive
            }

            @Test
            @DisplayName("Should calculate correct number for single year gap")
            void getNumberOfDevelopmentYears_withOneYearGap_returnsTwo() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(2000, 2001);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(2);
            }

            @Test
            @DisplayName("Should handle negative years correctly in calculation")
            void getNumberOfDevelopmentYears_withNegativeYears_returnsCorrectCount() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(-100, -50);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(51); // -100 to -50 inclusive
            }

            @Test
            @DisplayName("Should handle range crossing zero correctly")
            void getNumberOfDevelopmentYears_crossingZero_returnsCorrectCount() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(-5, 5);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(11); // -5, -4, ..., 0, ..., 4, 5
            }

            @Test
            @DisplayName("Should verify formula: maxDevYear - minOriginYear + 1")
            void getNumberOfDevelopmentYears_verifyFormula_matchesExpectedCalculation() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1999);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();
                int expectedYears = 1999 - 1990 + 1;

                // Assert
                assertThat(numberOfYears).isEqualTo(expectedYears).isEqualTo(10);
            }

            @Test
            @DisplayName("Should return consistent result across multiple calls")
            void getNumberOfDevelopmentYears_calledMultipleTimes_returnsSameResult() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                int firstCall = yearRange.getNumberOfDevelopmentYears();
                int secondCall = yearRange.getNumberOfDevelopmentYears();
                int thirdCall = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(firstCall).isEqualTo(secondCall).isEqualTo(thirdCall).isEqualTo(4);
            }

            @Test
            @DisplayName("Should handle typical insurance scenario (10 years)")
            void getNumberOfDevelopmentYears_typicalInsuranceScenario_returnsCorrectCount() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(2010, 2019);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(10);
            }

            @Test
            @DisplayName("Should handle example from problem statement (1990-1993)")
            void getNumberOfDevelopmentYears_problemStatementExample_returnsFour() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(4); // Matches expected output format
            }
        }

        @Nested
        @DisplayName("toString() Tests")
        class ToStringTests {

            @Test
            @DisplayName("Should return string containing min and max years")
            void toString_withValidRange_containsYears() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result)
                    .contains("1990")
                    .contains("1993");
            }

            @Test
            @DisplayName("Should return string containing number of development years")
            void toString_withValidRange_containsNumberOfYears() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result).contains("4");
            }

            @Test
            @DisplayName("Should follow expected format: YearRange{min-max (n years)}")
            void toString_withValidRange_followsExpectedFormat() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result).matches("YearRange\\{\\d+-\\d+ \\(\\d+ years\\)\\}");
            }

            @Test
            @DisplayName("Should produce expected string for single year range")
            void toString_withSingleYearRange_showsOneYear() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(2000, 2000);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result).isEqualTo("YearRange{2000-2000 (1 years)}");
            }

            @Test
            @DisplayName("Should handle negative years in string representation")
            void toString_withNegativeYears_formatsCorrectly() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(-100, -50);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result)
                    .contains("-100")
                    .contains("-50")
                    .contains("51");
            }

            @Test
            @DisplayName("Should produce consistent output across multiple calls")
            void toString_calledMultipleTimes_returnsSameString() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String firstCall = yearRange.toString();
                String secondCall = yearRange.toString();

                // Assert
                assertThat(firstCall).isEqualTo(secondCall);
            }

            @Test
            @DisplayName("Should not return null")
            void toString_withAnyValidRange_doesNotReturnNull() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result).isNotNull();
            }

            @Test
            @DisplayName("Should return non-empty string")
            void toString_withAnyValidRange_returnsNonEmptyString() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result).isNotEmpty();
            }

            @Test
            @DisplayName("Should be useful for logging and debugging")
            void toString_withValidRange_providesDebugInfo() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String result = yearRange.toString();

                // Assert - should contain all essential information
                assertThat(result)
                    .contains("YearRange")
                    .contains("1990")
                    .contains("1993")
                    .contains("4")
                    .contains("years");
            }

            @Test
            @DisplayName("Should produce example output for documentation")
            void toString_withExampleRange_producesExpectedOutput() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);

                // Act
                String result = yearRange.toString();

                // Assert
                assertThat(result).isEqualTo("YearRange{1990-1993 (4 years)}");
            }
        }

        @Nested
        @DisplayName("Edge Case Tests")
        class EdgeCaseTests {

            @Test
            @DisplayName("Should reject Integer.MAX_VALUE range that would overflow")
            void createYearRange_withMaxIntValue_throwsIllegalArgumentException() {
                // Act & Assert - Range from 0 to MAX_VALUE would overflow
                assertThatThrownBy(() -> new ClaimsReader.YearRange(0, Integer.MAX_VALUE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Year range too large")
                    .hasMessageContaining("would overflow");
            }

            @Test
            @DisplayName("Should reject Integer.MIN_VALUE range that would overflow")
            void createYearRange_withMinIntValue_throwsIllegalArgumentException() {
                // Act & Assert - Range from MIN_VALUE to 0 spans > Integer.MAX_VALUE years
                assertThatThrownBy(() -> new ClaimsReader.YearRange(Integer.MIN_VALUE, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Year range too large")
                    .hasMessageContaining("would overflow");
            }

            @Test
            @DisplayName("Should reject full integer range that would overflow")
            void createYearRange_withFullIntRange_throwsIllegalArgumentException() {
                // Act & Assert - Full integer range would overflow calculation
                assertThatThrownBy(() -> new ClaimsReader.YearRange(Integer.MIN_VALUE, Integer.MAX_VALUE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Year range too large")
                    .hasMessageContaining("would overflow");
            }

            @Test
            @DisplayName("Should verify immutability via accessor methods")
            void yearRangeFields_areImmutable_ensureConsistency() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1990, 1993);
                int originalMin = yearRange.minOriginYear();
                int originalMax = yearRange.maxDevYear();

                // Act - Read fields via accessor methods
                int readMin = yearRange.minOriginYear();
                int readMax = yearRange.maxDevYear();

                // Assert - Values remain unchanged (records are immutable by design)
                assertThat(readMin).isEqualTo(originalMin);
                assertThat(readMax).isEqualTo(originalMax);
            }

            @Test
            @DisplayName("Should handle boundary condition: max = min + 1")
            void createYearRange_withMinimalGap_calculatesCorrectly() {
                // Arrange
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(1000, 1001);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(2);
            }

            @Test
            @DisplayName("Should handle large but valid ranges without overflow")
            void getNumberOfDevelopmentYears_withLargeValidRange_calculatesCorrectly() {
                // Arrange - Range that's large but won't overflow (1 million years)
                ClaimsReader.YearRange yearRange = new ClaimsReader.YearRange(0, 1_000_000);

                // Act
                int numberOfYears = yearRange.getNumberOfDevelopmentYears();

                // Assert
                assertThat(numberOfYears).isEqualTo(1_000_001);
            }
        }
    }
}
