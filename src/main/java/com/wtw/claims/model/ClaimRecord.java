package com.wtw.claims.model;

/**
 * Immutable record representing a single row of incremental claims data.
 *
 * <p>Each claim record contains information about an insurance claim payment:
 * <ul>
 *   <li>Product: The insurance product type (e.g., "Comp", "Non-Comp")</li>
 *   <li>Origin Year: The year when the claim originally occurred</li>
 *   <li>Development Year: The year when the payment was made</li>
 *   <li>Incremental Value: The amount paid in this development year</li>
 * </ul>
 * </p>
 *
 * <p>This record is immutable by design, ensuring thread-safety and preventing
 * accidental modifications during processing.</p>
 *
 * @param product the insurance product name (trimmed, never null or empty)
 * @param originYear the year when the claim occurred
 * @param developmentYear the year when the payment was made
 * @param incrementalValue the amount paid in this development year
 *
 * @author Claims Triangle Accumulator
 * @version 1.0.0
 */
public record ClaimRecord(
        String product,
        int originYear,
        int developmentYear,
        double incrementalValue
) {
    /**
     * Compact constructor that validates and normalizes input.
     *
     * @throws IllegalArgumentException if product is null or empty
     */
    public ClaimRecord {
        if (product == null || product.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name must not be null or empty");
        }
        product = product.trim();
    }
}
