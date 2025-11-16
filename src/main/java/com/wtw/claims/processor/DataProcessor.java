package com.wtw.claims.processor;

import com.wtw.claims.model.ClaimRecord;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for processing and organizing claim records for triangle construction.
 *
 * <p>This class provides static utility methods for grouping claim records by product
 * and determining the global year ranges needed for claims triangle analysis.</p>
 *
 * <p>The year range methods (earliest origin year and latest development year)
 * calculate <strong>global</strong> values across ALL claim records, regardless
 * of product. This ensures all triangles have consistent dimensions for output
 * formatting.</p>
 *
 * <p><strong>Note:</strong> This is a utility class with static methods only.
 * It cannot be instantiated.</p>
 *
 * @author Claims Processing System
 * @version 1.0.0
 */
public final class DataProcessor {

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if an attempt is made to instantiate this class
     */
    private DataProcessor() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Groups claim records by product name.
     *
     * <p>This method organizes a list of claim records into a map where each
     * key is a product name and the value is a list of all claim records for
     * that product.</p>
     *
     * <p>Example:</p>
     * <pre>
     * Input: [ClaimRecord("Comp", 1992, 1992, 110.0),
     *         ClaimRecord("Non-Comp", 1990, 1990, 45.2),
     *         ClaimRecord("Comp", 1992, 1993, 170.0)]
     *
     * Output: {
     *   "Comp" -> [ClaimRecord("Comp", 1992, 1992, 110.0),
     *              ClaimRecord("Comp", 1992, 1993, 170.0)],
     *   "Non-Comp" -> [ClaimRecord("Non-Comp", 1990, 1990, 45.2)]
     * }
     * </pre>
     *
     * @param records the list of claim records to group (must not be null)
     * @return a map of product names to their corresponding claim records
     * @throws NullPointerException if records is null
     */
    public static Map<String, List<ClaimRecord>> groupByProduct(List<ClaimRecord> records) {
        if (records == null) {
            throw new NullPointerException("Records list must not be null");
        }

        return records.stream()
                .collect(Collectors.groupingBy(ClaimRecord::product));
    }

    /**
     * Finds the earliest origin year across all claim records.
     *
     * <p>This method determines the <strong>global minimum</strong> origin year
     * present in the dataset, which is used as the starting point for all claims
     * triangles regardless of product.</p>
     *
     * <p>Example:</p>
     * <pre>
     * Records with origin years: 1990, 1991, 1992, 1993
     * Returns: 1990
     * </pre>
     *
     * @param records the list of claim records to analyze (must not be null or empty)
     * @return the earliest (minimum) origin year found in the records
     * @throws NullPointerException if records is null
     * @throws IllegalArgumentException if records is empty
     */
    public static int findEarliestOriginYear(List<ClaimRecord> records) {
        if (records == null) {
            throw new NullPointerException("Records list must not be null");
        }

        if (records.isEmpty()) {
            throw new IllegalArgumentException("Records list must not be empty");
        }

        return records.stream()
                .mapToInt(ClaimRecord::originYear)
                .min()
                .orElseThrow(() -> new IllegalStateException("Unable to find minimum origin year"));
    }

    /**
     * Finds the latest development year across all claim records.
     *
     * <p>This method determines the <strong>global maximum</strong> development year
     * present in the dataset, which defines the endpoint for all claims triangles
     * regardless of product.</p>
     *
     * <p>Example:</p>
     * <pre>
     * Records with development years: 1990, 1991, 1992, 1993
     * Returns: 1993
     * </pre>
     *
     * @param records the list of claim records to analyze (must not be null or empty)
     * @return the latest (maximum) development year found in the records
     * @throws NullPointerException if records is null
     * @throws IllegalArgumentException if records is empty
     */
    public static int findLatestDevelopmentYear(List<ClaimRecord> records) {
        if (records == null) {
            throw new NullPointerException("Records list must not be null");
        }

        if (records.isEmpty()) {
            throw new IllegalArgumentException("Records list must not be empty");
        }

        return records.stream()
                .mapToInt(ClaimRecord::developmentYear)
                .max()
                .orElseThrow(() -> new IllegalStateException("Unable to find maximum development year"));
    }

    /**
     * Calculates the number of development years in the dataset.
     *
     * <p>This is a convenience method that computes the difference between
     * the latest development year and earliest origin year, plus one (to
     * make it inclusive).</p>
     *
     * <p>Example:</p>
     * <pre>
     * Earliest origin year: 1990
     * Latest development year: 1993
     * Number of development years: 1993 - 1990 + 1 = 4
     * </pre>
     *
     * @param records the list of claim records to analyze (must not be null or empty)
     * @return the number of development years in the dataset
     * @throws NullPointerException if records is null
     * @throws IllegalArgumentException if records is empty
     */
    public static int getNumberOfDevelopmentYears(List<ClaimRecord> records) {
        int earliestOrigin = findEarliestOriginYear(records);
        int latestDevelopment = findLatestDevelopmentYear(records);
        return latestDevelopment - earliestOrigin + 1;
    }
}
