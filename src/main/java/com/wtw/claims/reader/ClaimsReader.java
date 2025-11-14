package com.wtw.claims.reader;

import com.wtw.claims.model.ClaimRecord;
import com.wtw.claims.validator.ClaimRecordValidator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads and parses claims data from CSV files into ClaimRecord objects.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Reading CSV files from the file system</li>
 *   <li>Validating CSV header format</li>
 *   <li>Parsing each row into a ClaimRecord object</li>
 *   <li>Validating each record using ClaimRecordValidator</li>
 *   <li>Collecting and returning all valid records</li>
 * </ul>
 *
 * <p>Expected CSV format:
 * <pre>
 * Product, Origin Year, Development Year, Incremental Value
 * Comp, 1992, 1992, 110.0
 * Non-Comp, 1990, 1991, 64.8
 * Non-Comp, 1990, 1992,       &lt;-- empty value treated as 0.0
 * </pre>
 *
 * <p>This class handles various error scenarios including:
 * <ul>
 *   <li>File not found</li>
 *   <li>Invalid CSV header format</li>
 *   <li>Malformed CSV rows (missing columns, wrong data types)</li>
 *   <li>Invalid data that fails validation rules</li>
 *   <li>Empty files</li>
 * </ul>
 *
 * <p><strong>Special handling:</strong> Empty or missing Incremental Value fields
 * are treated as 0.0, as zero incremental values may be omitted from input data.
 *
 * @author WTW Claims Processing Team
 * @version 1.0
 */
public class ClaimsReader {

    private static final Logger logger = LoggerFactory.getLogger(ClaimsReader.class);

    /**
     * Expected CSV column headers in order.
     */
    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
        "Product",
        "Origin Year",
        "Development Year",
        "Incremental Value"
    );

    private final ClaimRecordValidator validator;

    /**
     * Constructs a new ClaimsReader with default validator.
     */
    public ClaimsReader() {
        this.validator = new ClaimRecordValidator();
    }

    /**
     * Constructs a new ClaimsReader with a custom validator.
     *
     * @param validator the validator to use for validating claim records
     * @throws NullPointerException if validator is null
     */
    public ClaimsReader(ClaimRecordValidator validator) {
        if (validator == null) {
            throw new NullPointerException("Validator cannot be null");
        }
        this.validator = validator;
    }

    /**
     * Simple data class representing the year range found in a claims dataset.
     * Used by the streaming approach to avoid loading all records into memory.
     *
     * <p>This is a minimal data holder - no equals/hashCode needed as it's only
     * used to pass data between methods, not for comparisons or collections.</p>
     */
    public static class YearRange {
        /** The minimum origin year found in the dataset */
        public final int minOriginYear;

        /** The maximum development year found in the dataset */
        public final int maxDevYear;

        /**
         * Creates a new YearRange with the specified boundaries.
         *
         * @param minOriginYear the minimum origin year
         * @param maxDevYear the maximum development year
         * @throws IllegalArgumentException if maxDevYear is less than minOriginYear or if the range would cause integer overflow in calculations
         */
        public YearRange(int minOriginYear, int maxDevYear) {
            if (maxDevYear < minOriginYear) {
                throw new IllegalArgumentException(
                    String.format("Max development year (%d) cannot be less than min origin year (%d)",
                        maxDevYear, minOriginYear)
                );
            }

            // Check for potential overflow in getNumberOfDevelopmentYears()
            // Using long arithmetic to detect if the result would overflow an int
            long yearSpan = (long) maxDevYear - (long) minOriginYear + 1;
            if (yearSpan > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                    String.format("Year range too large: would overflow integer calculation (range from %d to %d spans %d years)",
                        minOriginYear, maxDevYear, yearSpan)
                );
            }

            this.minOriginYear = minOriginYear;
            this.maxDevYear = maxDevYear;
        }

        /**
         * Calculates the number of development years in this range.
         *
         * @return the number of years (inclusive)
         */
        public int getNumberOfDevelopmentYears() {
            return maxDevYear - minOriginYear + 1;
        }

        @Override
        public String toString() {
            return String.format("YearRange{%d-%d (%d years)}",
                minOriginYear, maxDevYear, getNumberOfDevelopmentYears());
        }
    }

    /**
     * Performs a lightweight scan of the CSV file to determine the year range.
     *
     * <p>This method performs a first-pass scan that extracts only the minimum origin year
     * and maximum development year from the dataset. This is used by the streaming approach
     * to determine triangle dimensions before the actual data processing pass.</p>
     *
     * <p><strong>Memory efficiency:</strong> This method uses O(1) memory regardless of file size,
     * as it only tracks two integer values (min and max) while scanning through the file.</p>
     *
     * @param filePath the path to the CSV file to scan
     * @return YearRange containing the minimum origin year and maximum development year found in the file
     * @throws IOException if an I/O error occurs reading the file
     * @throws IllegalArgumentException if the CSV format is invalid, data is malformed, or no valid records are found
     * @throws NullPointerException if filePath is null
     */
    public YearRange scanForYearRange(Path filePath) throws IOException {
        if (filePath == null) {
            throw new NullPointerException("File path cannot be null");
        }

        logger.info("Scanning for year range in file: {}", filePath);

        Integer minOriginYear = null;
        Integer maxDevYear = null;

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            CSVParser csvParser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build()
                .parse(reader);

            // Validate header
            validateHeader(csvParser.getHeaderNames());

            // Scan records to find min/max years
            int lineNumber = 1; // Header is line 0, data starts at line 1
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    // Validate record has minimum required columns (need at least columns 0-2 for Product, Origin, Dev)
                    if (csvRecord.size() < 3) {
                        throw new IllegalArgumentException(
                            String.format("Expected at least 3 columns but found %d", csvRecord.size())
                        );
                    }

                    // Extract only the year fields - we don't need product or incremental value
                    int originYear = parseInt(csvRecord.get(1), "Origin Year");
                    int developmentYear = parseInt(csvRecord.get(2), "Development Year");

                    // Validate years are in logical order (development >= origin)
                    if (developmentYear < originYear) {
                        throw new IllegalArgumentException(
                            String.format("Development year (%d) cannot be before origin year (%d)",
                                developmentYear, originYear)
                        );
                    }

                    // Update min/max
                    if (minOriginYear == null || originYear < minOriginYear) {
                        minOriginYear = originYear;
                    }
                    if (maxDevYear == null || developmentYear > maxDevYear) {
                        maxDevYear = developmentYear;
                    }
                } catch (IllegalArgumentException e) {
                    String errorMsg = String.format(
                        "Error scanning line %d: %s",
                        lineNumber,
                        e.getMessage()
                    );
                    logger.error(errorMsg, e);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }

            // Validate we found at least one record
            if (minOriginYear == null || maxDevYear == null) {
                throw new IllegalArgumentException("No valid claim records found in input file");
            }

            YearRange yearRange = new YearRange(minOriginYear, maxDevYear);
            logger.info("Scanned year range: {}", yearRange);
            return yearRange;
        }
    }

    /**
     * Reads and parses a CSV file into a list of ClaimRecord objects.
     *
     * @param filePath the path to the CSV file to read
     * @return list of parsed and validated ClaimRecord objects
     * @throws IOException if an I/O error occurs reading the file
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     * @throws NullPointerException if filePath is null
     */
    public List<ClaimRecord> readClaims(Path filePath) throws IOException {
        if (filePath == null) {
            throw new NullPointerException("File path cannot be null");
        }

        logger.info("Reading claims from file: {}", filePath);

        List<ClaimRecord> records = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            CSVParser csvParser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build()
                .parse(reader);

            // Validate header
            validateHeader(csvParser.getHeaderNames());

            // Parse records
            int lineNumber = 1; // Header is line 0, data starts at line 1
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    ClaimRecord claimRecord = parseRecord(csvRecord, lineNumber);
                    records.add(claimRecord);
                } catch (IllegalArgumentException e) {
                    // Catches IllegalArgumentException and its subclass NumberFormatException
                    String errorMsg = String.format(
                        "Error parsing line %d: %s",
                        lineNumber,
                        e.getMessage()
                    );
                    logger.error(errorMsg, e);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }

            logger.info("Successfully read {} claim records from {}", records.size(), filePath);
        }

        return records;
    }

    /**
     * Validates that the CSV header matches the expected format.
     *
     * @param actualHeaders the actual header names from the CSV file
     * @throws IllegalArgumentException if headers don't match expected format
     */
    private void validateHeader(List<String> actualHeaders) {
        if (actualHeaders == null || actualHeaders.isEmpty()) {
            throw new IllegalArgumentException("CSV file has no header row");
        }

        if (actualHeaders.size() != EXPECTED_HEADERS.size()) {
            throw new IllegalArgumentException(
                String.format(
                    "Invalid CSV header: expected %d columns but found %d. Expected headers: %s",
                    EXPECTED_HEADERS.size(),
                    actualHeaders.size(),
                    EXPECTED_HEADERS
                )
            );
        }

        // Normalize headers for comparison (trim and ignore case)
        List<String> normalizedActual = new ArrayList<>();
        for (String header : actualHeaders) {
            normalizedActual.add(header.trim());
        }

        List<String> normalizedExpected = new ArrayList<>();
        for (String header : EXPECTED_HEADERS) {
            normalizedExpected.add(header.trim());
        }

        for (int i = 0; i < normalizedExpected.size(); i++) {
            if (!normalizedExpected.get(i).equalsIgnoreCase(normalizedActual.get(i))) {
                throw new IllegalArgumentException(
                    String.format(
                        "Invalid CSV header at column %d: expected '%s' but found '%s'",
                        i + 1,
                        normalizedExpected.get(i),
                        normalizedActual.get(i)
                    )
                );
            }
        }
    }

    /**
     * Parses a single CSV record into a ClaimRecord object.
     *
     * @param csvRecord the CSV record to parse
     * @param lineNumber the line number in the file (for error reporting)
     * @return the parsed ClaimRecord
     * @throws IllegalArgumentException if the record is malformed or invalid
     */
    private ClaimRecord parseRecord(CSVRecord csvRecord, int lineNumber) {
        // Validate record has correct number of columns
        if (csvRecord.size() != EXPECTED_HEADERS.size()) {
            throw new IllegalArgumentException(
                String.format(
                    "Expected %d columns but found %d",
                    EXPECTED_HEADERS.size(),
                    csvRecord.size()
                )
            );
        }

        try {
            // Parse fields
            String product = csvRecord.get(0);
            int originYear = parseInt(csvRecord.get(1), "Origin Year");
            int developmentYear = parseInt(csvRecord.get(2), "Development Year");

            // Incremental value defaults to 0.0 if empty (as per business rules)
            double incrementalValue = parseIncrementalValue(csvRecord.get(3));

            // Create and validate record
            ClaimRecord record = new ClaimRecord(product, originYear, developmentYear, incrementalValue);
            validator.validateRecord(record);

            return record;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric value: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a string value to an integer.
     *
     * @param value the string value to parse
     * @param fieldName the name of the field (for error reporting)
     * @return the parsed integer value
     * @throws NumberFormatException if the value cannot be parsed as an integer
     */
    private int parseInt(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new NumberFormatException(fieldName + " cannot be empty");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                String.format("%s must be an integer, but found: '%s'", fieldName, value)
            );
        }
    }

    /**
     * Parses the Incremental Value field, treating empty values as 0.0.
     *
     * <p>This is a special case: according to business rules, zero incremental
     * values may be omitted from the input data, so empty values are treated as 0.0
     * rather than as errors.
     *
     * @param value the string value to parse
     * @return the parsed double value, or 0.0 if the value is empty
     * @throws NumberFormatException if the value is not empty but cannot be parsed as a double
     */
    private double parseIncrementalValue(String value) {
        // Empty or whitespace-only values default to 0.0
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                String.format("Incremental Value must be a number, but found: '%s'", value)
            );
        }
    }
}
