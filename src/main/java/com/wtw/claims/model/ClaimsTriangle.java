package com.wtw.claims.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a claims triangle for a single insurance product.
 *
 * <p>A claims triangle stores both incremental and cumulative claim values
 * organized by origin year and development year. This data structure is used
 * in actuarial analysis to track claim development patterns over time.</p>
 *
 * <p>The triangle structure means that for each origin year, there are
 * progressively fewer development years (forming a triangular shape when
 * visualized). For example:</p>
 *
 * <pre>
 * Origin 1990: [dev 1990, dev 1991, dev 1992, dev 1993]  (4 values)
 * Origin 1991: [dev 1991, dev 1992, dev 1993]            (3 values)
 * Origin 1992: [dev 1992, dev 1993]                      (2 values)
 * Origin 1993: [dev 1993]                                (1 value)
 * </pre>
 *
 * @author Claims Processing System
 * @version 1.0.0
 */
public final class ClaimsTriangle {

    /**
     * The name of the insurance product (e.g., "Comp", "Non-Comp").
     */
    private final String productName;

    /**
     * The earliest origin year in the dataset (global minimum across all products).
     */
    private final int earliestOriginYear;

    /**
     * The latest development year in the dataset (global maximum across all products).
     */
    private final int latestDevelopmentYear;

    /**
     * Stores incremental claim values.
     * Structure: originYear -> (developmentYear -> incrementalValue)
     */
    private final Map<Integer, Map<Integer, Double>> incrementalData;

    /**
     * Stores cumulative claim values.
     * Structure: originYear -> (developmentYear -> cumulativeValue)
     */
    private final Map<Integer, Map<Integer, Double>> cumulativeData;

    /**
     * Constructs a new ClaimsTriangle for a specific product.
     *
     * @param productName the name of the insurance product (must not be null or empty)
     * @param earliestOriginYear the earliest origin year in the entire dataset
     * @param latestDevelopmentYear the latest development year in the entire dataset
     * @throws NullPointerException if productName is null
     * @throws IllegalArgumentException if productName is empty after trimming
     * @throws IllegalArgumentException if latestDevelopmentYear is less than earliestOriginYear
     */
    public ClaimsTriangle(String productName, int earliestOriginYear, int latestDevelopmentYear) {
        Objects.requireNonNull(productName, "Product name must not be null");

        String trimmedProductName = productName.trim();
        if (trimmedProductName.isEmpty()) {
            throw new IllegalArgumentException("Product name must not be empty");
        }

        if (latestDevelopmentYear < earliestOriginYear) {
            throw new IllegalArgumentException(
                "Latest development year (" + latestDevelopmentYear +
                ") must not be less than earliest origin year (" + earliestOriginYear + ")"
            );
        }

        this.productName = trimmedProductName;
        this.earliestOriginYear = earliestOriginYear;
        this.latestDevelopmentYear = latestDevelopmentYear;
        this.incrementalData = new HashMap<>();
        this.cumulativeData = new HashMap<>();
    }

    /**
     * Adds an incremental claim value for a specific origin year and development year.
     *
     * @param originYear the year when the claim originated
     * @param developmentYear the year when the payment was made
     * @param value the incremental claim amount (must be finite)
     * @throws IllegalArgumentException if value is not finite (NaN or infinite)
     */
    public void addIncrementalValue(int originYear, int developmentYear, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                "Incremental value must be finite (not NaN or infinite): " + value
            );
        }

        incrementalData
            .computeIfAbsent(originYear, k -> new HashMap<>())
            .put(developmentYear, value);
    }

    /**
     * Retrieves the incremental claim value for a specific origin year and development year.
     *
     * <p>If no value has been stored for the given year combination, returns 0.0 as per
     * the business requirement that missing incremental values should be treated as zero.</p>
     *
     * @param originYear the year when the claim originated
     * @param developmentYear the year when the payment was made
     * @return the incremental claim amount, or 0.0 if no value exists
     */
    public double getIncrementalValue(int originYear, int developmentYear) {
        return incrementalData
            .getOrDefault(originYear, new HashMap<>())
            .getOrDefault(developmentYear, 0.0);
    }

    /**
     * Sets the cumulative claim value for a specific origin year and development year.
     *
     * <p>Cumulative values represent the sum of all incremental values from the origin
     * year up to and including the development year.</p>
     *
     * @param originYear the year when the claim originated
     * @param developmentYear the year when the payment was made
     * @param value the cumulative claim amount (must be finite)
     * @throws IllegalArgumentException if value is not finite (NaN or infinite)
     */
    public void setCumulativeValue(int originYear, int developmentYear, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                "Cumulative value must be finite (not NaN or infinite): " + value
            );
        }

        cumulativeData
            .computeIfAbsent(originYear, k -> new HashMap<>())
            .put(developmentYear, value);
    }

    /**
     * Retrieves the cumulative claim value for a specific origin year and development year.
     *
     * <p>Returns 0.0 if no cumulative value has been calculated for the given year
     * combination. This should only occur if {@link #setCumulativeValue} has not been
     * called for this year pair.</p>
     *
     * @param originYear the year when the claim originated
     * @param developmentYear the year when the payment was made
     * @return the cumulative claim amount, or 0.0 if no value exists
     */
    public double getCumulativeValue(int originYear, int developmentYear) {
        return cumulativeData
            .getOrDefault(originYear, new HashMap<>())
            .getOrDefault(developmentYear, 0.0);
    }

    /**
     * Returns the product name for this triangle.
     *
     * @return the insurance product name
     */
    public String getProductName() {
        return productName;
    }

    /**
     * Returns the earliest origin year in the dataset.
     *
     * @return the earliest origin year (global minimum)
     */
    public int getEarliestOriginYear() {
        return earliestOriginYear;
    }

    /**
     * Returns the latest development year in the dataset.
     *
     * @return the latest development year (global maximum)
     */
    public int getLatestDevelopmentYear() {
        return latestDevelopmentYear;
    }

    /**
     * Calculates the number of development years in the triangle.
     *
     * <p>This represents the number of years from the earliest origin year
     * to the latest development year, inclusive.</p>
     *
     * @return the number of development years
     */
    public int getNumberOfDevelopmentYears() {
        return latestDevelopmentYear - earliestOriginYear + 1;
    }

    @Override
    public String toString() {
        return "ClaimsTriangle{" +
                "productName='" + productName + '\'' +
                ", earliestOriginYear=" + earliestOriginYear +
                ", latestDevelopmentYear=" + latestDevelopmentYear +
                ", developmentYears=" + getNumberOfDevelopmentYears() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClaimsTriangle that = (ClaimsTriangle) o;
        return earliestOriginYear == that.earliestOriginYear &&
                latestDevelopmentYear == that.latestDevelopmentYear &&
                Objects.equals(productName, that.productName) &&
                Objects.equals(incrementalData, that.incrementalData) &&
                Objects.equals(cumulativeData, that.cumulativeData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productName, earliestOriginYear, latestDevelopmentYear,
                            incrementalData, cumulativeData);
    }
}
