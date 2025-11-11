package com.wtw.claims.processor;

import com.wtw.claims.model.ClaimRecord;
import com.wtw.claims.model.ClaimsTriangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive unit tests for {@link TriangleBuilder}.
 *
 * <p>This test class verifies the static utility methods of TriangleBuilder,
 * including constructor non-instantiability, basic triangle building functionality,
 * missing data handling, validation, and real-world scenarios.</p>
 *
 * @author Claims Processing System
 * @version 1.0.0
 */
@DisplayName("TriangleBuilder Unit Tests")
class TriangleBuilderTest {

    // ============================================================
    // Helper Methods for Test Data Creation
    // ============================================================

    /**
     * Creates a ClaimRecord with the given parameters.
     */
    private ClaimRecord createRecord(String product, int originYear, int devYear, double value) {
        return new ClaimRecord(product, originYear, devYear, value);
    }

    /**
     * Creates a list of ClaimRecords for testing.
     */
    private List<ClaimRecord> createRecordsList(ClaimRecord... records) {
        return Arrays.asList(records);
    }

    // ============================================================
    // Constructor Tests
    // ============================================================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw UnsupportedOperationException when private constructor is invoked via reflection")
        void privateConstructor_whenInvokedViaReflection_throwsUnsupportedOperationException() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> {
                Constructor<TriangleBuilder> constructor = TriangleBuilder.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                constructor.newInstance();
            })
            .isInstanceOf(InvocationTargetException.class)
            .hasCauseInstanceOf(UnsupportedOperationException.class)
            .hasRootCauseMessage("Utility class cannot be instantiated");
        }

        @Test
        @DisplayName("Should not have publicly accessible constructor")
        void constructor_isNotPubliclyAccessible() throws NoSuchMethodException {
            // Arrange
            Constructor<TriangleBuilder> constructor = TriangleBuilder.class.getDeclaredConstructor();

            // Act & Assert
            assertThat(constructor.canAccess(null)).isFalse();
        }
    }

    // ============================================================
    // buildTriangle() Tests - Basic Functionality
    // ============================================================

    @Nested
    @DisplayName("buildTriangle() - Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should build triangle with all origin and development years present")
        void buildTriangle_withAllYearsPresent_buildsCorrectTriangle() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("TestProduct", 1990, 1990, 100.0),
                createRecord("TestProduct", 1990, 1991, 150.0),
                createRecord("TestProduct", 1991, 1991, 200.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("TestProduct", records, 1990, 1991);

            // Assert
            assertThat(triangle).isNotNull();
            assertThat(triangle.getProductName()).isEqualTo("TestProduct");
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(1991);
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(100.0);
            assertThat(triangle.getIncrementalValue(1990, 1991)).isEqualTo(150.0);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should build triangle with single record")
        void buildTriangle_withSingleRecord_buildsCorrectTriangle() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("SingleProduct", 1995, 1995, 500.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("SingleProduct", records, 1995, 1995);

            // Assert
            assertThat(triangle).isNotNull();
            assertThat(triangle.getProductName()).isEqualTo("SingleProduct");
            assertThat(triangle.getIncrementalValue(1995, 1995)).isEqualTo(500.0);
        }

        @Test
        @DisplayName("Should build triangle with multiple records for same product")
        void buildTriangle_withMultipleRecordsSameProduct_buildsCorrectTriangle() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("Multi", 2000, 2000, 10.0),
                createRecord("Multi", 2000, 2001, 20.0),
                createRecord("Multi", 2000, 2002, 30.0),
                createRecord("Multi", 2001, 2001, 40.0),
                createRecord("Multi", 2001, 2002, 50.0),
                createRecord("Multi", 2002, 2002, 60.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Multi", records, 2000, 2002);

            // Assert
            assertThat(triangle).isNotNull();
            assertThat(triangle.getProductName()).isEqualTo("Multi");
            assertThat(triangle.getIncrementalValue(2000, 2000)).isEqualTo(10.0);
            assertThat(triangle.getIncrementalValue(2000, 2001)).isEqualTo(20.0);
            assertThat(triangle.getIncrementalValue(2000, 2002)).isEqualTo(30.0);
            assertThat(triangle.getIncrementalValue(2001, 2001)).isEqualTo(40.0);
            assertThat(triangle.getIncrementalValue(2001, 2002)).isEqualTo(50.0);
            assertThat(triangle.getIncrementalValue(2002, 2002)).isEqualTo(60.0);
        }

        @Test
        @DisplayName("Should build triangle with empty records list and all values default to 0.0")
        void buildTriangle_withEmptyRecordsList_returnsTriangleWithZeroValues() {
            // Arrange
            List<ClaimRecord> records = Collections.emptyList();

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("EmptyProduct", records, 1990, 1993);

            // Assert
            assertThat(triangle).isNotNull();
            assertThat(triangle.getProductName()).isEqualTo("EmptyProduct");
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(1993);

            // All values should be 0.0 since no records were added
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1993, 1993)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should verify triangle has correct product name")
        void buildTriangle_verifyProductName_isCorrect() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("CorrectName", 2000, 2000, 100.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("CorrectName", records, 2000, 2000);

            // Assert
            assertThat(triangle.getProductName()).isEqualTo("CorrectName");
        }

        @Test
        @DisplayName("Should verify triangle has correct global year range")
        void buildTriangle_verifyGlobalYearRange_isCorrect() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("YearTest", 1995, 1995, 50.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("YearTest", records, 1990, 2000);

            // Assert
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(2000);
            assertThat(triangle.getNumberOfDevelopmentYears()).isEqualTo(11);
        }
    }

    // ============================================================
    // buildTriangle() Tests - Missing Data Handling
    // ============================================================

    @Nested
    @DisplayName("buildTriangle() - Missing Data Handling Tests")
    class MissingDataHandlingTests {

        @Test
        @DisplayName("Should handle missing development years with gaps")
        void buildTriangle_withMissingDevelopmentYears_returnsZeroForMissingValues() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("GapProduct", 1990, 1990, 45.2),
                createRecord("GapProduct", 1990, 1991, 64.8),
                // 1992 is missing
                createRecord("GapProduct", 1990, 1993, 37.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("GapProduct", records, 1990, 1993);

            // Assert
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(45.2);
            assertThat(triangle.getIncrementalValue(1990, 1991)).isEqualTo(64.8);
            assertThat(triangle.getIncrementalValue(1990, 1992)).isEqualTo(0.0); // Missing
            assertThat(triangle.getIncrementalValue(1990, 1993)).isEqualTo(37.0);
        }

        @Test
        @DisplayName("Should handle missing origin years")
        void buildTriangle_withMissingOriginYears_returnsZeroForMissingOrigins() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("SkipOrigin", 1990, 1990, 100.0),
                // 1991 origin year is missing entirely
                createRecord("SkipOrigin", 1992, 1992, 200.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("SkipOrigin", records, 1990, 1992);

            // Assert
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(100.0);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(0.0); // Missing origin
            assertThat(triangle.getIncrementalValue(1991, 1992)).isEqualTo(0.0); // Missing origin
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should return 0.0 for missing values when retrieved via getIncrementalValue")
        void buildTriangle_missingValues_returnZeroWhenRetrieved() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("Sparse", 1995, 1995, 100.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Sparse", records, 1990, 2000);

            // Assert - years with no data should return 0.0
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1994, 1994)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1995, 1995)).isEqualTo(100.0); // Only this has data
            assertThat(triangle.getIncrementalValue(1996, 1996)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle product with no data for early years (Comp example)")
        void buildTriangle_withNoDataForEarlyYears_returnsZeroForEarlyYears() {
            // Arrange - Comp has no 1990-1991 data
            List<ClaimRecord> records = createRecordsList(
                createRecord("Comp", 1992, 1992, 110.0),
                createRecord("Comp", 1992, 1993, 170.0),
                createRecord("Comp", 1993, 1993, 200.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Comp", records, 1990, 1993);

            // Assert
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1990, 1991)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(110.0);
            assertThat(triangle.getIncrementalValue(1992, 1993)).isEqualTo(170.0);
            assertThat(triangle.getIncrementalValue(1993, 1993)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should handle product with no data for late years")
        void buildTriangle_withNoDataForLateYears_returnsZeroForLateYears() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("EarlyOnly", 1990, 1990, 100.0),
                createRecord("EarlyOnly", 1991, 1991, 200.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("EarlyOnly", records, 1990, 1995);

            // Assert
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(100.0);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(200.0);
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1993, 1993)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1994, 1994)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1995, 1995)).isEqualTo(0.0);
        }
    }

    // ============================================================
    // buildTriangle() Tests - Validation
    // ============================================================

    @Nested
    @DisplayName("buildTriangle() - Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NullPointerException when product name is null")
        void buildTriangle_withNullProductName_throwsNullPointerException() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("Test", 1990, 1990, 100.0)
            );

            // Act & Assert
            assertThatThrownBy(() ->
                TriangleBuilder.buildTriangle(null, records, 1990, 1993)
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Product name must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when product name is empty")
        void buildTriangle_withEmptyProductName_throwsIllegalArgumentException() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("Test", 1990, 1990, 100.0)
            );

            // Act & Assert
            assertThatThrownBy(() ->
                TriangleBuilder.buildTriangle("", records, 1990, 1993)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product name must not be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when product name is whitespace only")
        void buildTriangle_withWhitespaceOnlyProductName_throwsIllegalArgumentException() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("Test", 1990, 1990, 100.0)
            );

            // Act & Assert
            assertThatThrownBy(() ->
                TriangleBuilder.buildTriangle("   ", records, 1990, 1993)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Product name must not be empty");
        }

        @Test
        @DisplayName("Should throw NullPointerException when records list is null")
        void buildTriangle_withNullRecordsList_throwsNullPointerException() {
            // Act & Assert
            assertThatThrownBy(() ->
                TriangleBuilder.buildTriangle("TestProduct", null, 1990, 1993)
            )
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Records list must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when globalMaxYear < globalMinYear")
        void buildTriangle_withInvalidYearRange_throwsIllegalArgumentException() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("Test", 1990, 1990, 100.0)
            );

            // Act & Assert
            assertThatThrownBy(() ->
                TriangleBuilder.buildTriangle("TestProduct", records, 1995, 1990)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Latest development year")
            .hasMessageContaining("must not be less than earliest origin year");
        }

        @Test
        @DisplayName("Should accept valid year range when globalMaxYear == globalMinYear")
        void buildTriangle_withSingleYearRange_buildsSuccessfully() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("SingleYear", 1990, 1990, 100.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("SingleYear", records, 1990, 1990);

            // Assert
            assertThat(triangle).isNotNull();
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(1990);
            assertThat(triangle.getNumberOfDevelopmentYears()).isEqualTo(1);
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(100.0);
        }
    }

    // ============================================================
    // buildTriangle() Tests - Real-World Scenarios
    // ============================================================

    @Nested
    @DisplayName("buildTriangle() - Real-World Scenarios")
    class RealWorldScenariosTests {

        @Test
        @DisplayName("Should build triangle for Comp product using example data from CLAUDE.md")
        void buildTriangle_withCompProductExampleData_buildsCorrectTriangle() {
            // Arrange - Data from CLAUDE.md
            List<ClaimRecord> records = createRecordsList(
                createRecord("Comp", 1992, 1992, 110.0),
                createRecord("Comp", 1992, 1993, 170.0),
                createRecord("Comp", 1993, 1993, 200.0)
            );

            // Act - Global range is 1990-1993 (includes Non-Comp data range)
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Comp", records, 1990, 1993);

            // Assert - Verify years 1990-1991 have 0.0
            assertThat(triangle.getProductName()).isEqualTo("Comp");
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(1993);

            // Years with no data
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1990, 1991)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1990, 1992)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1990, 1993)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1991, 1992)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(1991, 1993)).isEqualTo(0.0);

            // Years with actual data
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(110.0);
            assertThat(triangle.getIncrementalValue(1992, 1993)).isEqualTo(170.0);
            assertThat(triangle.getIncrementalValue(1993, 1993)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should build triangle for Non-Comp product using example data from CLAUDE.md")
        void buildTriangle_withNonCompProductExampleData_buildsCorrectTriangle() {
            // Arrange - Data from CLAUDE.md
            List<ClaimRecord> records = createRecordsList(
                createRecord("Non-Comp", 1990, 1990, 45.2),
                createRecord("Non-Comp", 1990, 1991, 64.8),
                createRecord("Non-Comp", 1990, 1993, 37.0),  // Note: 1992 is missing
                createRecord("Non-Comp", 1991, 1991, 50.0),
                createRecord("Non-Comp", 1991, 1992, 75.0),
                createRecord("Non-Comp", 1991, 1993, 25.0),
                createRecord("Non-Comp", 1992, 1992, 55.0),
                createRecord("Non-Comp", 1992, 1993, 85.0),
                createRecord("Non-Comp", 1993, 1993, 100.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Non-Comp", records, 1990, 1993);

            // Assert - Verify all values including missing 1990-1992 development year
            assertThat(triangle.getProductName()).isEqualTo("Non-Comp");
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(1993);

            // Origin year 1990
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(45.2);
            assertThat(triangle.getIncrementalValue(1990, 1991)).isEqualTo(64.8);
            assertThat(triangle.getIncrementalValue(1990, 1992)).isEqualTo(0.0);  // Missing
            assertThat(triangle.getIncrementalValue(1990, 1993)).isEqualTo(37.0);

            // Origin year 1991
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(50.0);
            assertThat(triangle.getIncrementalValue(1991, 1992)).isEqualTo(75.0);
            assertThat(triangle.getIncrementalValue(1991, 1993)).isEqualTo(25.0);

            // Origin year 1992
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(55.0);
            assertThat(triangle.getIncrementalValue(1992, 1993)).isEqualTo(85.0);

            // Origin year 1993
            assertThat(triangle.getIncrementalValue(1993, 1993)).isEqualTo(100.0);
        }
    }

    // ============================================================
    // buildTriangle() Tests - Data Integrity
    // ============================================================

    @Nested
    @DisplayName("buildTriangle() - Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should add all records to triangle without data loss")
        void buildTriangle_withMultipleRecords_addsAllRecordsWithoutLoss() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("DataTest", 2000, 2000, 10.5),
                createRecord("DataTest", 2000, 2001, 20.5),
                createRecord("DataTest", 2001, 2001, 30.5),
                createRecord("DataTest", 2001, 2002, 40.5),
                createRecord("DataTest", 2002, 2002, 50.5)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("DataTest", records, 2000, 2002);

            // Assert - All values should be present
            assertThat(triangle.getIncrementalValue(2000, 2000)).isEqualTo(10.5);
            assertThat(triangle.getIncrementalValue(2000, 2001)).isEqualTo(20.5);
            assertThat(triangle.getIncrementalValue(2001, 2001)).isEqualTo(30.5);
            assertThat(triangle.getIncrementalValue(2001, 2002)).isEqualTo(40.5);
            assertThat(triangle.getIncrementalValue(2002, 2002)).isEqualTo(50.5);
        }

        @Test
        @DisplayName("Should store exact incremental values matching input")
        void buildTriangle_incrementalValues_matchInputExactly() {
            // Arrange - Test with various decimal values
            List<ClaimRecord> records = createRecordsList(
                createRecord("Precision", 2000, 2000, 123.456),
                createRecord("Precision", 2000, 2001, 789.012),
                createRecord("Precision", 2001, 2001, 0.001),
                createRecord("Precision", 2001, 2002, 999.999)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Precision", records, 2000, 2002);

            // Assert - Values should match exactly
            assertThat(triangle.getIncrementalValue(2000, 2000)).isEqualTo(123.456);
            assertThat(triangle.getIncrementalValue(2000, 2001)).isEqualTo(789.012);
            assertThat(triangle.getIncrementalValue(2001, 2001)).isEqualTo(0.001);
            assertThat(triangle.getIncrementalValue(2001, 2002)).isEqualTo(999.999);
        }

        @Test
        @DisplayName("Should create triangle with dimensions matching global year range")
        void buildTriangle_triangleDimensions_matchGlobalYearRange() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("DimTest", 1995, 1995, 100.0)
            );

            // Act - Global range much larger than product data range
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("DimTest", records, 1990, 2000);

            // Assert
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(2000);
            assertThat(triangle.getNumberOfDevelopmentYears()).isEqualTo(11);
        }

        @Test
        @DisplayName("Should allow values to be retrieved using getIncrementalValue")
        void buildTriangle_values_canBeRetrievedUsingGetter() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("Retrieve", 2005, 2005, 555.55),
                createRecord("Retrieve", 2005, 2006, 666.66)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Retrieve", records, 2005, 2006);

            // Assert - Verify retrieval works
            double value1 = triangle.getIncrementalValue(2005, 2005);
            double value2 = triangle.getIncrementalValue(2005, 2006);

            assertThat(value1).isEqualTo(555.55);
            assertThat(value2).isEqualTo(666.66);
        }

        @Test
        @DisplayName("Should handle zero incremental values correctly")
        void buildTriangle_withZeroIncrementalValues_storesZeroCorrectly() {
            // Arrange - Explicit zero values should be stored
            List<ClaimRecord> records = createRecordsList(
                createRecord("ZeroTest", 2000, 2000, 0.0),
                createRecord("ZeroTest", 2000, 2001, 100.0),
                createRecord("ZeroTest", 2001, 2001, 0.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("ZeroTest", records, 2000, 2001);

            // Assert - Both explicit zeros and missing values return 0.0
            assertThat(triangle.getIncrementalValue(2000, 2000)).isEqualTo(0.0);
            assertThat(triangle.getIncrementalValue(2000, 2001)).isEqualTo(100.0);
            assertThat(triangle.getIncrementalValue(2001, 2001)).isEqualTo(0.0);
        }
    }

    // ============================================================
    // Integration Tests
    // ============================================================

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should build multiple triangles for different products with same global range")
        void buildTriangle_multipleProductsSameGlobalRange_eachHasCorrectData() {
            // Arrange
            List<ClaimRecord> compRecords = createRecordsList(
                createRecord("Comp", 1992, 1992, 110.0),
                createRecord("Comp", 1993, 1993, 200.0)
            );

            List<ClaimRecord> nonCompRecords = createRecordsList(
                createRecord("Non-Comp", 1990, 1990, 45.2),
                createRecord("Non-Comp", 1991, 1991, 50.0)
            );

            // Act
            ClaimsTriangle compTriangle = TriangleBuilder.buildTriangle("Comp", compRecords, 1990, 1993);
            ClaimsTriangle nonCompTriangle = TriangleBuilder.buildTriangle("Non-Comp", nonCompRecords, 1990, 1993);

            // Assert - Both have same year range but different data
            assertThat(compTriangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(compTriangle.getLatestDevelopmentYear()).isEqualTo(1993);
            assertThat(nonCompTriangle.getEarliestOriginYear()).isEqualTo(1990);
            assertThat(nonCompTriangle.getLatestDevelopmentYear()).isEqualTo(1993);

            // Comp has no 1990 data
            assertThat(compTriangle.getIncrementalValue(1990, 1990)).isEqualTo(0.0);
            assertThat(compTriangle.getIncrementalValue(1992, 1992)).isEqualTo(110.0);

            // Non-Comp has 1990 data
            assertThat(nonCompTriangle.getIncrementalValue(1990, 1990)).isEqualTo(45.2);
            assertThat(nonCompTriangle.getIncrementalValue(1992, 1992)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle complex scenario with multiple products and year gaps")
        void buildTriangle_complexScenarioWithGaps_buildsCorrectTriangles() {
            // Arrange - Complex scenario with various gaps
            List<ClaimRecord> productARecords = createRecordsList(
                createRecord("ProductA", 1990, 1990, 100.0),
                // Gap in 1991
                createRecord("ProductA", 1992, 1992, 300.0),
                createRecord("ProductA", 1992, 1993, 400.0)
            );

            List<ClaimRecord> productBRecords = createRecordsList(
                // Gap in 1990
                createRecord("ProductB", 1991, 1991, 200.0),
                createRecord("ProductB", 1991, 1992, 250.0),
                createRecord("ProductB", 1993, 1993, 500.0)
            );

            // Act
            ClaimsTriangle triangleA = TriangleBuilder.buildTriangle("ProductA", productARecords, 1990, 1993);
            ClaimsTriangle triangleB = TriangleBuilder.buildTriangle("ProductB", productBRecords, 1990, 1993);

            // Assert - ProductA
            assertThat(triangleA.getIncrementalValue(1990, 1990)).isEqualTo(100.0);
            assertThat(triangleA.getIncrementalValue(1991, 1991)).isEqualTo(0.0); // Gap
            assertThat(triangleA.getIncrementalValue(1992, 1992)).isEqualTo(300.0);
            assertThat(triangleA.getIncrementalValue(1992, 1993)).isEqualTo(400.0);

            // Assert - ProductB
            assertThat(triangleB.getIncrementalValue(1990, 1990)).isEqualTo(0.0); // Gap
            assertThat(triangleB.getIncrementalValue(1991, 1991)).isEqualTo(200.0);
            assertThat(triangleB.getIncrementalValue(1991, 1992)).isEqualTo(250.0);
            assertThat(triangleB.getIncrementalValue(1993, 1993)).isEqualTo(500.0);
        }

        @Test
        @DisplayName("Should handle large year range with sparse data")
        void buildTriangle_largeYearRangeWithSparseData_performsCorrectly() {
            // Arrange - Large range (20 years) with only a few data points
            List<ClaimRecord> records = createRecordsList(
                createRecord("Sparse", 1990, 1990, 100.0),
                createRecord("Sparse", 2000, 2000, 200.0),
                createRecord("Sparse", 2009, 2009, 300.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle("Sparse", records, 1990, 2009);

            // Assert
            assertThat(triangle.getNumberOfDevelopmentYears()).isEqualTo(20);
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(100.0);
            assertThat(triangle.getIncrementalValue(1995, 1995)).isEqualTo(0.0); // Middle gap
            assertThat(triangle.getIncrementalValue(2000, 2000)).isEqualTo(200.0);
            assertThat(triangle.getIncrementalValue(2005, 2005)).isEqualTo(0.0); // Middle gap
            assertThat(triangle.getIncrementalValue(2009, 2009)).isEqualTo(300.0);
        }

        @Test
        @DisplayName("Should handle product names with special characters and whitespace")
        void buildTriangle_withSpecialCharactersInProductName_trimsAndStoresCorrectly() {
            // Arrange
            List<ClaimRecord> records = createRecordsList(
                createRecord("  Special-Product_123  ", 2000, 2000, 100.0)
            );

            // Act
            ClaimsTriangle triangle = TriangleBuilder.buildTriangle(
                "  Special-Product_123  ",
                records,
                2000,
                2000
            );

            // Assert - Product name should be trimmed
            assertThat(triangle.getProductName()).isEqualTo("Special-Product_123");
            assertThat(triangle.getIncrementalValue(2000, 2000)).isEqualTo(100.0);
        }
    }
}
