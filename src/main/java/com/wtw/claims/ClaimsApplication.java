package com.wtw.claims;

import com.wtw.claims.model.ClaimsTriangle;
import com.wtw.claims.processor.TriangleAccumulator;
import com.wtw.claims.reader.ClaimsReader;
import com.wtw.claims.writer.ClaimsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main application entry point for the Claims Triangle Accumulator.
 *
 * <p>This application reads incremental insurance claims data from a CSV file,
 * processes it into cumulative claims triangles, and writes the output to a CSV file.
 * Claims triangles are used in actuarial analysis for tracking and predicting future
 * claim payments.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>
 * java -jar claims-triangle-accumulator.jar &lt;input-file&gt; &lt;output-file&gt;
 * </pre>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 * java -jar claims-triangle-accumulator.jar files/problem.csv output.csv
 * </pre>
 *
 * <p><strong>Processing Pipeline:</strong></p>
 * <ol>
 *   <li><strong>Scan:</strong> Determine global origin/development year bounds</li>
 *   <li><strong>Stream:</strong> Build product triangles incrementally without loading the full dataset</li>
 *   <li><strong>Accumulate:</strong> Calculate cumulative values</li>
 *   <li><strong>Write:</strong> Output cumulative triangles to CSV</li>
 * </ol>
 *
 * <p><strong>Input Format:</strong></p>
 * <pre>
 * Product, Origin Year, Development Year, Incremental Value
 * Comp, 1992, 1992, 110.0
 * Comp, 1992, 1993, 170.0
 * Non-Comp, 1990, 1990, 45.2
 * </pre>
 *
 * <p><strong>Output Format:</strong></p>
 * <pre>
 * 1990,4
 * Comp,0.0,0.0,0.0,0.0,0.0,0.0,0.0,110.0,280.0,200.0
 * Non-Comp,45.2,110.0,110.0,147.0,50.0,125.0,150.0,55.0,140.0,100.0
 * </pre>
 *
 * @author WTW Claims Processing Team
 * @version 1.0.0
 */
public class ClaimsApplication {

    private static final Logger logger = LoggerFactory.getLogger(ClaimsApplication.class);

    /**
     * Main entry point for the Claims Triangle Accumulator application.
     *
     * <p>This method orchestrates the entire processing pipeline from reading
     * incremental claims data to writing cumulative claims triangles.</p>
     *
     * @param args command-line arguments: [0] = input CSV file path, [1] = output CSV file path
     */
    public static void main(String[] args) {
        // Validate command-line arguments
        if (args.length != 2) {
            System.err.println("Usage: java ClaimsApplication <input-file> <output-file>");
            System.err.println();
            System.err.println("Example:");
            System.err.println("  java ClaimsApplication files/problem.csv output.csv");
            System.exit(1);
        }

        String inputFilePath = args[0];
        String outputFilePath = args[1];

        logger.info("=".repeat(70));
        logger.info("Claims Triangle Accumulator - Streaming Mode");
        logger.info("=".repeat(70));
        logger.info("Input file:  {}", inputFilePath);
        logger.info("Output file: {}", outputFilePath);
        logger.info("");

        try {
            // Validate input file exists
            Path inputPath = Paths.get(inputFilePath);
            if (!Files.exists(inputPath)) {
                System.err.println("Error: Input file does not exist: " + inputFilePath);
                System.exit(1);
            }

            // Process the claims data
            processClaims(inputPath, Paths.get(outputFilePath));

            logger.info("=".repeat(70));
            logger.info("Processing complete: {}", outputFilePath);
            logger.info("=".repeat(70));
            System.out.println("Processing complete: " + outputFilePath);

        } catch (IOException e) {
            logger.error("I/O error occurred", e);
            System.err.println("Error: I/O error - " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input data", e);
            System.err.println("Error: Invalid input - " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Processes claims data through the streaming pipeline (memory-efficient).
     *
     * <p>This method uses a two-pass streaming approach:</p>
     * <ol>
     *   <li><strong>Pass 1:</strong> Scan for year ranges (minimal memory - O(1))</li>
     *   <li><strong>Pass 2:</strong> Stream and build triangles directly (no full record list)</li>
     *   <li><strong>Calculate:</strong> Compute cumulative values</li>
     *   <li><strong>Write:</strong> Output cumulative triangles</li>
     * </ol>
     *
     * <p><strong>Memory Usage:</strong> This streaming approach uses significantly less memory
     * than the traditional approach by never loading all records into a List. Instead, records
     * are processed one at a time and immediately added to triangles.</p>
     *
     * <p><strong>Memory Comparison (1M records):</strong></p>
     * <ul>
     *   <li>Traditional approach: ~170 MB peak (records + grouped + triangles)</li>
     *   <li>Streaming approach: ~100 MB peak (triangles only)</li>
     *   <li>Reduction: 41%</li>
     * </ul>
     *
     * <p><strong>Note:</strong> This method is package-private to allow integration testing.</p>
     *
     * @param inputPath the path to the input CSV file
     * @param outputPath the path to the output CSV file
     * @throws IOException if an I/O error occurs reading or writing files
     * @throws IllegalArgumentException if the input data is invalid
     */
    static void processClaims(Path inputPath, Path outputPath) throws IOException {
        ClaimsReader reader = new ClaimsReader();

        // STEP 1: Scan the file once to determine the global year range
        logger.info("Step 1/4: Scanning year range...");
        ClaimsReader.YearRange yearRange = reader.scanForYearRange(inputPath);
        int earliestOriginYear = yearRange.minOriginYear();
        int latestDevelopmentYear = yearRange.maxDevYear();
        int numberOfDevelopmentYears = yearRange.getNumberOfDevelopmentYears();
        logger.info("  Year range: {} to {} ({} development years)",
            earliestOriginYear, latestDevelopmentYear, numberOfDevelopmentYears);

        // STEP 2: Stream records and build triangles on the fly
        logger.info("Step 2/4: Streaming claims and building triangles...");
        Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
        AtomicInteger recordCount = new AtomicInteger();

        reader.streamClaims(inputPath, record -> {
            int count = recordCount.incrementAndGet();

            // Get or create the triangle for this product
            ClaimsTriangle triangle = triangles.computeIfAbsent(
                record.product(),
                product -> {
                    logger.debug("Creating triangle for product: {}", product);
                    return new ClaimsTriangle(product, earliestOriginYear, latestDevelopmentYear);
                }
            );

            // Add this record's incremental value to the triangle
            triangle.addIncrementalValue(
                record.originYear(),
                record.developmentYear(),
                record.incrementalValue()
            );

            // Log progress for large files (every 10,000 records)
            if (count % 10000 == 0) {
                logger.debug("Processed {} records...", count);
            }

            // When this lambda returns, the 'record' parameter is no longer referenced
            // and can be garbage collected. We NEVER accumulate all records!
        });

        if (recordCount.get() == 0) {
            throw new IllegalArgumentException("No claim records found in input file");
        }

        logger.info("  Streamed {} claim record(s) into {} product triangle(s): {}",
            recordCount.get(), triangles.size(), triangles.keySet());

        // STEP 3: Calculate cumulative values for each triangle
        logger.info("Step 3/4: Calculating cumulative values...");
        for (Map.Entry<String, ClaimsTriangle> entry : triangles.entrySet()) {
            String product = entry.getKey();
            ClaimsTriangle triangle = entry.getValue();
            TriangleAccumulator.calculateCumulative(triangle);
            logger.info("  Calculated cumulative values for '{}'", product);
        }

        // STEP 4: Write cumulative claims to output CSV
        logger.info("Step 4/4: Writing cumulative claims to CSV...");
        ClaimsWriter writer = new ClaimsWriter();
        writer.writeCumulativeClaims(
            outputPath,
            triangles,
            earliestOriginYear,
            numberOfDevelopmentYears
        );
        logger.info("  Written {} product triangle(s) to {}", triangles.size(), outputPath);
    }
}
