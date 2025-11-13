package com.wtw.claims.writer;

import com.wtw.claims.model.ClaimsTriangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Writes cumulative claims triangle data to CSV files.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Writing cumulative claims data in the specified output format</li>
 *   <li>Formatting decimal values correctly (preserving .0 for whole numbers)</li>
 *   <li>Sorting products alphabetically</li>
 *   <li>Generating header line with earliest origin year and development year count</li>
 * </ul>
 *
 * <p>Expected output format:
 * <pre>
 * 1990,4
 * Comp,0.0,0.0,0.0,0.0,0.0,0.0,0.0,110.0,280.0,200.0
 * Non-Comp,45.2,110.0,110.0,147.0,50.0,125.0,150.0,55.0,140.0,100.0
 * </pre>
 *
 * <p><strong>Format specifications:</strong>
 * <ul>
 *   <li>Line 1: {@code <earliest_origin_year>,<number_of_development_years>}</li>
 *   <li>Subsequent lines: {@code <Product>,<cumulative_value_1>,<cumulative_value_2>,...}</li>
 *   <li>NO spaces after commas</li>
 *   <li>Decimal format: Always include decimal point (e.g., 110.0 not 110)</li>
 *   <li>Products sorted alphabetically (case-sensitive)</li>
 * </ul>
 *
 * @author WTW Claims Processing Team
 * @version 1.0
 */
public class ClaimsWriter {

    private static final Logger logger = LoggerFactory.getLogger(ClaimsWriter.class);

    /**
     * Maximum number of decimal places to display in output.
     */
    private static final int MAX_DECIMAL_PRECISION = 10;

    /**
     * Writes cumulative claims data to a CSV file.
     *
     * <p>The output format consists of:
     * <ol>
     *   <li>A header line: earliest origin year and number of development years</li>
     *   <li>One line per product with flattened cumulative values</li>
     * </ol>
     *
     * <p>Products are sorted alphabetically by name (case-sensitive). The cumulative
     * values are obtained by calling {@link ClaimsTriangle#flattenCumulative()} on
     * each triangle.
     *
     * @param filePath the path where the output CSV file will be written
     * @param triangles a map of product names to their corresponding ClaimsTriangle objects
     * @param earliestOriginYear the earliest origin year across all products
     * @param numberOfDevelopmentYears the number of development years in the dataset
     * @throws IOException if an I/O error occurs writing to the file
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if triangles map is empty, contains null values, or numberOfDevelopmentYears is not positive
     */
    public void writeCumulativeClaims(
            Path filePath,
            Map<String, ClaimsTriangle> triangles,
            int earliestOriginYear,
            int numberOfDevelopmentYears
    ) throws IOException {
        // Validate ALL inputs first before opening file to avoid partial file creation on validation failure
        Objects.requireNonNull(filePath, "File path cannot be null");
        Objects.requireNonNull(triangles, "Triangles map cannot be null");

        if (triangles.isEmpty()) {
            throw new IllegalArgumentException("Triangles map cannot be empty");
        }

        if (numberOfDevelopmentYears <= 0) {
            throw new IllegalArgumentException(
                "Number of development years must be positive, but was: " + numberOfDevelopmentYears
            );
        }

        // Validate that no triangle values are null
        for (Map.Entry<String, ClaimsTriangle> entry : triangles.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                    "Triangle for product '" + entry.getKey() + "' cannot be null"
                );
            }
        }

        logger.info("Writing cumulative claims to file: {}", filePath);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            // Write header line: earliest_origin_year,number_of_development_years
            String headerLine = String.format("%d,%d", earliestOriginYear, numberOfDevelopmentYears);
            writer.write(headerLine);
            writer.newLine();
            logger.debug("Written header: {}", headerLine);

            // Sort products alphabetically and write each product line
            List<String> sortedProducts = triangles.keySet()
                .stream().sorted().collect(Collectors.toList());

            for (String productName : sortedProducts) {
                ClaimsTriangle triangle = triangles.get(productName);
                String productLine = formatProductLine(productName, triangle);
                writer.write(productLine);
                writer.newLine();
                logger.debug("Written product line for '{}': {} values", productName,
                           triangle.flattenCumulative().size());
            }

            logger.info("Successfully wrote cumulative claims for {} products to {}",
                       triangles.size(), filePath);
        }
    }

    /**
     * Formats a product line for the output CSV.
     *
     * <p>The format is: {@code Product,value1,value2,value3,...}
     * with NO spaces after commas and decimal values formatted to one decimal place.
     *
     * @param productName the name of the product
     * @param triangle the ClaimsTriangle containing cumulative values
     * @return a formatted CSV line for this product
     */
    private String formatProductLine(String productName, ClaimsTriangle triangle) {
        List<Double> cumulativeValues = triangle.flattenCumulative();

        // Format each value with one decimal place (e.g., 110.0, 45.2)
        String valuesString = cumulativeValues.stream()
            .map(value -> formatDecimal(value))
            .collect(Collectors.joining(","));

        return productName + "," + valuesString;
    }

    /**
     * Formats a decimal value for output.
     *
     * <p>This method ensures that decimal values are formatted consistently:
     * <ul>
     *   <li>Always includes decimal point and at least one decimal place</li>
     *   <li>Removes trailing zeros beyond the first decimal place</li>
     *   <li>Examples: 110.0, 45.2, 37.0</li>
     *   <li>Uses consistent decimal separator regardless of locale (always '.')</li>
     *   <li>Uses HALF_UP rounding mode for predictable behavior</li>
     * </ul>
     *
     * <p>The implementation uses {@link DecimalFormat} to ensure consistent,
     * locale-independent formatting and to avoid floating-point precision artifacts
     * that can occur with string formatting.
     *
     * @param value the decimal value to format
     * @return the formatted string representation
     */
    private String formatDecimal(double value) {
        // Ensure consistent formatting regardless of locale (always use '.' as decimal separator)
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("#.#########", symbols);
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);  // Ensures at least one decimal place (e.g., 110.0)
        df.setMaximumFractionDigits(MAX_DECIMAL_PRECISION);
        df.setGroupingUsed(false);  // No thousand separators

        return df.format(value);
    }
}
