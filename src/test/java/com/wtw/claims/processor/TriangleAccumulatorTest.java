package com.wtw.claims.processor;

import com.wtw.claims.model.ClaimsTriangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test suite for {@link TriangleAccumulator}.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Constructor validation (utility class pattern)</li>
 *   <li>Basic cumulative calculation functionality</li>
 *   <li>Cumulative calculation correctness with various patterns</li>
 *   <li>Edge cases (empty data, negatives, precision, etc.)</li>
 *   <li>Validation and error handling</li>
 *   <li>Real-world scenarios from CLAUDE.md examples</li>
 * </ul>
 *
 * @author Claims Processing System - Test Suite
 * @version 1.0.0
 */
@DisplayName("TriangleAccumulator Tests")
class TriangleAccumulatorTest {

    // ========================================
    // Constructor Tests
    // ========================================

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw UnsupportedOperationException when constructor is invoked via reflection")
        void constructor_whenInvokedViaReflection_throwsUnsupportedOperationException() throws Exception {
            // Arrange
            Constructor<TriangleAccumulator> constructor = TriangleAccumulator.class.getDeclaredConstructor();
            constructor.setAccessible(true);

            // Act & Assert
            assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .cause()
                .hasMessageContaining("Utility class cannot be instantiated");
        }

        @Test
        @DisplayName("Should have private constructor that is not accessible")
        void constructor_isPrivateAndNotAccessible() throws Exception {
            // Arrange
            Constructor<TriangleAccumulator> constructor = TriangleAccumulator.class.getDeclaredConstructor();

            // Assert
            assertThat(constructor.canAccess(null)).isFalse();
        }
    }

    // ========================================
    // Basic Functionality Tests
    // ========================================

    @Nested
    @DisplayName("calculateCumulative() - Basic Functionality")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should calculate cumulative for single origin year with consecutive development years")
        void calculateCumulative_singleOriginYear_consecutiveDevelopmentYears() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 1992, 1994);
            triangle.addIncrementalValue(1992, 1992, 100.0);
            triangle.addIncrementalValue(1992, 1993, 200.0);
            triangle.addIncrementalValue(1992, 1994, 300.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert
            assertThat(triangle.getCumulativeValue(1992, 1992)).isEqualTo(100.0);
            assertThat(triangle.getCumulativeValue(1992, 1993)).isEqualTo(300.0);
            assertThat(triangle.getCumulativeValue(1992, 1994)).isEqualTo(600.0);
        }

        @Test
        @DisplayName("Should calculate cumulative for multiple origin years")
        void calculateCumulative_multipleOriginYears_calculatesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 1990, 1992);
            triangle.addIncrementalValue(1990, 1990, 50.0);
            triangle.addIncrementalValue(1990, 1991, 75.0);
            triangle.addIncrementalValue(1990, 1992, 25.0);
            triangle.addIncrementalValue(1991, 1991, 100.0);
            triangle.addIncrementalValue(1991, 1992, 150.0);
            triangle.addIncrementalValue(1992, 1992, 200.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Origin Year 1990
            assertThat(triangle.getCumulativeValue(1990, 1990)).isEqualTo(50.0);
            assertThat(triangle.getCumulativeValue(1990, 1991)).isEqualTo(125.0);
            assertThat(triangle.getCumulativeValue(1990, 1992)).isEqualTo(150.0);

            // Assert - Origin Year 1991
            assertThat(triangle.getCumulativeValue(1991, 1991)).isEqualTo(100.0);
            assertThat(triangle.getCumulativeValue(1991, 1992)).isEqualTo(250.0);

            // Assert - Origin Year 1992
            assertThat(triangle.getCumulativeValue(1992, 1992)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should verify cumulative values are correctly stored in the triangle")
        void calculateCumulative_verifyCumulativeValuesStoredCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 2000, 2002);
            triangle.addIncrementalValue(2000, 2000, 10.0);
            triangle.addIncrementalValue(2000, 2001, 20.0);
            triangle.addIncrementalValue(2000, 2002, 30.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Verify all cumulative values are present
            assertThat(triangle.getCumulativeValue(2000, 2000)).isNotZero();
            assertThat(triangle.getCumulativeValue(2000, 2001)).isNotZero();
            assertThat(triangle.getCumulativeValue(2000, 2002)).isNotZero();
        }

        @Test
        @DisplayName("Should not modify incremental values after calculation")
        void calculateCumulative_incrementalValuesRemainUnchanged() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 1995, 1997);
            triangle.addIncrementalValue(1995, 1995, 111.0);
            triangle.addIncrementalValue(1995, 1996, 222.0);
            triangle.addIncrementalValue(1995, 1997, 333.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Incremental values should remain unchanged
            assertThat(triangle.getIncrementalValue(1995, 1995)).isEqualTo(111.0);
            assertThat(triangle.getIncrementalValue(1995, 1996)).isEqualTo(222.0);
            assertThat(triangle.getIncrementalValue(1995, 1997)).isEqualTo(333.0);
        }

        @Test
        @DisplayName("Should handle empty triangle with no incremental data - all cumulative values should be 0.0")
        void calculateCumulative_emptyTriangle_allCumulativeValuesAreZero() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("EmptyProduct", 2000, 2003);
            // No incremental values added

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - All cumulative values should be 0.0
            assertThat(triangle.getCumulativeValue(2000, 2000)).isZero();
            assertThat(triangle.getCumulativeValue(2000, 2001)).isZero();
            assertThat(triangle.getCumulativeValue(2000, 2002)).isZero();
            assertThat(triangle.getCumulativeValue(2000, 2003)).isZero();
            assertThat(triangle.getCumulativeValue(2001, 2001)).isZero();
            assertThat(triangle.getCumulativeValue(2001, 2002)).isZero();
            assertThat(triangle.getCumulativeValue(2001, 2003)).isZero();
            assertThat(triangle.getCumulativeValue(2002, 2002)).isZero();
            assertThat(triangle.getCumulativeValue(2002, 2003)).isZero();
            assertThat(triangle.getCumulativeValue(2003, 2003)).isZero();
        }
    }

    // ========================================
    // Cumulative Calculation Correctness Tests
    // ========================================

    @Nested
    @DisplayName("calculateCumulative() - Calculation Correctness")
    class CalculationCorrectnessTests {

        @Test
        @DisplayName("Should handle single origin year with missing development years - cumulative should stay same")
        void calculateCumulative_singleOriginYearWithGaps_cumulativeStaysSame() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 1990, 1994);
            triangle.addIncrementalValue(1990, 1990, 100.0);
            triangle.addIncrementalValue(1990, 1992, 50.0);  // Gap at 1991
            triangle.addIncrementalValue(1990, 1994, 25.0);  // Gap at 1993

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert
            assertThat(triangle.getCumulativeValue(1990, 1990)).isEqualTo(100.0);
            assertThat(triangle.getCumulativeValue(1990, 1991)).isEqualTo(100.0);  // No increment, stays same
            assertThat(triangle.getCumulativeValue(1990, 1992)).isEqualTo(150.0);
            assertThat(triangle.getCumulativeValue(1990, 1993)).isEqualTo(150.0);  // No increment, stays same
            assertThat(triangle.getCumulativeValue(1990, 1994)).isEqualTo(175.0);
        }

        @Test
        @DisplayName("Should handle multiple origin years with different incremental patterns")
        void calculateCumulative_multipleOriginYearsDifferentPatterns_calculatesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 2000, 2003);

            // Origin 2000: Full data
            triangle.addIncrementalValue(2000, 2000, 10.0);
            triangle.addIncrementalValue(2000, 2001, 10.0);
            triangle.addIncrementalValue(2000, 2002, 10.0);
            triangle.addIncrementalValue(2000, 2003, 10.0);

            // Origin 2001: Partial data with gap
            triangle.addIncrementalValue(2001, 2001, 20.0);
            // Gap at 2002
            triangle.addIncrementalValue(2001, 2003, 30.0);

            // Origin 2002: Only first value
            triangle.addIncrementalValue(2002, 2002, 50.0);

            // Origin 2003: Single value
            triangle.addIncrementalValue(2003, 2003, 100.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Origin 2000
            assertThat(triangle.getCumulativeValue(2000, 2000)).isEqualTo(10.0);
            assertThat(triangle.getCumulativeValue(2000, 2001)).isEqualTo(20.0);
            assertThat(triangle.getCumulativeValue(2000, 2002)).isEqualTo(30.0);
            assertThat(triangle.getCumulativeValue(2000, 2003)).isEqualTo(40.0);

            // Assert - Origin 2001
            assertThat(triangle.getCumulativeValue(2001, 2001)).isEqualTo(20.0);
            assertThat(triangle.getCumulativeValue(2001, 2002)).isEqualTo(20.0);  // Gap
            assertThat(triangle.getCumulativeValue(2001, 2003)).isEqualTo(50.0);

            // Assert - Origin 2002
            assertThat(triangle.getCumulativeValue(2002, 2002)).isEqualTo(50.0);
            assertThat(triangle.getCumulativeValue(2002, 2003)).isEqualTo(50.0);

            // Assert - Origin 2003
            assertThat(triangle.getCumulativeValue(2003, 2003)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should match expected Non-Comp calculations from CLAUDE.md")
        void calculateCumulative_nonCompExample_matchesCLAUDEmdCalculations() {
            // Arrange - Non-Comp data from CLAUDE.md
            ClaimsTriangle triangle = new ClaimsTriangle("Non-Comp", 1990, 1993);

            // Origin 1990: Dev 1990: 45.2, Dev 1991: 64.8, Dev 1993: 37.0
            triangle.addIncrementalValue(1990, 1990, 45.2);
            triangle.addIncrementalValue(1990, 1991, 64.8);
            triangle.addIncrementalValue(1990, 1993, 37.0);

            // Origin 1991: Dev 1991: 50.0, Dev 1992: 75.0, Dev 1993: 25.0
            triangle.addIncrementalValue(1991, 1991, 50.0);
            triangle.addIncrementalValue(1991, 1992, 75.0);
            triangle.addIncrementalValue(1991, 1993, 25.0);

            // Origin 1992: Dev 1992: 55.0, Dev 1993: 85.0
            triangle.addIncrementalValue(1992, 1992, 55.0);
            triangle.addIncrementalValue(1992, 1993, 85.0);

            // Origin 1993: Dev 1993: 100.0
            triangle.addIncrementalValue(1993, 1993, 100.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Origin 1990: [45.2, 110.0, 110.0, 147.0]
            assertThat(triangle.getCumulativeValue(1990, 1990)).isEqualTo(45.2);
            assertThat(triangle.getCumulativeValue(1990, 1991)).isEqualTo(110.0);
            assertThat(triangle.getCumulativeValue(1990, 1992)).isEqualTo(110.0);  // Missing incremental
            assertThat(triangle.getCumulativeValue(1990, 1993)).isEqualTo(147.0);

            // Assert - Origin 1991: [50.0, 125.0, 150.0]
            assertThat(triangle.getCumulativeValue(1991, 1991)).isEqualTo(50.0);
            assertThat(triangle.getCumulativeValue(1991, 1992)).isEqualTo(125.0);
            assertThat(triangle.getCumulativeValue(1991, 1993)).isEqualTo(150.0);

            // Assert - Origin 1992: [55.0, 140.0]
            assertThat(triangle.getCumulativeValue(1992, 1992)).isEqualTo(55.0);
            assertThat(triangle.getCumulativeValue(1992, 1993)).isEqualTo(140.0);

            // Assert - Origin 1993: [100.0]
            assertThat(triangle.getCumulativeValue(1993, 1993)).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should match expected Comp calculations from CLAUDE.md")
        void calculateCumulative_compExample_matchesCLAUDEmdCalculations() {
            // Arrange - Comp data from CLAUDE.md
            ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

            // Origin 1992: Dev 1992: 110.0, Dev 1993: 170.0
            triangle.addIncrementalValue(1992, 1992, 110.0);
            triangle.addIncrementalValue(1992, 1993, 170.0);

            // Origin 1993: Dev 1993: 200.0
            triangle.addIncrementalValue(1993, 1993, 200.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Origin 1990: [0, 0, 0, 0] (no data)
            assertThat(triangle.getCumulativeValue(1990, 1990)).isZero();
            assertThat(triangle.getCumulativeValue(1990, 1991)).isZero();
            assertThat(triangle.getCumulativeValue(1990, 1992)).isZero();
            assertThat(triangle.getCumulativeValue(1990, 1993)).isZero();

            // Assert - Origin 1991: [0, 0, 0] (no data)
            assertThat(triangle.getCumulativeValue(1991, 1991)).isZero();
            assertThat(triangle.getCumulativeValue(1991, 1992)).isZero();
            assertThat(triangle.getCumulativeValue(1991, 1993)).isZero();

            // Assert - Origin 1992: [110.0, 280.0]
            assertThat(triangle.getCumulativeValue(1992, 1992)).isEqualTo(110.0);
            assertThat(triangle.getCumulativeValue(1992, 1993)).isEqualTo(280.0);

            // Assert - Origin 1993: [200.0]
            assertThat(triangle.getCumulativeValue(1993, 1993)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should verify cumulative resets to 0.0 for each new origin year")
        void calculateCumulative_cumulativeResetsForEachOriginYear() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 2010, 2012);
            triangle.addIncrementalValue(2010, 2010, 1000.0);
            triangle.addIncrementalValue(2010, 2011, 2000.0);
            triangle.addIncrementalValue(2010, 2012, 3000.0);
            triangle.addIncrementalValue(2011, 2011, 100.0);  // Should start fresh at 100.0, not continue from 6000.0
            triangle.addIncrementalValue(2011, 2012, 200.0);
            triangle.addIncrementalValue(2012, 2012, 50.0);   // Should start fresh at 50.0

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Origin 2010
            assertThat(triangle.getCumulativeValue(2010, 2012)).isEqualTo(6000.0);

            // Assert - Origin 2011 starts fresh (not continuing from 6000.0)
            assertThat(triangle.getCumulativeValue(2011, 2011)).isEqualTo(100.0);
            assertThat(triangle.getCumulativeValue(2011, 2012)).isEqualTo(300.0);

            // Assert - Origin 2012 starts fresh
            assertThat(triangle.getCumulativeValue(2012, 2012)).isEqualTo(50.0);
        }
    }

    // ========================================
    // Edge Cases Tests
    // ========================================

    @Nested
    @DisplayName("calculateCumulative() - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle triangle with all zero incremental values - cumulative should all be 0.0")
        void calculateCumulative_allZeroIncrementalValues_cumulativeAllZero() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("ZeroProduct", 2000, 2002);
            triangle.addIncrementalValue(2000, 2000, 0.0);
            triangle.addIncrementalValue(2000, 2001, 0.0);
            triangle.addIncrementalValue(2000, 2002, 0.0);
            triangle.addIncrementalValue(2001, 2001, 0.0);
            triangle.addIncrementalValue(2001, 2002, 0.0);
            triangle.addIncrementalValue(2002, 2002, 0.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert
            assertThat(triangle.getCumulativeValue(2000, 2000)).isZero();
            assertThat(triangle.getCumulativeValue(2000, 2001)).isZero();
            assertThat(triangle.getCumulativeValue(2000, 2002)).isZero();
            assertThat(triangle.getCumulativeValue(2001, 2001)).isZero();
            assertThat(triangle.getCumulativeValue(2001, 2002)).isZero();
            assertThat(triangle.getCumulativeValue(2002, 2002)).isZero();
        }

        @Test
        @DisplayName("Should handle negative incremental values (recoveries) - cumulative should decrease")
        void calculateCumulative_negativeIncrementalValues_cumulativeDecreases() {
            // Arrange - Simulating claim recoveries or adjustments
            ClaimsTriangle triangle = new ClaimsTriangle("RecoveryProduct", 2000, 2003);
            triangle.addIncrementalValue(2000, 2000, 1000.0);
            triangle.addIncrementalValue(2000, 2001, -200.0);  // Recovery
            triangle.addIncrementalValue(2000, 2002, 500.0);
            triangle.addIncrementalValue(2000, 2003, -100.0);  // Another recovery

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert
            assertThat(triangle.getCumulativeValue(2000, 2000)).isEqualTo(1000.0);
            assertThat(triangle.getCumulativeValue(2000, 2001)).isEqualTo(800.0);   // 1000 - 200
            assertThat(triangle.getCumulativeValue(2000, 2002)).isEqualTo(1300.0);  // 800 + 500
            assertThat(triangle.getCumulativeValue(2000, 2003)).isEqualTo(1200.0);  // 1300 - 100
        }

        @Test
        @DisplayName("Should handle single year range (earliestOrigin == latestDevelopment)")
        void calculateCumulative_singleYearRange_calculatesCorrectly() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("SingleYearProduct", 2020, 2020);
            triangle.addIncrementalValue(2020, 2020, 999.99);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert
            assertThat(triangle.getCumulativeValue(2020, 2020)).isEqualTo(999.99);
        }

        @Test
        @DisplayName("Should handle large year range with sparse data")
        void calculateCumulative_largeYearRangeSparseData_calculatesCorrectly() {
            // Arrange - 20 year span with only a few data points
            ClaimsTriangle triangle = new ClaimsTriangle("SparseProduct", 2000, 2020);
            triangle.addIncrementalValue(2000, 2000, 100.0);
            triangle.addIncrementalValue(2000, 2010, 50.0);  // 10 year gap
            triangle.addIncrementalValue(2000, 2020, 25.0);  // Another 10 year gap

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert
            assertThat(triangle.getCumulativeValue(2000, 2000)).isEqualTo(100.0);
            assertThat(triangle.getCumulativeValue(2000, 2005)).isEqualTo(100.0);  // No data, stays same
            assertThat(triangle.getCumulativeValue(2000, 2010)).isEqualTo(150.0);
            assertThat(triangle.getCumulativeValue(2000, 2015)).isEqualTo(150.0);  // No data, stays same
            assertThat(triangle.getCumulativeValue(2000, 2020)).isEqualTo(175.0);
        }

        @Test
        @DisplayName("Should handle precision with decimal values")
        void calculateCumulative_precisionWithDecimals_calculatesAccurately() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("PrecisionProduct", 2000, 2002);
            triangle.addIncrementalValue(2000, 2000, 123.456);
            triangle.addIncrementalValue(2000, 2001, 789.012);
            triangle.addIncrementalValue(2000, 2002, 345.678);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Using isCloseTo for floating-point comparison
            assertThat(triangle.getCumulativeValue(2000, 2000)).isCloseTo(123.456, within(0.001));
            assertThat(triangle.getCumulativeValue(2000, 2001)).isCloseTo(912.468, within(0.001));
            assertThat(triangle.getCumulativeValue(2000, 2002)).isCloseTo(1258.146, within(0.001));
        }

        @Test
        @DisplayName("Should verify that development years before origin year are not processed")
        void calculateCumulative_developmentYearsBeforeOriginYear_notProcessed() {
            // Arrange
            ClaimsTriangle triangle = new ClaimsTriangle("TestProduct", 1990, 1995);

            // Add data for origin year 1993
            triangle.addIncrementalValue(1993, 1993, 100.0);
            triangle.addIncrementalValue(1993, 1994, 150.0);
            triangle.addIncrementalValue(1993, 1995, 200.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Years 1990, 1991, 1992 should have cumulative values set to 0.0
            // (because algorithm processes all origin years from earliest to latest)
            assertThat(triangle.getCumulativeValue(1990, 1990)).isZero();
            assertThat(triangle.getCumulativeValue(1991, 1991)).isZero();
            assertThat(triangle.getCumulativeValue(1992, 1992)).isZero();

            // Assert - Origin year 1993 should have correct cumulative values
            assertThat(triangle.getCumulativeValue(1993, 1993)).isEqualTo(100.0);
            assertThat(triangle.getCumulativeValue(1993, 1994)).isEqualTo(250.0);
            assertThat(triangle.getCumulativeValue(1993, 1995)).isEqualTo(450.0);
        }
    }

    // ========================================
    // Validation Tests
    // ========================================

    @Nested
    @DisplayName("calculateCumulative() - Validation")
    class ValidationTests {

        @Test
        @DisplayName("Should throw NullPointerException when triangle is null")
        void calculateCumulative_nullTriangle_throwsNullPointerException() {
            // Act & Assert
            assertThatThrownBy(() -> TriangleAccumulator.calculateCumulative(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Triangle must not be null");
        }
    }

    // ========================================
    // Real-World Scenarios Tests
    // ========================================

    @Nested
    @DisplayName("calculateCumulative() - Real-World Scenarios")
    class RealWorldScenariosTests {

        @Test
        @DisplayName("Should handle complete Non-Comp example data from CLAUDE.md")
        void calculateCumulative_completeNonCompExample_matchesExpectedOutput() {
            // Arrange - Complete Non-Comp dataset from CLAUDE.md
            ClaimsTriangle triangle = createNonCompTriangleFromCLAUDEmd();

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Verify all expected cumulative values
            double[][] expectedCumulative = {
                {45.2, 110.0, 110.0, 147.0},  // Origin 1990
                {50.0, 125.0, 150.0},         // Origin 1991
                {55.0, 140.0},                // Origin 1992
                {100.0}                       // Origin 1993
            };

            verifyCumulativeValues(triangle, 1990, expectedCumulative);
        }

        @Test
        @DisplayName("Should handle complete Comp example data from CLAUDE.md")
        void calculateCumulative_completeCompExample_matchesExpectedOutput() {
            // Arrange - Complete Comp dataset from CLAUDE.md
            ClaimsTriangle triangle = createCompTriangleFromCLAUDEmd();

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Verify all expected cumulative values
            double[][] expectedCumulative = {
                {0.0, 0.0, 0.0, 0.0},  // Origin 1990 (no data)
                {0.0, 0.0, 0.0},       // Origin 1991 (no data)
                {110.0, 280.0},        // Origin 1992
                {200.0}                // Origin 1993
            };

            verifyCumulativeValues(triangle, 1990, expectedCumulative);
        }

        @Test
        @DisplayName("Should handle multiple products with different patterns in integration scenario")
        void calculateCumulative_multipleProducts_calculatesIndependently() {
            // Arrange - Create two separate triangles for different products
            ClaimsTriangle nonCompTriangle = createNonCompTriangleFromCLAUDEmd();
            ClaimsTriangle compTriangle = createCompTriangleFromCLAUDEmd();

            // Act
            TriangleAccumulator.calculateCumulative(nonCompTriangle);
            TriangleAccumulator.calculateCumulative(compTriangle);

            // Assert - Non-Comp should have correct values
            assertThat(nonCompTriangle.getCumulativeValue(1990, 1990)).isEqualTo(45.2);
            assertThat(nonCompTriangle.getCumulativeValue(1993, 1993)).isEqualTo(100.0);

            // Assert - Comp should have correct values (different from Non-Comp)
            assertThat(compTriangle.getCumulativeValue(1992, 1992)).isEqualTo(110.0);
            assertThat(compTriangle.getCumulativeValue(1993, 1993)).isEqualTo(200.0);
        }

        @Test
        @DisplayName("Should handle complex scenario with missing years and negative values")
        void calculateCumulative_complexScenarioMissingYearsAndNegatives_calculatesCorrectly() {
            // Arrange - Realistic scenario with gaps and recoveries
            ClaimsTriangle triangle = new ClaimsTriangle("ComplexProduct", 2015, 2020);

            // Origin 2015: Has data for most years with one recovery
            triangle.addIncrementalValue(2015, 2015, 5000.0);
            triangle.addIncrementalValue(2015, 2016, 2000.0);
            triangle.addIncrementalValue(2015, 2017, -500.0);  // Recovery
            triangle.addIncrementalValue(2015, 2018, 1000.0);
            // Gap at 2019
            triangle.addIncrementalValue(2015, 2020, 300.0);

            // Origin 2017: Only has data starting from 2017 (missing earlier origin years)
            triangle.addIncrementalValue(2017, 2017, 3000.0);
            triangle.addIncrementalValue(2017, 2019, 1500.0);  // Gap at 2018

            // Origin 2020: Single data point
            triangle.addIncrementalValue(2020, 2020, 800.0);

            // Act
            TriangleAccumulator.calculateCumulative(triangle);

            // Assert - Origin 2015
            assertThat(triangle.getCumulativeValue(2015, 2015)).isEqualTo(5000.0);
            assertThat(triangle.getCumulativeValue(2015, 2016)).isEqualTo(7000.0);
            assertThat(triangle.getCumulativeValue(2015, 2017)).isEqualTo(6500.0);   // Recovery
            assertThat(triangle.getCumulativeValue(2015, 2018)).isEqualTo(7500.0);
            assertThat(triangle.getCumulativeValue(2015, 2019)).isEqualTo(7500.0);   // Gap
            assertThat(triangle.getCumulativeValue(2015, 2020)).isEqualTo(7800.0);

            // Assert - Origin 2016 (no data)
            assertThat(triangle.getCumulativeValue(2016, 2016)).isZero();

            // Assert - Origin 2017
            assertThat(triangle.getCumulativeValue(2017, 2017)).isEqualTo(3000.0);
            assertThat(triangle.getCumulativeValue(2017, 2018)).isEqualTo(3000.0);   // Gap
            assertThat(triangle.getCumulativeValue(2017, 2019)).isEqualTo(4500.0);

            // Assert - Origin 2020
            assertThat(triangle.getCumulativeValue(2020, 2020)).isEqualTo(800.0);
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Creates a ClaimsTriangle with Non-Comp data from CLAUDE.md example.
     *
     * @return triangle populated with Non-Comp incremental values
     */
    private ClaimsTriangle createNonCompTriangleFromCLAUDEmd() {
        ClaimsTriangle triangle = new ClaimsTriangle("Non-Comp", 1990, 1993);

        // Origin 1990
        triangle.addIncrementalValue(1990, 1990, 45.2);
        triangle.addIncrementalValue(1990, 1991, 64.8);
        triangle.addIncrementalValue(1990, 1993, 37.0);

        // Origin 1991
        triangle.addIncrementalValue(1991, 1991, 50.0);
        triangle.addIncrementalValue(1991, 1992, 75.0);
        triangle.addIncrementalValue(1991, 1993, 25.0);

        // Origin 1992
        triangle.addIncrementalValue(1992, 1992, 55.0);
        triangle.addIncrementalValue(1992, 1993, 85.0);

        // Origin 1993
        triangle.addIncrementalValue(1993, 1993, 100.0);

        return triangle;
    }

    /**
     * Creates a ClaimsTriangle with Comp data from CLAUDE.md example.
     *
     * @return triangle populated with Comp incremental values
     */
    private ClaimsTriangle createCompTriangleFromCLAUDEmd() {
        ClaimsTriangle triangle = new ClaimsTriangle("Comp", 1990, 1993);

        // Origin 1992
        triangle.addIncrementalValue(1992, 1992, 110.0);
        triangle.addIncrementalValue(1992, 1993, 170.0);

        // Origin 1993
        triangle.addIncrementalValue(1993, 1993, 200.0);

        return triangle;
    }

    /**
     * Verifies cumulative values in a triangle match expected values.
     *
     * @param triangle the triangle to verify
     * @param startOriginYear the starting origin year
     * @param expectedCumulative 2D array of expected cumulative values
     */
    private void verifyCumulativeValues(ClaimsTriangle triangle, int startOriginYear,
                                       double[][] expectedCumulative) {
        for (int i = 0; i < expectedCumulative.length; i++) {
            int originYear = startOriginYear + i;
            for (int j = 0; j < expectedCumulative[i].length; j++) {
                int devYear = originYear + j;
                double expected = expectedCumulative[i][j];
                double actual = triangle.getCumulativeValue(originYear, devYear);

                assertThat(actual)
                    .as("Cumulative value for origin=%d, dev=%d", originYear, devYear)
                    .isCloseTo(expected, within(0.001));
            }
        }
    }
}
