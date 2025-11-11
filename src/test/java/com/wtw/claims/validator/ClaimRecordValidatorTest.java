package com.wtw.claims.validator;

import com.wtw.claims.model.ClaimRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Comprehensive test suite for ClaimRecordValidator.
 * <p>
 * Tests cover all validation methods including:
 * - validateRecord() with various valid and invalid scenarios
 * - isValidYear() boundary conditions
 * - isValidDevelopmentYear() temporal logic
 * - isValidValue() finite number validation
 * </p>
 *
 * @author Claims Triangle Accumulator Test Suite
 * @version 1.0.0
 */
@DisplayName("ClaimRecordValidator")
class ClaimRecordValidatorTest {

    private ClaimRecordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ClaimRecordValidator();
    }

    @Nested
    @DisplayName("validateRecord() Tests")
    class ValidateRecordTests {

        @Test
        @DisplayName("Should pass validation for completely valid record")
        void validateRecord_withValidRecord_shouldNotThrowException() {
            // Arrange
            ClaimRecord validRecord = new ClaimRecord("Comp", 2000, 2005, 150.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(validRecord));
        }

        @Test
        @DisplayName("Should throw NullPointerException when record is null")
        void validateRecord_withNullRecord_shouldThrowNullPointerException() {
            // Arrange
            ClaimRecord nullRecord = null;

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(nullRecord))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("ClaimRecord cannot be null");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when origin year is below MIN_YEAR (1900)")
        void validateRecord_withOriginYearBelowMin_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithInvalidOriginYear = new ClaimRecord("Comp", 1899, 1900, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithInvalidOriginYear))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Origin year (1899)")
                    .hasMessageContaining("outside valid range [1900-2100]");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when origin year is above MAX_YEAR (2100)")
        void validateRecord_withOriginYearAboveMax_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithInvalidOriginYear = new ClaimRecord("Comp", 2101, 2101, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithInvalidOriginYear))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Origin year (2101)")
                    .hasMessageContaining("outside valid range [1900-2100]");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when development year is below MIN_YEAR (1900)")
        void validateRecord_withDevelopmentYearBelowMin_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithInvalidDevYear = new ClaimRecord("Comp", 1900, 1899, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithInvalidDevYear))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Development year (1899)")
                    .hasMessageContaining("outside valid range [1900-2100]");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when development year is above MAX_YEAR (2100)")
        void validateRecord_withDevelopmentYearAboveMax_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithInvalidDevYear = new ClaimRecord("Comp", 2100, 2101, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithInvalidDevYear))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Development year (2101)")
                    .hasMessageContaining("outside valid range [1900-2100]");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when development year is before origin year")
        void validateRecord_withDevelopmentYearBeforeOriginYear_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithInvalidDevYear = new ClaimRecord("Comp", 2000, 1995, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithInvalidDevYear))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Development year (1995)")
                    .hasMessageContaining("cannot be before origin year (2000)");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when incremental value is NaN")
        void validateRecord_withNaNValue_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithNaN = new ClaimRecord("Comp", 2000, 2000, Double.NaN);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithNaN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Incremental value (NaN)")
                    .hasMessageContaining("must be a finite number");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when incremental value is POSITIVE_INFINITY")
        void validateRecord_withPositiveInfinity_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithPosInfinity = new ClaimRecord("Comp", 2000, 2000, Double.POSITIVE_INFINITY);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithPosInfinity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Incremental value (Infinity)")
                    .hasMessageContaining("must be a finite number");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when incremental value is NEGATIVE_INFINITY")
        void validateRecord_withNegativeInfinity_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord recordWithNegInfinity = new ClaimRecord("Comp", 2000, 2000, Double.NEGATIVE_INFINITY);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(recordWithNegInfinity))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Incremental value (-Infinity)")
                    .hasMessageContaining("must be a finite number");
        }

        @Test
        @DisplayName("Should pass validation when incremental value is negative (salvage recovery scenario)")
        void validateRecord_withNegativeValue_shouldNotThrowException() {
            // Arrange
            ClaimRecord recordWithNegativeValue = new ClaimRecord("Comp", 2000, 2005, -50.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(recordWithNegativeValue));
        }

        @Test
        @DisplayName("Should pass validation when incremental value is zero")
        void validateRecord_withZeroValue_shouldNotThrowException() {
            // Arrange
            ClaimRecord recordWithZeroValue = new ClaimRecord("Comp", 2000, 2005, 0.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(recordWithZeroValue));
        }

        @Test
        @DisplayName("Should pass validation for record at MIN_YEAR boundary (1900)")
        void validateRecord_withMinYearBoundary_shouldNotThrowException() {
            // Arrange
            ClaimRecord recordAtMinYear = new ClaimRecord("Comp", 1900, 1900, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(recordAtMinYear));
        }

        @Test
        @DisplayName("Should pass validation for record at MAX_YEAR boundary (2100)")
        void validateRecord_withMaxYearBoundary_shouldNotThrowException() {
            // Arrange
            ClaimRecord recordAtMaxYear = new ClaimRecord("Comp", 2100, 2100, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(recordAtMaxYear));
        }

        @Test
        @DisplayName("Should pass validation when development year equals origin year")
        void validateRecord_withDevelopmentYearEqualToOriginYear_shouldNotThrowException() {
            // Arrange
            ClaimRecord recordWithSameYears = new ClaimRecord("Comp", 2000, 2000, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(recordWithSameYears));
        }
    }

    @Nested
    @DisplayName("Year Validation Tests (via validateRecord)")
    class YearValidationTests {

        @Test
        @DisplayName("Should accept year 1900 (MIN_YEAR boundary) for origin year")
        void validateRecord_withOriginYearAtMinBoundary_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 1900, 1900, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept year 2100 (MAX_YEAR boundary) for origin year")
        void validateRecord_withOriginYearAtMaxBoundary_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2100, 2100, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept year 2000 (middle of valid range) for origin year")
        void validateRecord_withOriginYearInMiddleRange_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2000, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept year 1900 (MIN_YEAR boundary) for development year")
        void validateRecord_withDevelopmentYearAtMinBoundary_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 1900, 1900, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept year 2100 (MAX_YEAR boundary) for development year")
        void validateRecord_withDevelopmentYearAtMaxBoundary_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2100, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should reject year 0 for origin year")
        void validateRecord_withOriginYearZero_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 0, 2000, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(record))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Origin year (0)")
                    .hasMessageContaining("outside valid range");
        }

        @Test
        @DisplayName("Should reject large negative year for origin year")
        void validateRecord_withOriginYearNegative_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", -1000, 2000, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(record))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Origin year (-1000)")
                    .hasMessageContaining("outside valid range");
        }

        @Test
        @DisplayName("Should reject year far in the future for origin year")
        void validateRecord_withOriginYearFarFuture_shouldThrowIllegalArgumentException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 3000, 3000, 100.0);

            // Act & Assert
            assertThatThrownBy(() -> validator.validateRecord(record))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Origin year (3000)")
                    .hasMessageContaining("outside valid range");
        }
    }

    @Nested
    @DisplayName("Development Year Temporal Logic Tests (via validateRecord)")
    class DevelopmentYearTemporalTests {

        @Test
        @DisplayName("Should accept development year equal to origin year")
        void validateRecord_withDevelopmentYearEqualToOrigin_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2000, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept development year greater than origin year")
        void validateRecord_withDevelopmentYearAfterOrigin_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2005, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept large gap between origin and development year")
        void validateRecord_withLargeYearGap_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 1990, 2020, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept development year one year after origin")
        void validateRecord_withDevelopmentYearOneYearAfter_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2001, 100.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }
    }

    @Nested
    @DisplayName("Incremental Value Tests (via validateRecord)")
    class IncrementalValueTests {

        @Test
        @DisplayName("Should accept positive value")
        void validateRecord_withPositiveValue_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2000, 150.0);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept very small positive value")
        void validateRecord_withVerySmallPositiveValue_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2000, 0.0001);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept very large finite value")
        void validateRecord_withVeryLargeFiniteValue_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2000, 1.7E308);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept Double.MAX_VALUE")
        void validateRecord_withDoubleMaxValue_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2000, Double.MAX_VALUE);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }

        @Test
        @DisplayName("Should accept Double.MIN_VALUE")
        void validateRecord_withDoubleMinValue_shouldNotThrowException() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 2000, 2000, Double.MIN_VALUE);

            // Act & Assert
            assertThatNoException()
                    .isThrownBy(() -> validator.validateRecord(record));
        }
    }
}
