package com.wtw.claims.validator;

import com.wtw.claims.model.ClaimRecord;

/**
 * Validator for ClaimRecord objects that enforces business rules.
 * <p>
 * This validator ensures that claim records satisfy business constraints beyond
 * basic field validation:
 * </p>
 * <ul>
 *   <li>Years must be within a reasonable range (1900-2100)</li>
 *   <li>Development year must not be before origin year</li>
 *   <li>Incremental values must be finite numbers (not NaN or infinite)</li>
 * </ul>
 *
 * @author Claims Triangle Accumulator
 * @version 1.0.0
 */
public class ClaimRecordValidator {

    /** Minimum valid year for claims data */
    private static final int MIN_YEAR = 1900;

    /** Maximum valid year for claims data */
    private static final int MAX_YEAR = 2100;

    /**
     * Validates a ClaimRecord against business rules.
     * <p>
     * This method performs comprehensive validation including:
     * </p>
     * <ul>
     *   <li>Origin year is within valid range</li>
     *   <li>Development year is within valid range</li>
     *   <li>Development year is not before origin year</li>
     *   <li>Incremental value is a finite number</li>
     * </ul>
     *
     * @param record the claim record to validate
     * @throws IllegalArgumentException if the record violates any business rule
     * @throws NullPointerException if record is null
     */
    public void validateRecord(ClaimRecord record) {
        if (record == null) {
            throw new NullPointerException("ClaimRecord cannot be null");
        }

        int originYear = record.getOriginYear();
        int developmentYear = record.getDevelopmentYear();
        double incrementalValue = record.getIncrementalValue();

        // Validate origin year
        if (!isValidYear(originYear)) {
            throw new IllegalArgumentException(
                String.format("Origin year (%d) is outside valid range [%d-%d]",
                    originYear, MIN_YEAR, MAX_YEAR)
            );
        }

        // Validate development year
        if (!isValidYear(developmentYear)) {
            throw new IllegalArgumentException(
                String.format("Development year (%d) is outside valid range [%d-%d]",
                    developmentYear, MIN_YEAR, MAX_YEAR)
            );
        }

        // Validate development year is not before origin year
        if (!isValidDevelopmentYear(originYear, developmentYear)) {
            throw new IllegalArgumentException(
                String.format("Development year (%d) cannot be before origin year (%d)",
                    developmentYear, originYear)
            );
        }

        // Validate incremental value is finite
        if (!isValidValue(incrementalValue)) {
            throw new IllegalArgumentException(
                String.format("Incremental value (%s) must be a finite number",
                    incrementalValue)
            );
        }
    }

    /**
     * Checks if a year is within the valid range.
     *
     * @param year the year to validate
     * @return true if the year is between MIN_YEAR and MAX_YEAR (inclusive), false otherwise
     */
    private boolean isValidYear(int year) {
        return year >= MIN_YEAR && year <= MAX_YEAR;
    }

    /**
     * Checks if the development year is valid relative to the origin year.
     * <p>
     * A development year is valid if it is greater than or equal to the origin year,
     * as claims cannot be developed before they occur.
     * </p>
     *
     * @param originYear      the year when the claim occurred
     * @param developmentYear the year when the payment was made
     * @return true if developmentYear >= originYear, false otherwise
     */
    private boolean isValidDevelopmentYear(int originYear, int developmentYear) {
        return developmentYear >= originYear;
    }

    /**
     * Checks if an incremental value is valid.
     * <p>
     * A value is valid if it is finite (not NaN, not positive infinity, not negative infinity).
     * Negative values are allowed to support salvage recoveries and claim reversals.
     * </p>
     *
     * @param value the incremental value to validate
     * @return true if the value is finite, false otherwise
     */
    private boolean isValidValue(double value) {
        return Double.isFinite(value);
    }
}
