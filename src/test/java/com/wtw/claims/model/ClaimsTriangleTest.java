package com.wtw.claims.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
