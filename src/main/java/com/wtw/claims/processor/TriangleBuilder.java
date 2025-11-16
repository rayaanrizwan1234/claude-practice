package com.wtw.claims.processor;

import com.wtw.claims.model.ClaimRecord;
import com.wtw.claims.model.ClaimsTriangle;

import java.util.List;

/**
 * Utility class for building claims triangles from claim records.
 *
 * <p>This class constructs {@link ClaimsTriangle} objects by populating them with
 * incremental claim values from a list of {@link ClaimRecord}s. The triangles are
 * built using global year ranges to ensure consistent dimensions across all products.</p>
 *
 * <p><strong>Note:</strong> This is a utility class with static methods only.
 * It cannot be instantiated.</p>
 *
 * @author Claims Processing System
 * @version 1.0.0
 */
public final class TriangleBuilder {

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws UnsupportedOperationException if an attempt is made to instantiate this class
     */
    private TriangleBuilder() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Builds a claims triangle for a specific product using global year ranges.
     *
     * <p>This method creates a {@link ClaimsTriangle} for the given product and populates
     * it with incremental values from the provided claim records. The triangle uses
     * <strong>global</strong> year ranges (not product-specific) to ensure all triangles
     * have consistent dimensions for output formatting.</p>
     *
     * <p>Missing data (i.e., no claim record for a specific origin/development year
     * combination) will default to 0.0 when retrieved from the triangle.</p>
     *
     * <p><strong>Algorithm:</strong></p>
     * <ol>
     *   <li>Create a ClaimsTriangle with the product name and global year range</li>
     *   <li>Iterate through all claim records for this product</li>
     *   <li>Add each record's incremental value to the triangle</li>
     *   <li>Return the populated triangle (missing cells automatically default to 0.0)</li>
     * </ol>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * // Global year range: 1990-1993
     * // Records for "Comp" product:
     * //   - (1992, 1992, 110.0)
     * //   - (1992, 1993, 170.0)
     * //   - (1993, 1993, 200.0)
     *
     * List&lt;ClaimRecord&gt; compRecords = Arrays.asList(
     *     new ClaimRecord("Comp", 1992, 1992, 110.0),
     *     new ClaimRecord("Comp", 1992, 1993, 170.0),
     *     new ClaimRecord("Comp", 1993, 1993, 200.0)
     * );
     *
     * ClaimsTriangle triangle = TriangleBuilder.buildTriangle(
     *     "Comp",
     *     compRecords,
     *     1990,  // global earliest origin year
     *     1993   // global latest development year
     * );
     *
     * // Triangle will have data for years 1990-1993 (global range)
     * // Years 1990-1991 will have 0.0 for all cells (no Comp data for those years)
     * // Years 1992-1993 will have the actual claim values
     * </pre>
     *
     * @param product the name of the insurance product (must not be null or empty)
     * @param records the list of claim records for this product (must not be null, can be empty)
     * @param globalMinYear the earliest origin year across ALL products (global minimum)
     * @param globalMaxYear the latest development year across ALL products (global maximum)
     * @return a ClaimsTriangle populated with incremental values from the records
     * @throws NullPointerException if product is null or records is null
     * @throws IllegalArgumentException if product is empty after trimming
     * @throws IllegalArgumentException if globalMaxYear is less than globalMinYear
     */
    public static ClaimsTriangle buildTriangle(String product,
                                               List<ClaimRecord> records,
                                               int globalMinYear,
                                               int globalMaxYear) {
        // Validate inputs (ClaimsTriangle constructor validates product and year range)
        if (records == null) {
            throw new NullPointerException("Records list must not be null");
        }

        // Create the triangle with global year range
        ClaimsTriangle triangle = new ClaimsTriangle(product, globalMinYear, globalMaxYear);

        // Populate the triangle with incremental values from records
        for (ClaimRecord record : records) {
            triangle.addIncrementalValue(
                record.originYear(),
                record.developmentYear(),
                record.incrementalValue()
            );
        }

        return triangle;
    }
}
