package com.wtw.claims.reader;

import com.wtw.claims.model.ClaimRecord;
import com.wtw.claims.validator.ClaimRecordValidator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
     * Immutable record representing the year range found in a claims dataset.
     * Used by the streaming approach to avoid loading all records into memory.
     *
     * @param minOriginYear the minimum origin year found in the dataset
     * @param maxDevYear the maximum development year found in the dataset
     */
    public record YearRange(int minOriginYear, int maxDevYear) {
        /**
         * Compact constructor that validates year range boundaries.
         *
         * @throws IllegalArgumentException if maxDevYear is less than minOriginYear or if the range would cause integer overflow in calculations
         */
        public YearRange {
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

        try (BufferedReader reader = Files.newBufferedReader(filePath);
             CSVParser csvParser = CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build()
                 .parse(reader)) {

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
                throw new IllegalArgumentException("No claim records found in input file");
            }

            YearRange yearRange = new YearRange(minOriginYear, maxDevYear);
            logger.info("Scanned year range: {}", yearRange);
            return yearRange;
        }
    }

    /**
     * Streams claims data from a CSV file, processing records one at a time using a consumer.
     *
     * <p>This method performs a memory-efficient streaming read where each record is:
     * <ol>
     *   <li>Parsed from CSV</li>
     *   <li>Validated</li>
     *   <li>Passed to the consumer for processing</li>
     *   <li>Immediately eligible for garbage collection</li>
     * </ol>
     *
     * <p><strong>Memory efficiency:</strong> This method uses O(1) memory for data storage
     * (excluding the consumer's internal state). Records are processed one at a time and
     * not accumulated in memory, making it suitable for very large files.</p>
     *
     * <p><strong>Usage example:</strong></p>
     * <pre>
     * ClaimsReader reader = new ClaimsReader();
     * Map&lt;String, ClaimsTriangle&gt; triangles = new HashMap&lt;&gt;();
     *
     * reader.streamClaims(filePath, record -&gt; {
     *     // Process each record as it's read
     *     String product = record.getProduct();
     *     ClaimsTriangle triangle = triangles.computeIfAbsent(
     *         product, k -&gt; new ClaimsTriangle(product, minYear, maxYear)
     *     );
     *     triangle.addRecord(record);
     * });
     * </pre>
     *
     * @param filePath the path to the CSV file to stream
     * @param recordConsumer consumer function that processes each ClaimRecord as it's read
     * @throws IOException if an I/O error occurs reading the file
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     * @throws NullPointerException if filePath or recordConsumer is null
     */
    public void streamClaims(Path filePath, Consumer<ClaimRecord> recordConsumer) throws IOException {
        if (filePath == null) {
            throw new NullPointerException("File path cannot be null");
        }
        if (recordConsumer == null) {
            throw new NullPointerException("Record consumer cannot be null");
        }

        logger.info("Streaming claims from file: {}", filePath);

        int recordCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath);
             CSVParser csvParser = CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build()
                 .parse(reader)) {

            // Validate header
            validateHeader(csvParser.getHeaderNames());

            // Stream records one at a time
            int lineNumber = 1; // Header is line 0, data starts at line 1
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    ClaimRecord claimRecord = parseRecord(csvRecord, lineNumber);
                    recordConsumer.accept(claimRecord);
                    recordCount++;
                    // claimRecord is now eligible for garbage collection
                } catch (IllegalArgumentException e) {
                    // Catches IllegalArgumentException and its subclass NumberFormatException
                    String errorMsg = String.format(
                        "Error streaming line %d: %s",
                        lineNumber,
                        e.getMessage()
                    );
                    logger.error(errorMsg, e);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }

            logger.info("Successfully streamed {} claim records from {}", recordCount, filePath);
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

        try (BufferedReader reader = Files.newBufferedReader(filePath);
             CSVParser csvParser = CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build()
                 .parse(reader)) {

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

    // ========================================================================
    // InputStream-based methods for Web API support
    // ========================================================================

    /**
     * Performs a lightweight scan of CSV data from an InputStream to determine the year range.
     *
     * <p>This is an overloaded version of {@link #scanForYearRange(Path)} that accepts
     * an InputStream instead of a file path. This is useful for processing uploaded files
     * in web applications where the data is available as a stream.</p>
     *
     * <p><strong>Important:</strong> The caller is responsible for closing the InputStream
     * after this method returns. This method will consume the entire stream.</p>
     *
     * @param inputStream the InputStream containing CSV data
     * @return YearRange containing the minimum origin year and maximum development year
     * @throws IOException if an I/O error occurs reading the stream
     * @throws IllegalArgumentException if the CSV format is invalid, data is malformed, or no valid records are found
     * @throws NullPointerException if inputStream is null
     */
    public YearRange scanForYearRange(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new NullPointerException("InputStream cannot be null");
        }

        logger.info("Scanning for year range from InputStream");

        Integer minOriginYear = null;
        Integer maxDevYear = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build()
                 .parse(reader)) {

            // Validate header
            validateHeader(csvParser.getHeaderNames());

            // Scan records to find min/max years
            int lineNumber = 1;
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    if (csvRecord.size() < 3) {
                        throw new IllegalArgumentException(
                            String.format("Expected at least 3 columns but found %d", csvRecord.size())
                        );
                    }

                    int originYear = parseInt(csvRecord.get(1), "Origin Year");
                    int developmentYear = parseInt(csvRecord.get(2), "Development Year");

                    if (developmentYear < originYear) {
                        throw new IllegalArgumentException(
                            String.format("Development year (%d) cannot be before origin year (%d)",
                                developmentYear, originYear)
                        );
                    }

                    if (minOriginYear == null || originYear < minOriginYear) {
                        minOriginYear = originYear;
                    }
                    if (maxDevYear == null || developmentYear > maxDevYear) {
                        maxDevYear = developmentYear;
                    }
                } catch (IllegalArgumentException e) {
                    String errorMsg = String.format("Error scanning line %d: %s", lineNumber, e.getMessage());
                    logger.error(errorMsg, e);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }

            if (minOriginYear == null || maxDevYear == null) {
                throw new IllegalArgumentException("No claim records found in input stream");
            }

            YearRange yearRange = new YearRange(minOriginYear, maxDevYear);
            logger.info("Scanned year range from InputStream: {}", yearRange);
            return yearRange;
        }
    }

    /**
     * Streams claims data from an InputStream, processing records one at a time using a consumer.
     *
     * <p>This is an overloaded version of {@link #streamClaims(Path, Consumer)} that accepts
     * an InputStream instead of a file path. This is useful for processing uploaded files
     * in web applications.</p>
     *
     * <p><strong>Important:</strong> The caller is responsible for closing the InputStream
     * after this method returns. This method will consume the entire stream.</p>
     *
     * @param inputStream the InputStream containing CSV data
     * @param recordConsumer consumer function that processes each ClaimRecord as it's read
     * @throws IOException if an I/O error occurs reading the stream
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     * @throws NullPointerException if inputStream or recordConsumer is null
     */
    public void streamClaims(InputStream inputStream, Consumer<ClaimRecord> recordConsumer) throws IOException {
        if (inputStream == null) {
            throw new NullPointerException("InputStream cannot be null");
        }
        if (recordConsumer == null) {
            throw new NullPointerException("Record consumer cannot be null");
        }

        logger.info("Streaming claims from InputStream");

        int recordCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build()
                 .parse(reader)) {

            validateHeader(csvParser.getHeaderNames());

            int lineNumber = 1;
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    ClaimRecord claimRecord = parseRecord(csvRecord, lineNumber);
                    recordConsumer.accept(claimRecord);
                    recordCount++;
                } catch (IllegalArgumentException e) {
                    String errorMsg = String.format("Error streaming line %d: %s", lineNumber, e.getMessage());
                    logger.error(errorMsg, e);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }

            logger.info("Successfully streamed {} claim records from InputStream", recordCount);
        }
    }

    /**
     * Reads and parses CSV data from an InputStream into a list of ClaimRecord objects.
     *
     * <p>This is an overloaded version of {@link #readClaims(Path)} that accepts
     * an InputStream instead of a file path. This is useful for processing uploaded files
     * in web applications.</p>
     *
     * <p><strong>Important:</strong> The caller is responsible for closing the InputStream
     * after this method returns. This method will consume the entire stream.</p>
     *
     * @param inputStream the InputStream containing CSV data
     * @return list of parsed and validated ClaimRecord objects
     * @throws IOException if an I/O error occurs reading the stream
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     * @throws NullPointerException if inputStream is null
     */
    public List<ClaimRecord> readClaims(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new NullPointerException("InputStream cannot be null");
        }

        logger.info("Reading claims from InputStream");

        List<ClaimRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT
                 .builder()
                 .setHeader()
                 .setSkipHeaderRecord(true)
                 .setTrim(true)
                 .setIgnoreEmptyLines(true)
                 .build()
                 .parse(reader)) {

            validateHeader(csvParser.getHeaderNames());

            int lineNumber = 1;
            for (CSVRecord csvRecord : csvParser) {
                lineNumber++;
                try {
                    ClaimRecord claimRecord = parseRecord(csvRecord, lineNumber);
                    records.add(claimRecord);
                } catch (IllegalArgumentException e) {
                    String errorMsg = String.format("Error parsing line %d: %s", lineNumber, e.getMessage());
                    logger.error(errorMsg, e);
                    throw new IllegalArgumentException(errorMsg, e);
                }
            }

            logger.info("Successfully read {} claim records from InputStream", records.size());
        }

        return records;
    }

    /**
     * Convenience method to read claims from a CSV string.
     *
     * <p>This method is primarily useful for testing and for processing
     * small amounts of in-memory CSV data.</p>
     *
     * @param csvContent the CSV content as a string
     * @return list of parsed and validated ClaimRecord objects
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     * @throws NullPointerException if csvContent is null
     */
    public List<ClaimRecord> readClaims(String csvContent) throws IOException {
        if (csvContent == null) {
            throw new NullPointerException("CSV content cannot be null");
        }
        return readClaims(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
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
