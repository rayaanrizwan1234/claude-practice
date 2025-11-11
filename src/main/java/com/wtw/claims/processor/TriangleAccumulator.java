package com.wtw.claims.processor;

import com.wtw.claims.model.ClaimsTriangle;

/**
 * Utility class for calculating cumulative claim values in claims triangles.
 *
 * <p>This class calculates cumulative values by summing incremental claim values
 * over time for each origin year. Cumulative values represent the total amount
 * paid from the origin year up to and including each development year.</p>
 *
 * <p>The cumulative calculation is performed in-place, modifying the provided
 * {@link ClaimsTriangle} object by setting cumulative values using
 * {@link ClaimsTriangle#setCumulativeValue}.</p>
 *
 * <p><strong>Note:</strong> This is a utility class with static methods only.
 * It cannot be instantiated.</p>
 *
 * @author Claims Processing System
 * @version 1.0.0
 */
public final class TriangleAccumulator {

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if an attempt is made to instantiate this class
     */
    private TriangleAccumulator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Calculates cumulative claim values for a claims triangle.
     *
     * <p>This method computes cumulative values by summing incremental claim amounts
     * over time for each origin year. For each origin year, it starts with a cumulative
     * sum of 0.0 and adds the incremental value for each development year from the
     * origin year to the latest development year.</p>
     *
     * <p><strong>Algorithm:</strong></p>
     * <pre>
     * For each origin year (from earliestOriginYear to latestDevelopmentYear):
     *   cumulative = 0.0
     *   For each development year (from origin year to latestDevelopmentYear):
     *     incremental = triangle.getIncrementalValue(originYear, developmentYear)
     *     cumulative += incremental
     *     triangle.setCumulativeValue(originYear, developmentYear, cumulative)
     * </pre>
     *
     * <p><strong>Important:</strong> Missing incremental values (those not explicitly set)
     * return 0.0 from {@link ClaimsTriangle#getIncrementalValue}, so the cumulative value
     * remains unchanged for missing data points.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * // Given a triangle with incremental values for Non-Comp, Origin Year 1990:
     * // Dev 1990: 45.2
     * // Dev 1991: 64.8
     * // Dev 1992: 0.0 (missing)
     * // Dev 1993: 37.0
     *
     * TriangleAccumulator.calculateCumulative(triangle);
     *
     * // After calculation, cumulative values for Origin Year 1990:
     * // Dev 1990: 45.2  (cumulative = 0 + 45.2)
     * // Dev 1991: 110.0 (cumulative = 45.2 + 64.8)
     * // Dev 1992: 110.0 (cumulative = 110.0 + 0.0, missing value)
     * // Dev 1993: 147.0 (cumulative = 110.0 + 37.0)
     * </pre>
     *
     * <p><strong>Why Development Years Start from Origin Year:</strong></p>
     * <p>Development years cannot occur before the origin year (you can't make a payment
     * before the claim originated). Therefore, for each origin year, we only iterate through
     * development years from that origin year onwards. Earlier development years would be
     * invalid and are not processed.</p>
     *
     * @param triangle the claims triangle to calculate cumulative values for (must not be null)
     * @throws NullPointerException if triangle is null
     */
    public static void calculateCumulative(ClaimsTriangle triangle) {
        if (triangle == null) {
            throw new NullPointerException("Triangle must not be null");
        }

        int earliestOriginYear = triangle.getEarliestOriginYear();
        int latestDevelopmentYear = triangle.getLatestDevelopmentYear();

        // Iterate through each origin year
        for (int originYear = earliestOriginYear; originYear <= latestDevelopmentYear; originYear++) {
            double cumulative = 0.0;

            // For each origin year, iterate through development years starting from the origin year
            // (development years before origin year are invalid - can't pay before claim occurs)
            for (int devYear = originYear; devYear <= latestDevelopmentYear; devYear++) {
                // Get incremental value (returns 0.0 if missing)
                double incremental = triangle.getIncrementalValue(originYear, devYear);

                // Add to cumulative total
                cumulative += incremental;

                // Store the cumulative value
                triangle.setCumulativeValue(originYear, devYear, cumulative);
            }
        }
    }
}
