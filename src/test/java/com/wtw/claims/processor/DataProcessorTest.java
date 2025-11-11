package com.wtw.claims.processor;

import com.wtw.claims.model.ClaimRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for the DataProcessor utility class.
 *
 * <p>This test suite validates all static utility methods for processing claim records,
 * including grouping by product, finding year ranges, and calculating development periods.</p>
 *
 * <p>Test Organization:</p>
 * <ul>
 *   <li>Constructor Tests - Verify utility class cannot be instantiated</li>
 *   <li>groupByProduct() Tests - Validate product grouping functionality</li>
 *   <li>findEarliestOriginYear() Tests - Validate minimum origin year detection</li>
 *   <li>findLatestDevelopmentYear() Tests - Validate maximum development year detection</li>
 *   <li>getNumberOfDevelopmentYears() Tests - Validate year range calculations</li>
 *   <li>Integration Tests - Validate end-to-end scenarios</li>
 * </ul>
 *
 * @author Claims Processing System - Test Suite
 * @version 1.0.0
 */
@DisplayName("DataProcessor Utility Class Tests")
class DataProcessorTest {

    // ============================================================================
    // Helper Methods for Test Data Creation
    // ============================================================================

    /**
     * Creates a sample ClaimRecord with the specified parameters.
     */
    private ClaimRecord createRecord(String product, int originYear, int devYear, double value) {
        return new ClaimRecord(product, originYear, devYear, value);
    }

    /**
     * Creates a list of records for the "Comp" product with the given years.
     */
    private List<ClaimRecord> createCompRecords() {
        return Arrays.asList(
            createRecord("Comp", 1992, 1992, 110.0),
            createRecord("Comp", 1992, 1993, 170.0),
            createRecord("Comp", 1993, 1993, 200.0)
        );
    }

    /**
     * Creates a list of records for the "Non-Comp" product with the given years.
     */
    private List<ClaimRecord> createNonCompRecords() {
        return Arrays.asList(
            createRecord("Non-Comp", 1990, 1990, 45.2),
            createRecord("Non-Comp", 1990, 1991, 64.8),
            createRecord("Non-Comp", 1990, 1993, 37.0),
            createRecord("Non-Comp", 1991, 1991, 50.0),
            createRecord("Non-Comp", 1991, 1992, 75.0),
            createRecord("Non-Comp", 1991, 1993, 25.0),
            createRecord("Non-Comp", 1992, 1992, 55.0),
            createRecord("Non-Comp", 1992, 1993, 85.0),
            createRecord("Non-Comp", 1993, 1993, 100.0)
        );
    }

    /**
     * Creates a combined list of records from the example in CLAUDE.md.
     */
    private List<ClaimRecord> createExampleRecords() {
        List<ClaimRecord> records = new ArrayList<>();
        records.addAll(createCompRecords());
        records.addAll(createNonCompRecords());
        return records;
    }

    // ============================================================================
    // Constructor Tests
    // ============================================================================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw UnsupportedOperationException when instantiated via reflection")
        void privateConstructor_whenInvokedViaReflection_throwsUnsupportedOperationException() throws Exception {
            // Arrange
            Constructor<DataProcessor> constructor = DataProcessor.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // Act & Assert
            assertThatThrownBy(constructor::newInstance)
                    .isInstanceOf(InvocationTargetException.class)
                    .hasCauseInstanceOf(UnsupportedOperationException.class)
                    .hasRootCauseMessage("Utility class cannot be instantiated");
        }

        @Test
        @DisplayName("Should have private constructor")
        void constructor_shouldBePrivate() throws Exception {
            // Arrange & Act
            Constructor<DataProcessor> constructor = DataProcessor.class.getDeclaredConstructor();

            // Assert
            assertThat(constructor.canAccess(null)).isFalse();
        }
    }

    // ============================================================================
    // groupByProduct() Tests
    // ============================================================================

    @Nested
    @DisplayName("groupByProduct() Tests")
    class GroupByProductTests {

        @Test
        @DisplayName("Should group single product correctly")
        void groupByProduct_withSingleProduct_returnsMapWithOneEntry() {
            // Arrange
            List<ClaimRecord> records = createCompRecords();

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            assertThat(result)
                    .hasSize(1)
                    .containsKey("Comp");
            assertThat(result.get("Comp"))
                    .hasSize(3)
                    .containsExactlyElementsOf(records);
        }

        @Test
        @DisplayName("Should group two products correctly")
        void groupByProduct_withTwoProducts_returnsMapWithTwoEntries() {
            // Arrange
            List<ClaimRecord> compRecords = createCompRecords();
            List<ClaimRecord> nonCompRecords = createNonCompRecords();
            List<ClaimRecord> allRecords = new ArrayList<>();
            allRecords.addAll(compRecords);
            allRecords.addAll(nonCompRecords);

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(allRecords);

            // Assert
            assertThat(result)
                    .hasSize(2)
                    .containsKeys("Comp", "Non-Comp");
            assertThat(result.get("Comp"))
                    .hasSize(3)
                    .containsExactlyElementsOf(compRecords);
            assertThat(result.get("Non-Comp"))
                    .hasSize(9)
                    .containsExactlyElementsOf(nonCompRecords);
        }

        @Test
        @DisplayName("Should group three or more products correctly")
        void groupByProduct_withThreeProducts_returnsMapWithThreeEntries() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1992, 1992, 110.0),
                createRecord("Non-Comp", 1990, 1990, 45.2),
                createRecord("Auto", 1995, 1995, 200.0),
                createRecord("Comp", 1992, 1993, 170.0),
                createRecord("Auto", 1995, 1996, 250.0),
                createRecord("Non-Comp", 1990, 1991, 64.8)
            );

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            assertThat(result)
                    .hasSize(3)
                    .containsKeys("Comp", "Non-Comp", "Auto");
            assertThat(result.get("Comp")).hasSize(2);
            assertThat(result.get("Non-Comp")).hasSize(2);
            assertThat(result.get("Auto")).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty map when given empty list")
        void groupByProduct_withEmptyList_returnsEmptyMap() {
            // Arrange
            List<ClaimRecord> records = Collections.emptyList();

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw NullPointerException when given null list")
        void groupByProduct_withNullList_throwsNullPointerException() {
            // Arrange
            List<ClaimRecord> records = null;

            // Act & Assert
            assertThatThrownBy(() -> DataProcessor.groupByProduct(records))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Records list must not be null");
        }

        @Test
        @DisplayName("Should preserve case-sensitive product names")
        void groupByProduct_withCaseSensitiveProducts_preservesCase() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("COMP", 1992, 1992, 110.0),
                createRecord("comp", 1992, 1992, 120.0),
                createRecord("Comp", 1992, 1992, 130.0)
            );

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            assertThat(result)
                    .hasSize(3)
                    .containsKeys("COMP", "comp", "Comp");
            assertThat(result.get("COMP")).hasSize(1);
            assertThat(result.get("comp")).hasSize(1);
            assertThat(result.get("Comp")).hasSize(1);
        }

        @Test
        @DisplayName("Should handle product names with whitespace correctly")
        void groupByProduct_withWhitespaceInProductNames_groupsCorrectly() {
            // Arrange
            // Note: ClaimRecord trims product names in constructor
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1992, 1992, 110.0),
                createRecord("  Comp  ", 1992, 1993, 120.0), // Will be trimmed to "Comp"
                createRecord("Comp ", 1992, 1994, 130.0)     // Will be trimmed to "Comp"
            );

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            // All records should be grouped under "Comp" since ClaimRecord trims whitespace
            assertThat(result).hasSize(1).containsKey("Comp");
            assertThat(result.get("Comp")).hasSize(3);
        }

        @Test
        @DisplayName("Should handle product names with special characters")
        void groupByProduct_withSpecialCharactersInProductNames_groupsCorrectly() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp-2000", 1992, 1992, 110.0),
                createRecord("Non_Comp", 1990, 1990, 45.2),
                createRecord("Auto (Premium)", 1995, 1995, 200.0),
                createRecord("Comp-2000", 1992, 1993, 170.0)
            );

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            assertThat(result)
                    .hasSize(3)
                    .containsKeys("Comp-2000", "Non_Comp", "Auto (Premium)");
            assertThat(result.get("Comp-2000")).hasSize(2);
            assertThat(result.get("Non_Comp")).hasSize(1);
            assertThat(result.get("Auto (Premium)")).hasSize(1);
        }

        @Test
        @DisplayName("Should include all records in grouped output")
        void groupByProduct_withMultipleProducts_includesAllRecords() {
            // Arrange
            List<ClaimRecord> records = createExampleRecords();
            int totalRecords = records.size();

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            int groupedRecords = result.values().stream()
                    .mapToInt(List::size)
                    .sum();
            assertThat(groupedRecords).isEqualTo(totalRecords);
        }

        @Test
        @DisplayName("Should include multiple records for same product")
        void groupByProduct_withMultipleRecordsForProduct_includesAllRecords() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1990, 100.0),
                createRecord("Comp", 1991, 1991, 110.0),
                createRecord("Comp", 1992, 1992, 120.0),
                createRecord("Comp", 1993, 1993, 130.0),
                createRecord("Comp", 1994, 1994, 140.0)
            );

            // Act
            Map<String, List<ClaimRecord>> result = DataProcessor.groupByProduct(records);

            // Assert
            assertThat(result).hasSize(1).containsKey("Comp");
            assertThat(result.get("Comp"))
                    .hasSize(5)
                    .containsExactlyElementsOf(records);
        }
    }

    // ============================================================================
    // findEarliestOriginYear() Tests
    // ============================================================================

    @Nested
    @DisplayName("findEarliestOriginYear() Tests")
    class FindEarliestOriginYearTests {

        @Test
        @DisplayName("Should return origin year for single record")
        void findEarliestOriginYear_withSingleRecord_returnsOriginYear() {
            // Arrange
            List<ClaimRecord> records = Collections.singletonList(
                createRecord("Comp", 1995, 1995, 100.0)
            );

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1995);
        }

        @Test
        @DisplayName("Should return minimum origin year with different years")
        void findEarliestOriginYear_withDifferentOriginYears_returnsMinimum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1995, 1995, 100.0),
                createRecord("Comp", 1992, 1992, 110.0),
                createRecord("Comp", 1998, 1998, 120.0),
                createRecord("Comp", 1990, 1990, 130.0)
            );

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1990);
        }

        @Test
        @DisplayName("Should return year when all records have same origin year")
        void findEarliestOriginYear_withSameOriginYears_returnsYear() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1992, 1992, 100.0),
                createRecord("Comp", 1992, 1993, 110.0),
                createRecord("Comp", 1992, 1994, 120.0)
            );

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1992);
        }

        @Test
        @DisplayName("Should find minimum when it's at beginning of list")
        void findEarliestOriginYear_withMinimumAtBeginning_returnsMinimum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1990, 100.0), // Minimum at beginning
                createRecord("Comp", 1995, 1995, 110.0),
                createRecord("Comp", 1998, 1998, 120.0)
            );

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1990);
        }

        @Test
        @DisplayName("Should find minimum when it's at end of list")
        void findEarliestOriginYear_withMinimumAtEnd_returnsMinimum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1995, 1995, 110.0),
                createRecord("Comp", 1998, 1998, 120.0),
                createRecord("Comp", 1990, 1990, 100.0)  // Minimum at end
            );

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1990);
        }

        @Test
        @DisplayName("Should find minimum when it's in middle of list")
        void findEarliestOriginYear_withMinimumInMiddle_returnsMinimum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1995, 1995, 110.0),
                createRecord("Comp", 1990, 1990, 100.0),  // Minimum in middle
                createRecord("Comp", 1998, 1998, 120.0)
            );

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1990);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when given empty list")
        void findEarliestOriginYear_withEmptyList_throwsIllegalArgumentException() {
            // Arrange
            List<ClaimRecord> records = Collections.emptyList();

            // Act & Assert
            assertThatThrownBy(() -> DataProcessor.findEarliestOriginYear(records))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Records list must not be empty");
        }

        @Test
        @DisplayName("Should throw NullPointerException when given null list")
        void findEarliestOriginYear_withNullList_throwsNullPointerException() {
            // Arrange
            List<ClaimRecord> records = null;

            // Act & Assert
            assertThatThrownBy(() -> DataProcessor.findEarliestOriginYear(records))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Records list must not be null");
        }

        @Test
        @DisplayName("Should handle large year range correctly")
        void findEarliestOriginYear_withLargeYearRange_returnsMinimum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 2020, 2020, 100.0),
                createRecord("Comp", 1990, 1990, 110.0),
                createRecord("Comp", 2010, 2010, 120.0),
                createRecord("Comp", 2000, 2000, 130.0)
            );

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1990);
        }

        @Test
        @DisplayName("Should work with example data from CLAUDE.md")
        void findEarliestOriginYear_withExampleData_returnsCorrectYear() {
            // Arrange
            List<ClaimRecord> records = createExampleRecords();

            // Act
            int result = DataProcessor.findEarliestOriginYear(records);

            // Assert
            assertThat(result).isEqualTo(1990);
        }
    }

    // ============================================================================
    // findLatestDevelopmentYear() Tests
    // ============================================================================

    @Nested
    @DisplayName("findLatestDevelopmentYear() Tests")
    class FindLatestDevelopmentYearTests {

        @Test
        @DisplayName("Should return development year for single record")
        void findLatestDevelopmentYear_withSingleRecord_returnsDevelopmentYear() {
            // Arrange
            List<ClaimRecord> records = Collections.singletonList(
                createRecord("Comp", 1995, 1997, 100.0)
            );

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(1997);
        }

        @Test
        @DisplayName("Should return maximum development year with different years")
        void findLatestDevelopmentYear_withDifferentDevelopmentYears_returnsMaximum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1995, 100.0),
                createRecord("Comp", 1991, 1992, 110.0),
                createRecord("Comp", 1992, 1998, 120.0),
                createRecord("Comp", 1993, 1993, 130.0)
            );

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(1998);
        }

        @Test
        @DisplayName("Should return year when all records have same development year")
        void findLatestDevelopmentYear_withSameDevelopmentYears_returnsYear() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1995, 100.0),
                createRecord("Comp", 1991, 1995, 110.0),
                createRecord("Comp", 1992, 1995, 120.0)
            );

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(1995);
        }

        @Test
        @DisplayName("Should find maximum when it's at beginning of list")
        void findLatestDevelopmentYear_withMaximumAtBeginning_returnsMaximum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1998, 100.0), // Maximum at beginning
                createRecord("Comp", 1991, 1995, 110.0),
                createRecord("Comp", 1992, 1992, 120.0)
            );

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(1998);
        }

        @Test
        @DisplayName("Should find maximum when it's at end of list")
        void findLatestDevelopmentYear_withMaximumAtEnd_returnsMaximum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1992, 100.0),
                createRecord("Comp", 1991, 1995, 110.0),
                createRecord("Comp", 1992, 1998, 120.0)  // Maximum at end
            );

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(1998);
        }

        @Test
        @DisplayName("Should find maximum when it's in middle of list")
        void findLatestDevelopmentYear_withMaximumInMiddle_returnsMaximum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1992, 100.0),
                createRecord("Comp", 1991, 1998, 110.0),  // Maximum in middle
                createRecord("Comp", 1992, 1995, 120.0)
            );

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(1998);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when given empty list")
        void findLatestDevelopmentYear_withEmptyList_throwsIllegalArgumentException() {
            // Arrange
            List<ClaimRecord> records = Collections.emptyList();

            // Act & Assert
            assertThatThrownBy(() -> DataProcessor.findLatestDevelopmentYear(records))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Records list must not be empty");
        }

        @Test
        @DisplayName("Should throw NullPointerException when given null list")
        void findLatestDevelopmentYear_withNullList_throwsNullPointerException() {
            // Arrange
            List<ClaimRecord> records = null;

            // Act & Assert
            assertThatThrownBy(() -> DataProcessor.findLatestDevelopmentYear(records))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Records list must not be null");
        }

        @Test
        @DisplayName("Should handle large year range correctly")
        void findLatestDevelopmentYear_withLargeYearRange_returnsMaximum() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1995, 100.0),
                createRecord("Comp", 1991, 2020, 110.0),
                createRecord("Comp", 1992, 2000, 120.0),
                createRecord("Comp", 1993, 2010, 130.0)
            );

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(2020);
        }

        @Test
        @DisplayName("Should work with example data from CLAUDE.md")
        void findLatestDevelopmentYear_withExampleData_returnsCorrectYear() {
            // Arrange
            List<ClaimRecord> records = createExampleRecords();

            // Act
            int result = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(result).isEqualTo(1993);
        }
    }

    // ============================================================================
    // getNumberOfDevelopmentYears() Tests
    // ============================================================================

    @Nested
    @DisplayName("getNumberOfDevelopmentYears() Tests")
    class GetNumberOfDevelopmentYearsTests {

        @Test
        @DisplayName("Should calculate correctly for small year range")
        void getNumberOfDevelopmentYears_withSmallRange_returnsCorrectCount() {
            // Arrange: 1990-1993 should be 4 years
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1990, 100.0),
                createRecord("Comp", 1991, 1992, 110.0),
                createRecord("Comp", 1992, 1993, 120.0)
            );

            // Act
            int result = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert
            assertThat(result).isEqualTo(4); // 1993 - 1990 + 1 = 4
        }

        @Test
        @DisplayName("Should return 1 for single year")
        void getNumberOfDevelopmentYears_withSingleYear_returnsOne() {
            // Arrange: Same origin and development year
            List<ClaimRecord> records = Collections.singletonList(
                createRecord("Comp", 2000, 2000, 100.0)
            );

            // Act
            int result = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert
            assertThat(result).isEqualTo(1); // 2000 - 2000 + 1 = 1
        }

        @Test
        @DisplayName("Should calculate correctly for large year range")
        void getNumberOfDevelopmentYears_withLargeRange_returnsCorrectCount() {
            // Arrange: 1990-2020 should be 31 years
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1990, 1995, 100.0),
                createRecord("Comp", 1995, 2010, 110.0),
                createRecord("Comp", 2000, 2020, 120.0)
            );

            // Act
            int result = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert
            assertThat(result).isEqualTo(31); // 2020 - 1990 + 1 = 31
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when given empty list")
        void getNumberOfDevelopmentYears_withEmptyList_throwsIllegalArgumentException() {
            // Arrange
            List<ClaimRecord> records = Collections.emptyList();

            // Act & Assert
            assertThatThrownBy(() -> DataProcessor.getNumberOfDevelopmentYears(records))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Records list must not be empty");
        }

        @Test
        @DisplayName("Should throw NullPointerException when given null list")
        void getNumberOfDevelopmentYears_withNullList_throwsNullPointerException() {
            // Arrange
            List<ClaimRecord> records = null;

            // Act & Assert
            assertThatThrownBy(() -> DataProcessor.getNumberOfDevelopmentYears(records))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Records list must not be null");
        }

        @Test
        @DisplayName("Should verify formula: latestDev - earliestOrigin + 1")
        void getNumberOfDevelopmentYears_verifyFormula_calculatesCorrectly() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Comp", 1995, 1997, 100.0),
                createRecord("Comp", 1997, 2005, 110.0),
                createRecord("Comp", 2000, 2000, 120.0)
            );

            // Act
            int result = DataProcessor.getNumberOfDevelopmentYears(records);
            int earliestOrigin = DataProcessor.findEarliestOriginYear(records);
            int latestDev = DataProcessor.findLatestDevelopmentYear(records);

            // Assert
            assertThat(earliestOrigin).isEqualTo(1995);
            assertThat(latestDev).isEqualTo(2005);
            assertThat(result).isEqualTo(latestDev - earliestOrigin + 1);
            assertThat(result).isEqualTo(11); // 2005 - 1995 + 1 = 11
        }

        @Test
        @DisplayName("Should work with example data from CLAUDE.md")
        void getNumberOfDevelopmentYears_withExampleData_returnsCorrectCount() {
            // Arrange
            List<ClaimRecord> records = createExampleRecords();

            // Act
            int result = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert
            // Earliest origin: 1990, Latest development: 1993
            // Expected: 1993 - 1990 + 1 = 4
            assertThat(result).isEqualTo(4);
        }
    }

    // ============================================================================
    // Integration Tests
    // ============================================================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should process real-world scenario with multiple products and years correctly")
        void integrationTest_withMultipleProductsAndYears_processesCorrectly() {
            // Arrange
            List<ClaimRecord> records = createExampleRecords();

            // Act
            Map<String, List<ClaimRecord>> grouped = DataProcessor.groupByProduct(records);
            int earliestOrigin = DataProcessor.findEarliestOriginYear(records);
            int latestDev = DataProcessor.findLatestDevelopmentYear(records);
            int numDevYears = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert - Grouping
            assertThat(grouped)
                    .hasSize(2)
                    .containsKeys("Comp", "Non-Comp");
            assertThat(grouped.get("Comp")).hasSize(3);
            assertThat(grouped.get("Non-Comp")).hasSize(9);

            // Assert - Year ranges
            assertThat(earliestOrigin).isEqualTo(1990);
            assertThat(latestDev).isEqualTo(1993);
            assertThat(numDevYears).isEqualTo(4);
        }

        @Test
        @DisplayName("Should verify grouping and year calculations work together")
        void integrationTest_groupingAndYearCalculations_workTogether() {
            // Arrange
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Product-A", 2000, 2000, 100.0),
                createRecord("Product-B", 1998, 2002, 110.0),
                createRecord("Product-A", 2001, 2003, 120.0),
                createRecord("Product-C", 1995, 1995, 130.0),
                createRecord("Product-B", 1999, 2001, 140.0)
            );

            // Act
            Map<String, List<ClaimRecord>> grouped = DataProcessor.groupByProduct(records);
            int earliestOrigin = DataProcessor.findEarliestOriginYear(records);
            int latestDev = DataProcessor.findLatestDevelopmentYear(records);
            int numDevYears = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert - Grouping
            assertThat(grouped).hasSize(3);
            assertThat(grouped.get("Product-A")).hasSize(2);
            assertThat(grouped.get("Product-B")).hasSize(2);
            assertThat(grouped.get("Product-C")).hasSize(1);

            // Assert - Year calculations
            assertThat(earliestOrigin).isEqualTo(1995);
            assertThat(latestDev).isEqualTo(2003);
            assertThat(numDevYears).isEqualTo(9); // 2003 - 1995 + 1

            // Assert - All records accounted for
            int totalGrouped = grouped.values().stream().mapToInt(List::size).sum();
            assertThat(totalGrouped).isEqualTo(records.size());
        }

        @Test
        @DisplayName("Should process example from CLAUDE.md correctly")
        void integrationTest_withClaudeMdExample_processesCorrectly() {
            // Arrange - Using exact data from CLAUDE.md
            List<ClaimRecord> records = createExampleRecords();

            // Act
            Map<String, List<ClaimRecord>> grouped = DataProcessor.groupByProduct(records);
            int earliestOrigin = DataProcessor.findEarliestOriginYear(records);
            int latestDev = DataProcessor.findLatestDevelopmentYear(records);
            int numDevYears = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert - Match expected output format from CLAUDE.md
            // Output should show: "1990, 4"
            assertThat(earliestOrigin).isEqualTo(1990);
            assertThat(numDevYears).isEqualTo(4);

            // Assert - Grouping matches expected structure
            assertThat(grouped).hasSize(2);

            // Comp product has 3 records (origin years: 1992, 1992, 1993)
            List<ClaimRecord> compRecords = grouped.get("Comp");
            assertThat(compRecords).hasSize(3);
            assertThat(compRecords)
                    .extracting(ClaimRecord::getOriginYear)
                    .containsExactly(1992, 1992, 1993);

            // Non-Comp product has 9 records (origin years: 1990-1993)
            List<ClaimRecord> nonCompRecords = grouped.get("Non-Comp");
            assertThat(nonCompRecords).hasSize(9);

            // Verify all products span from earliest origin (1990) to latest dev (1993)
            assertThat(earliestOrigin).isEqualTo(1990);
            assertThat(latestDev).isEqualTo(1993);
        }

        @Test
        @DisplayName("Should handle complex multi-product scenario with gaps in years")
        void integrationTest_withYearGaps_processesCorrectly() {
            // Arrange - Scenario with gaps in years (no 1996, 1997)
            List<ClaimRecord> records = Arrays.asList(
                createRecord("Auto", 1995, 1995, 100.0),
                createRecord("Auto", 1995, 1998, 110.0),
                createRecord("Home", 1995, 1999, 120.0),
                createRecord("Life", 1998, 2000, 130.0),
                createRecord("Auto", 1999, 2000, 140.0)
            );

            // Act
            Map<String, List<ClaimRecord>> grouped = DataProcessor.groupByProduct(records);
            int earliestOrigin = DataProcessor.findEarliestOriginYear(records);
            int latestDev = DataProcessor.findLatestDevelopmentYear(records);
            int numDevYears = DataProcessor.getNumberOfDevelopmentYears(records);

            // Assert
            assertThat(grouped).hasSize(3);
            assertThat(earliestOrigin).isEqualTo(1995);
            assertThat(latestDev).isEqualTo(2000);
            assertThat(numDevYears).isEqualTo(6); // 2000 - 1995 + 1 (includes gap years)
        }
    }
}
