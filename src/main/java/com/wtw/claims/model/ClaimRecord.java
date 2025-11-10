package com.wtw.claims.model;

import java.util.Objects;

/**
 * Immutable data class representing a single row of incremental claims data.
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
 * <p>This class is immutable to ensure thread-safety and prevent accidental modifications
 * during processing.</p>
 *
 * @author Claims Triangle Accumulator
 * @version 1.0.0
 */
public final class ClaimRecord {

    private final String product;
    private final int originYear;
    private final int developmentYear;
    private final double incrementalValue;

    /**
     * Constructs a new ClaimRecord with the specified values.
     *
     * @param product the insurance product name (must not be null or empty)
     * @param originYear the year when the claim occurred
     * @param developmentYear the year when the payment was made
     * @param incrementalValue the amount paid in this development year
     * @throws IllegalArgumentException if product is null or empty
     */
    public ClaimRecord(String product, int originYear, int developmentYear, double incrementalValue) {
        if (product == null || product.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name must not be null or empty");
        }
        this.product = product.trim();
        this.originYear = originYear;
        this.developmentYear = developmentYear;
        this.incrementalValue = incrementalValue;
    }

    /**
     * Returns the insurance product name.
     *
     * @return the product name (never null or empty)
     */
    public String getProduct() {
        return product;
    }

    /**
     * Returns the origin year (when the claim occurred).
     *
     * @return the origin year
     */
    public int getOriginYear() {
        return originYear;
    }

    /**
     * Returns the development year (when the payment was made).
     *
     * @return the development year
     */
    public int getDevelopmentYear() {
        return developmentYear;
    }

    /**
     * Returns the incremental value (amount paid).
     *
     * @return the incremental value
     */
    public double getIncrementalValue() {
        return incrementalValue;
    }

    /**
     * Compares this ClaimRecord to another object for equality.
     * Two ClaimRecords are equal if all their fields are equal.
     *
     * @param obj the object to compare with
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ClaimRecord that = (ClaimRecord) obj;
        return originYear == that.originYear
            && developmentYear == that.developmentYear
            && Double.compare(that.incrementalValue, incrementalValue) == 0
            && Objects.equals(product, that.product);
    }

    /**
     * Returns a hash code for this ClaimRecord.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return Objects.hash(product, originYear, developmentYear, incrementalValue);
    }

    /**
     * Returns a string representation of this ClaimRecord.
     *
     * @return a string in the format "ClaimRecord{product='...', originYear=..., developmentYear=..., incrementalValue=...}"
     */
    @Override
    public String toString() {
        return "ClaimRecord{" +
                "product='" + product + '\'' +
                ", originYear=" + originYear +
                ", developmentYear=" + developmentYear +
                ", incrementalValue=" + incrementalValue +
                '}';
    }
}
