package com.wtw.claims.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for the ClaimRecord model class.
 *
 * <p>This test suite verifies:
 * <ul>
 *   <li>Constructor validation and field initialization</li>
 *   <li>Getter method correctness</li>
 *   <li>Equals method contract</li>
 *   <li>HashCode method contract and consistency with equals</li>
 *   <li>ToString method output</li>
 * </ul>
 * </p>
 *
 * @author Claims Triangle Accumulator Test Suite
 * @version 1.0.0
 */
@DisplayName("ClaimRecord Model Tests")
class ClaimRecordTest {

    // Test data constants
    private static final String VALID_PRODUCT = "Comp";
    private static final int VALID_ORIGIN_YEAR = 1992;
    private static final int VALID_DEVELOPMENT_YEAR = 1993;
    private static final double VALID_INCREMENTAL_VALUE = 110.0;

    /**
     * Tests for the ClaimRecord constructor.
     */
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should successfully create ClaimRecord with all valid fields")
        void constructor_withValidFields_createsClaimRecordSuccessfully() {
            // Arrange & Act
            ClaimRecord record = new ClaimRecord(
                VALID_PRODUCT,
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                VALID_INCREMENTAL_VALUE
            );

            // Assert
            assertThat(record).isNotNull();
            assertThat(record.product()).isEqualTo(VALID_PRODUCT);
            assertThat(record.originYear()).isEqualTo(VALID_ORIGIN_YEAR);
            assertThat(record.developmentYear()).isEqualTo(VALID_DEVELOPMENT_YEAR);
            assertThat(record.incrementalValue()).isEqualTo(VALID_INCREMENTAL_VALUE);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when product is null")
        void constructor_withNullProduct_throwsIllegalArgumentException() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> new ClaimRecord(
                null,
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                VALID_INCREMENTAL_VALUE
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name must not be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when product is empty string")
        void constructor_withEmptyProduct_throwsIllegalArgumentException() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> new ClaimRecord(
                "",
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                VALID_INCREMENTAL_VALUE
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name must not be null or empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when product is whitespace only")
        void constructor_withWhitespaceOnlyProduct_throwsIllegalArgumentException() {
            // Arrange, Act & Assert
            assertThatThrownBy(() -> new ClaimRecord(
                "   ",
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                VALID_INCREMENTAL_VALUE
            ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product name must not be null or empty");
        }

        @Test
        @DisplayName("Should trim leading and trailing whitespace from product name")
        void constructor_withWhitespaceAroundProduct_trimsWhitespace() {
            // Arrange
            String productWithWhitespace = "  Comp  ";

            // Act
            ClaimRecord record = new ClaimRecord(
                productWithWhitespace,
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                VALID_INCREMENTAL_VALUE
            );

            // Assert
            assertThat(record.product()).isEqualTo("Comp");
        }

        @Test
        @DisplayName("Should accept zero as incremental value")
        void constructor_withZeroIncrementalValue_createsClaimRecordSuccessfully() {
            // Arrange & Act
            ClaimRecord record = new ClaimRecord(
                VALID_PRODUCT,
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                0.0
            );

            // Assert
            assertThat(record.incrementalValue()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should accept negative incremental value")
        void constructor_withNegativeIncrementalValue_createsClaimRecordSuccessfully() {
            // Arrange & Act
            ClaimRecord record = new ClaimRecord(
                VALID_PRODUCT,
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                -50.0
            );

            // Assert
            assertThat(record.incrementalValue()).isEqualTo(-50.0);
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
        void getProduct_returnsCorrectProductName() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            String product = record.product();

            // Assert
            assertThat(product).isEqualTo(VALID_PRODUCT);
        }

        @Test
        @DisplayName("Should return correct origin year")
        void getOriginYear_returnsCorrectOriginYear() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            int originYear = record.originYear();

            // Assert
            assertThat(originYear).isEqualTo(VALID_ORIGIN_YEAR);
        }

        @Test
        @DisplayName("Should return correct development year")
        void getDevelopmentYear_returnsCorrectDevelopmentYear() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            int developmentYear = record.developmentYear();

            // Assert
            assertThat(developmentYear).isEqualTo(VALID_DEVELOPMENT_YEAR);
        }

        @Test
        @DisplayName("Should return correct incremental value")
        void getIncrementalValue_returnsCorrectIncrementalValue() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            double incrementalValue = record.incrementalValue();

            // Assert
            assertThat(incrementalValue).isEqualTo(VALID_INCREMENTAL_VALUE);
        }

        @Test
        @DisplayName("Should return trimmed product name when created with whitespace")
        void getProduct_withWhitespaceInConstructor_returnsTrimmedProduct() {
            // Arrange
            ClaimRecord record = new ClaimRecord(
                "  Non-Comp  ",
                VALID_ORIGIN_YEAR,
                VALID_DEVELOPMENT_YEAR,
                VALID_INCREMENTAL_VALUE
            );

            // Act
            String product = record.product();

            // Assert
            assertThat(product)
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
            ClaimRecord record = createValidClaimRecord();

            // Act & Assert
            assertThat(record.equals(record)).isTrue();
            assertThat(record).isEqualTo(record);
        }

        @Test
        @DisplayName("Should return true when all fields are equal (symmetric)")
        void equals_withEqualFields_returnsTrue() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1993, 110.0);

            // Act & Assert
            assertThat(record1).isEqualTo(record2);
            assertThat(record2).isEqualTo(record1);
        }

        @Test
        @DisplayName("Should return true for transitive equality")
        void equals_withThreeEqualObjects_isTransitive() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record3 = new ClaimRecord("Comp", 1992, 1993, 110.0);

            // Act & Assert
            assertThat(record1).isEqualTo(record2);
            assertThat(record2).isEqualTo(record3);
            assertThat(record1).isEqualTo(record3);
        }

        @Test
        @DisplayName("Should return false when product is different")
        void equals_withDifferentProduct_returnsFalse() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Non-Comp", 1992, 1993, 110.0);

            // Act & Assert
            assertThat(record1).isNotEqualTo(record2);
        }

        @Test
        @DisplayName("Should return false when origin year is different")
        void equals_withDifferentOriginYear_returnsFalse() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1993, 1993, 110.0);

            // Act & Assert
            assertThat(record1).isNotEqualTo(record2);
        }

        @Test
        @DisplayName("Should return false when development year is different")
        void equals_withDifferentDevelopmentYear_returnsFalse() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1994, 110.0);

            // Act & Assert
            assertThat(record1).isNotEqualTo(record2);
        }

        @Test
        @DisplayName("Should return false when incremental value is different")
        void equals_withDifferentIncrementalValue_returnsFalse() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1993, 120.0);

            // Act & Assert
            assertThat(record1).isNotEqualTo(record2);
        }

        @Test
        @DisplayName("Should return false when comparing with null")
        void equals_withNull_returnsFalse() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act & Assert
            assertThat(record.equals(null)).isFalse();
            assertThat(record).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Should return false when comparing with different class")
        void equals_withDifferentClass_returnsFalse() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();
            String differentClassObject = "Not a ClaimRecord";

            // Act & Assert
            assertThat(record.equals(differentClassObject)).isFalse();
            assertThat(record).isNotEqualTo(differentClassObject);
        }

        @Test
        @DisplayName("Should handle floating point comparison correctly")
        void equals_withVeryCloseFloatingPointValues_comparesCorrectly() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.00000001);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1993, 110.00000002);

            // Act & Assert
            assertThat(record1).isNotEqualTo(record2);
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
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1993, 110.0);

            // Act
            int hashCode1 = record1.hashCode();
            int hashCode2 = record2.hashCode();

            // Assert
            assertThat(record1).isEqualTo(record2);
            assertThat(hashCode1).isEqualTo(hashCode2);
        }

        @Test
        @DisplayName("Should return same hash code when called multiple times on same object")
        void hashCode_calledMultipleTimes_returnsSameValue() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            int hashCode1 = record.hashCode();
            int hashCode2 = record.hashCode();
            int hashCode3 = record.hashCode();

            // Assert
            assertThat(hashCode1).isEqualTo(hashCode2);
            assertThat(hashCode2).isEqualTo(hashCode3);
        }

        @Test
        @DisplayName("Should typically return different hash codes for different objects")
        void hashCode_withDifferentObjects_typicallyReturnsDifferentHashCodes() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Non-Comp", 1993, 1994, 120.0);

            // Act
            int hashCode1 = record1.hashCode();
            int hashCode2 = record2.hashCode();

            // Assert
            // Note: This is not guaranteed by the hashCode contract, but good implementations
            // should produce different hash codes for different objects most of the time
            assertThat(hashCode1).isNotEqualTo(hashCode2);
        }

        @Test
        @DisplayName("Should verify equals-hashCode contract consistency")
        void hashCode_verifiesEqualsHashCodeContract() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record3 = new ClaimRecord("Non-Comp", 1992, 1993, 110.0);

            // Act & Assert
            // If two objects are equal, their hash codes must be equal
            if (record1.equals(record2)) {
                assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
            }

            // If two objects are not equal, they may or may not have different hash codes
            // (but different hash codes is preferred for hash table performance)
            assertThat(record1).isNotEqualTo(record3);
        }

        @Test
        @DisplayName("Should produce different hash codes when only product differs")
        void hashCode_withDifferentProduct_producesDifferentHashCode() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Non-Comp", 1992, 1993, 110.0);

            // Act & Assert
            assertThat(record1.hashCode()).isNotEqualTo(record2.hashCode());
        }

        @Test
        @DisplayName("Should produce different hash codes when only years differ")
        void hashCode_withDifferentYears_producesDifferentHashCode() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1993, 1994, 110.0);

            // Act & Assert
            assertThat(record1.hashCode()).isNotEqualTo(record2.hashCode());
        }

        @Test
        @DisplayName("Should produce different hash codes when only incremental value differs")
        void hashCode_withDifferentIncrementalValue_producesDifferentHashCode() {
            // Arrange
            ClaimRecord record1 = new ClaimRecord("Comp", 1992, 1993, 110.0);
            ClaimRecord record2 = new ClaimRecord("Comp", 1992, 1993, 120.0);

            // Act & Assert
            assertThat(record1.hashCode()).isNotEqualTo(record2.hashCode());
        }
    }

    /**
     * Tests for the toString method.
     */
    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should contain all field values")
        void toString_containsAllFieldValues() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Comp", 1992, 1993, 110.5);

            // Act
            String result = record.toString();

            // Assert
            assertThat(result)
                .contains("Comp")
                .contains("1992")
                .contains("1993")
                .contains("110.5");
        }

        @Test
        @DisplayName("Should contain class name")
        void toString_containsClassName() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            String result = record.toString();

            // Assert
            assertThat(result).contains("ClaimRecord");
        }

        @Test
        @DisplayName("Should contain field names for clarity")
        void toString_containsFieldNames() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            String result = record.toString();

            // Assert
            assertThat(result)
                .contains("product")
                .contains("originYear")
                .contains("developmentYear")
                .contains("incrementalValue");
        }

        @Test
        @DisplayName("Should return non-empty string")
        void toString_returnsNonEmptyString() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            String result = record.toString();

            // Assert
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("Should return consistent result when called multiple times")
        void toString_calledMultipleTimes_returnsConsistentResult() {
            // Arrange
            ClaimRecord record = createValidClaimRecord();

            // Act
            String result1 = record.toString();
            String result2 = record.toString();
            String result3 = record.toString();

            // Assert
            assertThat(result1).isEqualTo(result2);
            assertThat(result2).isEqualTo(result3);
        }

        @Test
        @DisplayName("Should handle special characters in product name")
        void toString_withSpecialCharactersInProduct_handlesCorrectly() {
            // Arrange
            ClaimRecord record = new ClaimRecord("Non-Comp & Liability", 1992, 1993, 110.0);

            // Act
            String result = record.toString();

            // Assert
            assertThat(result).contains("Non-Comp & Liability");
        }
    }

    /**
     * Helper method to create a valid ClaimRecord for testing.
     *
     * @return a ClaimRecord with valid test data
     */
    private ClaimRecord createValidClaimRecord() {
        return new ClaimRecord(
            VALID_PRODUCT,
            VALID_ORIGIN_YEAR,
            VALID_DEVELOPMENT_YEAR,
            VALID_INCREMENTAL_VALUE
        );
    }
}
