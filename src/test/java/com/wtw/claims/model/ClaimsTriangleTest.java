package com.wtw.claims.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

/**
 * Comprehensive test suite for the ClaimsTriangle model class.
 *
 * <p>This test suite verifies:
 * <ul>
 *   <li>Constructor validation and field initialization</li>
 *   <li>Adding and retrieving incremental claim values</li>
 *   <li>Setting and retrieving cumulative claim values</li>
 *   <li>Getter method correctness</li>
 *   <li>Development year calculation</li>
 *   <li>Equals method contract</li>
 *   <li>HashCode method contract and consistency with equals</li>
 *   <li>ToString method output</li>
 * </ul>
 * </p>
 *
 * @author Claims Triangle Accumulator Test Suite
 * @version 1.0.0
 */
@DisplayName("ClaimsTriangle Model Tests")
class ClaimsTriangleTest {

    // Test data constants
    private static final String VALID_PRODUCT = "Comp";
    private static final int VALID_EARLIEST_ORIGIN_YEAR = 1990;
    private static final int VALID_LATEST_DEVELOPMENT_YEAR = 1993;
    private static final int VALID_ORIGIN_YEAR = 1992;
    private static final int VALID_DEVELOPMENT_YEAR = 1993;
    private static final double VALID_INCREMENTAL_VALUE = 110.0;
    private static final double VALID_CUMULATIVE_VALUE = 280.0;

    /**
     * Tests for the ClaimsTriangle constructor.
     */
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create ClaimsTriangle with all valid fields")
        void constructor_withValidFields_createsClaimsTriangleSuccessfully() {
            // Arrange & Act
            ClaimsTriangle triangle = new ClaimsTriangle(
                VALID_PRODUCT,
                VALID_EARLIEST_ORIGIN_YEAR,
                VALID_LATEST_DEVELOPMENT_YEAR
            );

            // Assert
            assertThat(triangle).isNotNull();
            assertThat(triangle.getProductName()).isEqualTo(VALID_PRODUCT);
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(VALID_EARLIEST_ORIGIN_YEAR);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(VALID_LATEST_DEVELOPMENT_YEAR);
        }

        @Test
        @DisplayName("Should throw NullPointerException when product name is null")
        void constructor_withNullProductName_throwsNullPointerException() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> new ClaimsTriangle(
                null,
                VALID_EARLIEST_ORIGIN_YEAR,
                VALID_LATEST_DEVELOPMENT_YEAR
            ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Product name must not be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when product name is empty string")
        void constructor_withEmptyProductName_throwsIllegalArgumentException() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> new ClaimsTriangle(
                "",
                VALID_EARLIEST_ORIGIN_YEAR,
                VALID_LATEST_DEVELOPMENT_YEAR
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name must not be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when product name is whitespace only")
        void constructor_withWhitespaceOnlyProductName_throwsIllegalArgumentException() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> new ClaimsTriangle(
                "   ",
                VALID_EARLIEST_ORIGIN_YEAR,
                VALID_LATEST_DEVELOPMENT_YEAR
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name must not be empty");
        }

        @Test
        @DisplayName("Should trim leading and trailing whitespace from product name")
        void constructor_withWhitespaceAroundProductName_trimsWhitespace() {
            // Arrange
            String productWithWhitespace = "  Non-Comp  ";

            // Act
            ClaimsTriangle triangle = new ClaimsTriangle(
                productWithWhitespace,
                VALID_EARLIEST_ORIGIN_YEAR,
                VALID_LATEST_DEVELOPMENT_YEAR
            );

            // Assert
            assertThat(triangle.getProductName()).isEqualTo("Non-Comp");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when latestDevelopmentYear is less than earliestOriginYear")
        void constructor_withLatestYearLessThanEarliestYear_throwsIllegalArgumentException() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> new ClaimsTriangle(
                VALID_PRODUCT,
                1995,
                1990
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latest development year")
                .hasMessageContaining("must not be less than earliest origin year");
        }

        @Test
        @DisplayName("Should successfully create ClaimsTriangle when earliestOriginYear equals latestDevelopmentYear")
        void constructor_withEqualYears_createsClaimsTriangleSuccessfully() {
            // Arrange & Act
            ClaimsTriangle triangle = new ClaimsTriangle(
                VALID_PRODUCT,
                1992,
                1992
            );

            // Assert
            assertThat(triangle).isNotNull();
            assertThat(triangle.getEarliestOriginYear()).isEqualTo(1992);
            assertThat(triangle.getLatestDevelopmentYear()).isEqualTo(1992);
            assertThat(triangle.getNumberOfDevelopmentYears()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle product names with special characters")
        void constructor_withSpecialCharactersInProductName_createsTriangleSuccessfully() {
            // Arrange & Act
            ClaimsTriangle triangle = new ClaimsTriangle(
                "Comp & Liability",
                VALID_EARLIEST_ORIGIN_YEAR,
                VALID_LATEST_DEVELOPMENT_YEAR
            );

            // Assert
            assertThat(triangle.getProductName()).isEqualTo("Comp & Liability");
        }
    }

    /**
     * Tests for adding and retrieving incremental values.
     */
    @Nested
    @DisplayName("Incremental Value Tests")
    class IncrementalValueTests {

        @Test
        @DisplayName("Should add and retrieve single incremental value")
        void addIncrementalValue_withSingleValue_retrievesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.addIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, VALID_INCREMENTAL_VALUE);

            // Assert
            assertThat(triangle.getIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(VALID_INCREMENTAL_VALUE);
        }

        @Test
        @DisplayName("Should add multiple incremental values for same origin year, different development years")
        void addIncrementalValue_withMultipleValuesForSameOriginYear_retrievesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.addIncrementalValue(1992, 1992, 110.0);
            triangle.addIncrementalValue(1992, 1993, 170.0);

            // Assert
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(110.0);
            assertThat(triangle.getIncrementalValue(1992, 1993)).isEqualTo(170.0);
        }

        @Test
        @DisplayName("Should add multiple incremental values for different origin years")
        void addIncrementalValue_withMultipleOriginYears_retrievesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.addIncrementalValue(1990, 1990, 45.2);
            triangle.addIncrementalValue(1991, 1991, 50.0);
            triangle.addIncrementalValue(1992, 1992, 55.0);

            // Assert
            assertThat(triangle.getIncrementalValue(1990, 1990)).isEqualTo(45.2);
            assertThat(triangle.getIncrementalValue(1991, 1991)).isEqualTo(50.0);
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(55.0);
        }

        @Test
        @DisplayName("Should return 0.0 for non-existent incremental value")
        void getIncrementalValue_withNonExistentValue_returnsZero() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            double value = triangle.getIncrementalValue(1992, 1992);

            // Assert
            assertThat(value).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when adding NaN incremental value")
        void addIncrementalValue_withNaN_throwsIllegalArgumentException() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThatThrownBy(() -> triangle.addIncrementalValue(
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                Double.NaN
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incremental value must be finite")
                .hasMessageContaining("not NaN or infinite");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when adding positive infinity incremental value")
        void addIncrementalValue_withPositiveInfinity_throwsIllegalArgumentException() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThatThrownBy(() -> triangle.addIncrementalValue(
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                Double.POSITIVE_INFINITY
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incremental value must be finite")
                .hasMessageContaining("not NaN or infinite");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when adding negative infinity incremental value")
        void addIncrementalValue_withNegativeInfinity_throwsIllegalArgumentException() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThatThrownBy(() -> triangle.addIncrementalValue(
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                Double.NEGATIVE_INFINITY
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incremental value must be finite")
                .hasMessageContaining("not NaN or infinite");
        }

        @Test
        @DisplayName("Should accept negative incremental value (for recoveries)")
        void addIncrementalValue_withNegativeValue_acceptsValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.addIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, -50.0);

            // Assert
            assertThat(triangle.getIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(-50.0);
        }

        @Test
        @DisplayName("Should accept zero incremental value")
        void addIncrementalValue_withZeroValue_acceptsValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.addIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, 0.0);

            // Assert
            assertThat(triangle.getIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should overwrite existing incremental value when adding to same coordinates")
        void addIncrementalValue_withSameCoordinates_overwritesExistingValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();
            triangle.addIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, 100.0);

            // Act
            triangle.addIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, 200.0);

            // Assert
            assertThat(triangle.getIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should handle very small incremental values")
        void addIncrementalValue_withVerySmallValue_acceptsValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();
            double verySmallValue = 0.000001;

            // Act
            triangle.addIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, verySmallValue);

            // Assert
            assertThat(triangle.getIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(verySmallValue);
        }

        @Test
        @DisplayName("Should handle very large incremental values")
        void addIncrementalValue_withVeryLargeValue_acceptsValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();
            double veryLargeValue = 999_999_999.99;

            // Act
            triangle.addIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, veryLargeValue);

            // Assert
            assertThat(triangle.getIncrementalValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(veryLargeValue);
        }
    }

    /**
     * Tests for setting and retrieving cumulative values.
     */
    @Nested
    @DisplayName("Cumulative Value Tests")
    class CumulativeValueTests {

        @Test
        @DisplayName("Should set and retrieve single cumulative value")
        void setCumulativeValue_withSingleValue_retrievesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.setCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, VALID_CUMULATIVE_VALUE);

            // Assert
            assertThat(triangle.getCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(VALID_CUMULATIVE_VALUE);
        }

        @Test
        @DisplayName("Should set multiple cumulative values for same origin year, different development years")
        void setCumulativeValue_withMultipleValuesForSameOriginYear_retrievesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.setCumulativeValue(1992, 1992, 110.0);
            triangle.setCumulativeValue(1992, 1993, 280.0);

            // Assert
            assertThat(triangle.getCumulativeValue(1992, 1992)).isEqualTo(110.0);
            assertThat(triangle.getCumulativeValue(1992, 1993)).isEqualTo(280.0);
        }

        @Test
        @DisplayName("Should set multiple cumulative values for different origin years")
        void setCumulativeValue_withMultipleOriginYears_retrievesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.setCumulativeValue(1990, 1990, 45.2);
            triangle.setCumulativeValue(1991, 1991, 50.0);
            triangle.setCumulativeValue(1992, 1992, 55.0);

            // Assert
            assertThat(triangle.getCumulativeValue(1990, 1990)).isEqualTo(45.2);
            assertThat(triangle.getCumulativeValue(1991, 1991)).isEqualTo(50.0);
            assertThat(triangle.getCumulativeValue(1992, 1992)).isEqualTo(55.0);
        }

        @Test
        @DisplayName("Should return 0.0 for non-existent cumulative value")
        void getCumulativeValue_withNonExistentValue_returnsZero() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            double value = triangle.getCumulativeValue(1992, 1992);

            // Assert
            assertThat(value).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when setting NaN cumulative value")
        void setCumulativeValue_withNaN_throwsIllegalArgumentException() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThatThrownBy(() -> triangle.setCumulativeValue(
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                Double.NaN
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cumulative value must be finite")
                .hasMessageContaining("not NaN or infinite");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when setting positive infinity cumulative value")
        void setCumulativeValue_withPositiveInfinity_throwsIllegalArgumentException() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThatThrownBy(() -> triangle.setCumulativeValue(
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                Double.POSITIVE_INFINITY
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cumulative value must be finite")
                .hasMessageContaining("not NaN or infinite");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when setting negative infinity cumulative value")
        void setCumulativeValue_withNegativeInfinity_throwsIllegalArgumentException() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThatThrownBy(() -> triangle.setCumulativeValue(
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                Double.NEGATIVE_INFINITY
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cumulative value must be finite")
                .hasMessageContaining("not NaN or infinite");
        }

        @Test
        @DisplayName("Should accept negative cumulative value")
        void setCumulativeValue_withNegativeValue_acceptsValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.setCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, -50.0);

            // Assert
            assertThat(triangle.getCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(-50.0);
        }

        @Test
        @DisplayName("Should accept zero cumulative value")
        void setCumulativeValue_withZeroValue_acceptsValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.setCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, 0.0);

            // Assert
            assertThat(triangle.getCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should overwrite existing cumulative value when setting to same coordinates")
        void setCumulativeValue_withSameCoordinates_overwritesExistingValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();
            triangle.setCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, 100.0);

            // Act
            triangle.setCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR, 200.0);

            // Assert
            assertThat(triangle.getCumulativeValue(VALID_ORIGIN_YEAR, VALID_DEVELOPMENT_YEAR))
                .isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should independently store incremental and cumulative values")
        void addIncrementalAndSetCumulative_storesIndependently() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            triangle.addIncrementalValue(1992, 1992, 110.0);
            triangle.setCumulativeValue(1992, 1992, 110.0);

            // Assert
            assertThat(triangle.getIncrementalValue(1992, 1992)).isEqualTo(110.0);
            assertThat(triangle.getCumulativeValue(1992, 1992)).isEqualTo(110.0);
        }
    }

    /**
     * Tests for getter methods.
     */
    @Nested
    @DisplayName("Getter Tests")
    class GetterTests {

        @Test
        @DisplayName("Should return correct product name")
        void getProductName_returnsCorrectProductName() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            String productName = triangle.getProductName();

            // Assert
            assertThat(productName).isEqualTo(VALID_PRODUCT);
        }

        @Test
        @DisplayName("Should return correct earliest origin year")
        void getEarliestOriginYear_returnsCorrectYear() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            int earliestYear = triangle.getEarliestOriginYear();

            // Assert
            assertThat(earliestYear).isEqualTo(VALID_EARLIEST_ORIGIN_YEAR);
        }

        @Test
        @DisplayName("Should return correct latest development year")
        void getLatestDevelopmentYear_returnsCorrectYear() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            int latestYear = triangle.getLatestDevelopmentYear();

            // Assert
            assertThat(latestYear).isEqualTo(VALID_LATEST_DEVELOPMENT_YEAR);
        }

        @Test
        @DisplayName("Should calculate correct number of development years")
        void getNumberOfDevelopmentYears_calculatesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

            // Act
            int numberOfYears = triangle.getNumberOfDevelopmentYears();

            // Assert
            assertThat(numberOfYears).isEqualTo(4); // 1990, 1991, 1992, 1993
        }

        @Test
        @DisplayName("Should calculate 1 development year when earliest equals latest year")
        void getNumberOfDevelopmentYears_withEqualYears_returnsOne() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1992, 1992);

            // Act
            int numberOfYears = triangle.getNumberOfDevelopmentYears();

            // Assert
            assertThat(numberOfYears).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate correct number of development years for large range")
        void getNumberOfDevelopmentYears_withLargeRange_calculatesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 2020);

            // Act
            int numberOfYears = triangle.getNumberOfDevelopmentYears();

            // Assert
            assertThat(numberOfYears).isEqualTo(31); // 31 years inclusive
        }

        @Test
        @DisplayName("Should return trimmed product name when created with whitespace")
        void getProductName_withWhitespaceInConstructor_returnsTrimmedProductName() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle(
                "  Non-Comp  ",
                VALID_EARLIEST_ORIGIN_YEAR,
                VALID_LATEST_DEVELOPMENT_YEAR
            );

            // Act
            String productName = triangle.getProductName();

            // Assert
            assertThat(productName)
                .isEqualTo("Non-Comp")
                .doesNotStartWith(" ")
                .doesNotEndWith(" ");
        }
    }

    /**
     * Tests for the equals method.
     */
    @Nested
    @DisplayName("Equals Tests")
    class EqualsTests {

        @Test
        @DisplayName("Should return true when comparing same object (reflexive)")
        void equals_withSameObject_returnsTrue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThat(triangle.equals(triangle)).isTrue();
            assertThat(triangle).isEqualTo(triangle);
        }

        @Test
        @DisplayName("Should return true when all fields are equal (symmetric)")
        void equals_withEqualFields_returnsTrue() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);

            // Act & Assert
            assertThat(triangle1).isEqualTo(triangle2);
            assertThat(triangle2).isEqualTo(triangle1);
        }

        @Test
        @DisplayName("Should return true for transitive equality")
        void equals_withThreeEqualObjects_isTransitive() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle3 = new ClaimsTriangle("Comp", 1990, 1993);

            // Act & Assert
            assertThat(triangle1).isEqualTo(triangle2);
            assertThat(triangle2).isEqualTo(triangle3);
            assertThat(triangle1).isEqualTo(triangle3);
        }

        @Test
        @DisplayName("Should return false when product names are different")
        void equals_withDifferentProductNames_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Non-Comp", 1990, 1993);

            // Act & Assert
            assertThat(triangle1).isNotEqualTo(triangle2);
        }

        @Test
        @DisplayName("Should return false when earliest origin years are different")
        void equals_withDifferentEarliestOriginYears_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1991, 1993);

            // Act & Assert
            assertThat(triangle1).isNotEqualTo(triangle2);
        }

        @Test
        @DisplayName("Should return false when latest development years are different")
        void equals_withDifferentLatestDevelopmentYears_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1994);

            // Act & Assert
            assertThat(triangle1).isNotEqualTo(triangle2);
        }

        @Test
        @DisplayName("Should return false when incremental data is different")
        void equals_withDifferentIncrementalData_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.addIncrementalValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle2.addIncrementalValue(1992, 1992, 120.0);

            // Act & Assert
            assertThat(triangle1).isNotEqualTo(triangle2);
        }

        @Test
        @DisplayName("Should return false when cumulative data is different")
        void equals_withDifferentCumulativeData_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.setCumulativeValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle2.setCumulativeValue(1992, 1992, 120.0);

            // Act & Assert
            assertThat(triangle1).isNotEqualTo(triangle2);
        }

        @Test
        @DisplayName("Should return true when triangles have same incremental and cumulative data")
        void equals_withSameData_returnsTrue() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.addIncrementalValue(1992, 1992, 110.0);
            triangle1.setCumulativeValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle2.addIncrementalValue(1992, 1992, 110.0);
            triangle2.setCumulativeValue(1992, 1992, 110.0);

            // Act & Assert
            assertThat(triangle1).isEqualTo(triangle2);
        }

        @Test
        @DisplayName("Should return false when comparing with null")
        void equals_withNull_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act & Assert
            assertThat(triangle.equals(null)).isFalse();
            assertThat(triangle).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should return false when comparing with different class")
        void equals_withDifferentClass_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();
            String differentClassObject = "Not a ClaimsTriangle";

            // Act & Assert
            assertThat(triangle.equals(differentClassObject)).isFalse();
            assertThat(triangle).isNotEqualTo(differentClassObject);
        }

        @Test
        @DisplayName("Should return false when one triangle has incremental data and other doesn't")
        void equals_withOneHavingIncrementalData_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.addIncrementalValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);

            // Act & Assert
            assertThat(triangle1).isNotEqualTo(triangle2);
        }

        @Test
        @DisplayName("Should return false when one triangle has cumulative data and other doesn't")
        void equals_withOneHavingCumulativeData_returnsFalse() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.setCumulativeValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);

            // Act & Assert
            assertThat(triangle1).isNotEqualTo(triangle2);
        }
    }

    /**
     * Tests for the hashCode method.
     */
    @Nested
    @DisplayName("HashCode Tests")
    class HashCodeTests {

        @Test
        @DisplayName("Should return same hash code for equal objects")
        void hashCode_withEqualObjects_returnsSameHashCode() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);

            // Act
            int hashCode1 = triangle1.hashCode();
            int hashCode2 = triangle2.hashCode();

            // Assert
            assertThat(triangle1).isEqualTo(triangle2);
            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        @DisplayName("Should return same hash code when called multiple times on same object")
        void hashCode_calledMultipleTimes_returnsSameValue() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            int hashCode1 = triangle.hashCode();
            int hashCode2 = triangle.hashCode();
            int hashCode3 = triangle.hashCode();

            // Assert
            assertThat(hashCode1).isEqualTo(hashCode2);
            assertThat(hashCode2).isEqualTo(hashCode3);
        }

        @Test
        @DisplayName("Should typically return different hash codes for different objects")
        void hashCode_withDifferentObjects_typicallyReturnsDifferentHashCodes() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Non-Comp", 1991, 1994);

            // Act
            int hashCode1 = triangle1.hashCode();
            int hashCode2 = triangle2.hashCode();

            // Assert
            assertThat(hashCode1).isNotEqualTo(hashCode2);
        }

        @Test
        @DisplayName("Should verify equals-hashCode contract consistency")
        void hashCode_verifiesEqualsHashCodeContract() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle3 = new ClaimsTriangle("Non-Comp", 1990, 1993);

            // Act & Assert
            // If two objects are equal, their hash codes must be equal
            if (triangle1.equals(triangle2)) {
                assertThat(triangle1.hashCode()).isEqualTo(triangle2.hashCode());
            }

            // If two objects are not equal, they may or may not have different hash codes
            assertThat(triangle1).isNotEqualTo(triangle3);
        }

        @Test
        @DisplayName("Should produce different hash codes when only product name differs")
        void hashCode_withDifferentProductName_producesDifferentHashCode() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Non-Comp", 1990, 1993);

            // Act & Assert
            assertThat(triangle1.hashCode()).isNotEqualTo(triangle2.hashCode());
        }

        @Test
        @DisplayName("Should produce different hash codes when only years differ")
        void hashCode_withDifferentYears_producesDifferentHashCode() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1991, 1994);

            // Act & Assert
            assertThat(triangle1.hashCode()).isNotEqualTo(triangle2.hashCode());
        }

        @Test
        @DisplayName("Should produce same hash code for triangles with same data")
        void hashCode_withSameData_producesSameHashCode() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.addIncrementalValue(1992, 1992, 110.0);
            triangle1.setCumulativeValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle2.addIncrementalValue(1992, 1992, 110.0);
            triangle2.setCumulativeValue(1992, 1992, 110.0);

            // Act & Assert
            assertThat(triangle1.hashCode()).isEqualTo(triangle2.hashCode());
        }

        @Test
        @DisplayName("Should produce different hash codes for triangles with different incremental data")
        void hashCode_withDifferentIncrementalData_producesDifferentHashCode() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.addIncrementalValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle2.addIncrementalValue(1992, 1992, 120.0);

            // Act & Assert
            assertThat(triangle1.hashCode()).isNotEqualTo(triangle2.hashCode());
        }

        @Test
        @DisplayName("Should produce different hash codes for triangles with different cumulative data")
        void hashCode_withDifferentCumulativeData_producesDifferentHashCode() {
            // Arrange
            ClaimsTriangle triangle1 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle1.setCumulativeValue(1992, 1992, 110.0);

            ClaimsTriangle triangle2 = new ClaimsTriangle("Comp", 1990, 1993);
            triangle2.setCumulativeValue(1992, 1992, 120.0);

            // Act & Assert
            assertThat(triangle1.hashCode()).isNotEqualTo(triangle2.hashCode());
        }
    }

    /**
     * Tests for the toString method.
     */
    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain product name")
        void toString_containsProductName() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result).contains("Comp");
        }

        @Test
        @DisplayName("Should contain earliest origin year")
        void toString_containsEarliestOriginYear() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result).contains("1990");
        }

        @Test
        @DisplayName("Should contain latest development year")
        void toString_containsLatestDevelopmentYear() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result).contains("1993");
        }

        @Test
        @DisplayName("Should contain number of development years")
        void toString_containsNumberOfDevelopmentYears() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result).contains("4"); // 1990-1993 = 4 years
        }

        @Test
        @DisplayName("Should contain class name")
        void toString_containsClassName() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result).contains("ClaimsTriangle");
        }

        @Test
        @DisplayName("Should contain field names for clarity")
        void toString_containsFieldNames() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result)
                .contains("productName")
                .contains("earliestOriginYear")
                .contains("latestDevelopmentYear")
                .contains("developmentYears");
        }

        @Test
        @DisplayName("Should return non-empty string")
        void toString_returnsNonEmptyString() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("Should return consistent result when called multiple times")
        void toString_calledMultipleTimes_returnsConsistentResult() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();

            // Act
            String result1 = triangle.toString();
            String result2 = triangle.toString();
            String result3 = triangle.toString();

            // Assert
            assertThat(result1).isEqualTo(result2);
            assertThat(result2).isEqualTo(result3);
        }

        @Test
        @DisplayName("Should handle special characters in product name")
        void toString_withSpecialCharactersInProductName_handlesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp & Liability", 1990, 1993);

            // Act
            String result = triangle.toString();

            // Assert
            assertThat(result).contains("Comp & Liability");
        }

        @Test
        @DisplayName("Should not show internal data structures in output")
        void toString_doesNotShowInternalDataStructures() {
            // Arrange
            ClaimsTriangle triangle = createValidClaimsTriangle();
            triangle.addIncrementalValue(1992, 1992, 110.0);
            triangle.setCumulativeValue(1992, 1992, 110.0);

            // Act
            String result = triangle.toString();

            // Assert
            // Should not contain internal map representations
            assertThat(result).doesNotContain("incrementalData");
            assertThat(result).doesNotContain("cumulativeData");
        }
    }

    /**
     * Tests for the flattenCumulative method.
     */
    @Nested
    @DisplayName("FlattenCumulative Tests")
    class FlattenCumulativeTests {

        @Test
        @DisplayName("Should flatten single origin year triangle with single value")
        void flattenCumulative_withSingleOriginYearSingleValue_returnsCorrectList() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1993, 1993);
            triangle.setCumulativeValue(1993, 1993, 200.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(1);
            assertThat(flattened).containsExactly(200.0);
        }

        @Test
        @DisplayName("Should flatten single origin year triangle with multiple development years")
        void flattenCumulative_withSingleOriginYearMultipleDevelopmentYears_returnsCorrectList() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);
            triangle.setCumulativeValue(1990, 1990, 45.2);
            triangle.setCumulativeValue(1990, 1991, 110.0);
            triangle.setCumulativeValue(1990, 1992, 110.0);
            triangle.setCumulativeValue(1990, 1993, 147.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(10); // Triangular number: 4+3+2+1 = 10
            assertThat(flattened.subList(0, 4)).containsExactly(45.2, 110.0, 110.0, 147.0);
        }

        @Test
        @DisplayName("Should flatten multiple origin years triangle in correct row-major order")
        void flattenCumulative_withMultipleOriginYears_returnsCorrectRowMajorOrder() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1992, 1993);
            triangle.setCumulativeValue(1992, 1992, 110.0);
            triangle.setCumulativeValue(1992, 1993, 280.0);
            triangle.setCumulativeValue(1993, 1993, 200.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert - Total size should be 3 (2 + 1)
            assertThat(flattened).hasSize(3);
            // First row (origin 1992): 2 development years
            assertThat(flattened.get(0)).isEqualTo(110.0);
            assertThat(flattened.get(1)).isEqualTo(280.0);
            // Second row (origin 1993): 1 development year
            assertThat(flattened.get(2)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should flatten triangle with all zeros when no cumulative values are set")
        void flattenCumulative_withNoCumulativeValues_returnsListOfZeros() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1992, 1993);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(3); // 2 + 1
            assertThat(flattened).containsExactly(0.0, 0.0, 0.0);
        }

        @Test
        @DisplayName("Should verify correct list size using triangular number formula")
        void flattenCumulative_verifiesTriangularNumberFormula() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1995);
            int n = triangle.getNumberOfDevelopmentYears(); // 6 years

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert - Triangular number formula: n * (n + 1) / 2 = 6 * 7 / 2 = 21
            int expectedSize = n * (n + 1) / 2;
            assertThat(flattened).hasSize(expectedSize);
            assertThat(flattened).hasSize(21);
        }

        @Test
        @DisplayName("Should verify Comp example from CLAUDE.md documentation")
        void flattenCumulative_withCompExample_matchesDocumentation() {
            // Arrange - Comp example from CLAUDE.md
            // Origin 1990: [0.0, 0.0, 0.0, 0.0]
            // Origin 1991: [0.0, 0.0, 0.0]
            // Origin 1992: [110.0, 280.0]
            // Origin 1993: [200.0]
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);
            // Origin 1990 - all zeros (missing data)
            // Origin 1991 - all zeros (missing data)
            // Origin 1992
            triangle.setCumulativeValue(1992, 1992, 110.0);
            triangle.setCumulativeValue(1992, 1993, 280.0);
            // Origin 1993
            triangle.setCumulativeValue(1993, 1993, 200.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(10); // 4 + 3 + 2 + 1 = 10
            assertThat(flattened).containsExactly(
                0.0, 0.0, 0.0, 0.0,    // Origin 1990
                0.0, 0.0, 0.0,          // Origin 1991
                110.0, 280.0,           // Origin 1992
                200.0                   // Origin 1993
            );
        }

        @Test
        @DisplayName("Should verify Non-Comp example from CLAUDE.md documentation")
        void flattenCumulative_withNonCompExample_matchesDocumentation() {
            // Arrange - Non-Comp example from CLAUDE.md
            // Origin 1990: [45.2, 110.0, 110.0, 147.0]
            // Origin 1991: [50.0, 125.0, 150.0]
            // Origin 1992: [55.0, 140.0]
            // Origin 1993: [100.0]
            ClaimsTriangle triangle = new ClaimsTriangle("Non-Comp", 1990, 1993);
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

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(10); // 4 + 3 + 2 + 1 = 10
            assertThat(flattened).containsExactly(
                45.2, 110.0, 110.0, 147.0,  // Origin 1990
                50.0, 125.0, 150.0,          // Origin 1991
                55.0, 140.0,                 // Origin 1992
                100.0                        // Origin 1993
            );
        }

        @Test
        @DisplayName("Should flatten single year range triangle correctly")
        void flattenCumulative_withSingleYearRange_returnsOneValue() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 2000, 2000);
            triangle.setCumulativeValue(2000, 2000, 99.99);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(1);
            assertThat(flattened).containsExactly(99.99);
        }

        @Test
        @DisplayName("Should flatten large year range triangle correctly")
        void flattenCumulative_withLargeYearRange_calculatesCorrectSize() {
            // Arrange - 10 years span (1990-1999)
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1999);
            triangle.setCumulativeValue(1990, 1990, 100.0);
            triangle.setCumulativeValue(1999, 1999, 999.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert - Triangular number: 10 * 11 / 2 = 55
            assertThat(flattened).hasSize(55);
            // First value should be from origin 1990, dev 1990
            assertThat(flattened.get(0)).isEqualTo(100.0);
            // Last value should be from origin 1999, dev 1999
            assertThat(flattened.get(54)).isEqualTo(999.0);
        }

        @Test
        @DisplayName("Should preserve order with earliest origin year first")
        void flattenCumulative_preservesOrderWithEarliestOriginYearFirst() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1992);
            triangle.setCumulativeValue(1990, 1990, 1.0);
            triangle.setCumulativeValue(1991, 1991, 2.0);
            triangle.setCumulativeValue(1992, 1992, 3.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert - First value should be from earliest origin year (1990)
            assertThat(flattened.get(0)).isEqualTo(1.0);
            // Triangle structure: 3 + 2 + 1 = 6 values
            assertThat(flattened).hasSize(6);
        }

        @Test
        @DisplayName("Should return non-null list")
        void flattenCumulative_returnsNonNullList() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).isNotNull();
        }

        @Test
        @DisplayName("Should return mutable list that can be modified")
        void flattenCumulative_returnsMutableList() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1992, 1993);
            triangle.setCumulativeValue(1992, 1992, 110.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert - List should be mutable (can add elements)
            assertThat(flattened).isNotNull();
            flattened.add(999.0); // Should not throw UnsupportedOperationException
            assertThat(flattened).contains(999.0);
        }

        @Test
        @DisplayName("Should return independent lists on multiple calls")
        void flattenCumulative_calledMultipleTimes_returnsIndependentLists() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1992, 1993);
            triangle.setCumulativeValue(1992, 1992, 110.0);
            triangle.setCumulativeValue(1992, 1993, 280.0);

            // Act
            List<Double> flattened1 = triangle.flattenCumulative();
            List<Double> flattened2 = triangle.flattenCumulative();

            // Assert - Lists should be independent (not same reference)
            assertThat(flattened1).isNotSameAs(flattened2);
            assertThat(flattened1).isEqualTo(flattened2); // But contain same values

            // Modifying one should not affect the other
            flattened1.set(0, 999.0);
            assertThat(flattened1.get(0)).isEqualTo(999.0);
            assertThat(flattened2.get(0)).isEqualTo(110.0); // Should remain unchanged
        }

        @Test
        @DisplayName("Should handle mix of set and unset cumulative values")
        void flattenCumulative_withMixOfSetAndUnsetValues_fillsUnsetWithZeros() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1991, 1993);
            // Set some values, leave others unset (should default to 0.0)
            triangle.setCumulativeValue(1991, 1992, 100.0); // Set middle value
            triangle.setCumulativeValue(1993, 1993, 300.0); // Set last value

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(6); // 3 + 2 + 1
            assertThat(flattened).containsExactly(
                0.0, 100.0, 0.0,  // Origin 1991: unset, set, unset
                0.0, 0.0,          // Origin 1992: all unset
                300.0              // Origin 1993: set
            );
        }

        @Test
        @DisplayName("Should handle negative cumulative values correctly")
        void flattenCumulative_withNegativeValues_includesNegativeValues() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1992, 1993);
            triangle.setCumulativeValue(1992, 1992, -50.0);
            triangle.setCumulativeValue(1992, 1993, -100.0);
            triangle.setCumulativeValue(1993, 1993, 200.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).containsExactly(-50.0, -100.0, 200.0);
        }

        @Test
        @DisplayName("Should handle very small cumulative values")
        void flattenCumulative_withVerySmallValues_preservesPrecision() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1993, 1993);
            triangle.setCumulativeValue(1993, 1993, 0.000001);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(1);
            assertThat(flattened.get(0)).isEqualTo(0.000001);
        }

        @Test
        @DisplayName("Should handle very large cumulative values")
        void flattenCumulative_withVeryLargeValues_handlesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1993, 1993);
            double largeValue = 999_999_999.99;
            triangle.setCumulativeValue(1993, 1993, largeValue);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(1);
            assertThat(flattened.get(0)).isEqualTo(largeValue);
        }

        @Test
        @DisplayName("Should maintain correct order for triangle with sparse data")
        void flattenCumulative_withSparseData_maintainsCorrectOrder() {
            // Arrange - Only set values on the diagonal
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);
            triangle.setCumulativeValue(1990, 1990, 1.0);
            triangle.setCumulativeValue(1991, 1991, 2.0);
            triangle.setCumulativeValue(1992, 1992, 3.0);
            triangle.setCumulativeValue(1993, 1993, 4.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).hasSize(10);
            // Check diagonal values are in correct positions
            assertThat(flattened.get(0)).isEqualTo(1.0);  // Origin 1990, dev 1990 (pos 0)
            assertThat(flattened.get(4)).isEqualTo(2.0);  // Origin 1991, dev 1991 (pos 4)
            assertThat(flattened.get(7)).isEqualTo(3.0);  // Origin 1992, dev 1992 (pos 7)
            assertThat(flattened.get(9)).isEqualTo(4.0);  // Origin 1993, dev 1993 (pos 9)
        }

        @Test
        @DisplayName("Should verify triangular structure with 2-year range")
        void flattenCumulative_with2YearRange_verifies3Values() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 2000, 2001);
            triangle.setCumulativeValue(2000, 2000, 10.0);
            triangle.setCumulativeValue(2000, 2001, 20.0);
            triangle.setCumulativeValue(2001, 2001, 30.0);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert - 2 + 1 = 3 values
            assertThat(flattened).hasSize(3);
            assertThat(flattened).containsExactly(10.0, 20.0, 30.0);
        }

        @Test
        @DisplayName("Should verify triangular structure with 5-year range")
        void flattenCumulative_with5YearRange_verifies15Values() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1994);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert - 5 + 4 + 3 + 2 + 1 = 15 values
            assertThat(flattened).hasSize(15);
        }

        @Test
        @DisplayName("Should flatten consistent results when called multiple times without modifications")
        void flattenCumulative_calledMultipleTimesWithoutModifications_returnsConsistentResults() {
            // Arrange
            ClaimsTriangle triangle = buildNonCompTriangleFromDocumentation();

            // Act
            List<Double> flattened1 = triangle.flattenCumulative();
            List<Double> flattened2 = triangle.flattenCumulative();
            List<Double> flattened3 = triangle.flattenCumulative();

            // Assert
            assertThat(flattened1).isEqualTo(flattened2);
            assertThat(flattened2).isEqualTo(flattened3);
        }

        /**
         * Helper method to build Non-Comp triangle from CLAUDE.md documentation.
         *
         * @return ClaimsTriangle with Non-Comp example data
         */
        private ClaimsTriangle buildNonCompTriangleFromDocumentation() {
            ClaimsTriangle triangle = new ClaimsTriangle("Non-Comp", 1990, 1993);
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
            return triangle;
        }

        /**
         * Helper method to populate a triangle with specific cumulative values for testing.
         *
         * @param triangle the triangle to populate
         * @param values 2D array where values[originYearOffset][devYearOffset] contains the cumulative value
         */
        private void populateTriangleWithValues(ClaimsTriangle triangle, double[][] values) {
            int earliestOrigin = triangle.getEarliestOriginYear();
            int latestDev = triangle.getLatestDevelopmentYear();

            for (int originOffset = 0; originOffset <= (latestDev - earliestOrigin); originOffset++) {
                int originYear = earliestOrigin + originOffset;
                for (int devOffset = 0; devOffset <= (latestDev - originYear); devOffset++) {
                    int devYear = originYear + devOffset;
                    if (originOffset < values.length && devOffset < values[originOffset].length) {
                        triangle.setCumulativeValue(originYear, devYear, values[originOffset][devOffset]);
                    }
                }
            }
        }

        @Test
        @DisplayName("Should use helper method to verify custom triangle flattening")
        void flattenCumulative_withHelperMethod_verifiesCustomData() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("Test", 1990, 1991);
            double[][] values = {
                {100.0, 200.0},  // Origin 1990
                {50.0}           // Origin 1991
            };
            populateTriangleWithValues(triangle, values);

            // Act
            List<Double> flattened = triangle.flattenCumulative();

            // Assert
            assertThat(flattened).containsExactly(100.0, 200.0, 50.0);
        }
    }

    /**
     * Helper method to create a valid ClaimsTriangle for testing.
     *
     * @return a ClaimsTriangle with valid test data
     */
    private ClaimsTriangle createValidClaimsTriangle() {
        return new ClaimsTriangle(
            VALID_PRODUCT,
            VALID_EARLIEST_ORIGIN_YEAR,
            VALID_LATEST_DEVELOPMENT_YEAR
        );
    }
}
