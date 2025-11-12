package com.wtw.claims;

import com.wtw.claims.model.ClaimRecord;
import com.wtw.claims.model.ClaimsTriangle;
import com.wtw.claims.processor.DataProcessor;
import com.wtw.claims.processor.TriangleAccumulator;
import com.wtw.claims.processor.TriangleBuilder;
import com.wtw.claims.reader.ClaimsReader;
import com.wtw.claims.writer.ClaimsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 *   <li><strong>Read:</strong> Parse incremental claims data from CSV</li>
 *   <li><strong>Group:</strong> Organize claims by product</li>
 *   <li><strong>Build:</strong> Create claims triangles for each product</li>
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
        logger.info("Claims Triangle Accumulator");
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
            e.printStackTrace();
            System.exit(1);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid input data", e);
            System.err.println("Error: Invalid input - " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Processes claims data through the complete pipeline.
     *
     * <p>This method orchestrates the five main steps:</p>
     * <ol>
     *   <li><strong>Read:</strong> Load incremental claims from CSV</li>
     *   <li><strong>Group:</strong> Organize claims by product</li>
     *   <li><strong>Build:</strong> Create triangle structures</li>
     *   <li><strong>Accumulate:</strong> Calculate cumulative values</li>
     *   <li><strong>Write:</strong> Output cumulative triangles</li>
     * </ol>
     *
     * @param inputPath the path to the input CSV file
     * @param outputPath the path to the output CSV file
     * @throws IOException if an I/O error occurs reading or writing files
     * @throws IllegalArgumentException if the input data is invalid
     */
    private static void processClaims(Path inputPath, Path outputPath) throws IOException {
        // STEP 1: Read claims from CSV file
        logger.info("Step 1/5: Reading claims from CSV...");
        ClaimsReader reader = new ClaimsReader();
        List<ClaimRecord> records = reader.readClaims(inputPath);
        logger.info("  Read {} claim records", records.size());

        // Validate we have data to process
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No claim records found in input file");
        }

        // STEP 2: Group claims by product and determine year ranges
        logger.info("Step 2/5: Grouping claims by product...");
        Map<String, List<ClaimRecord>> recordsByProduct = DataProcessor.groupByProduct(records);
        logger.info("  Found {} product(s): {}", recordsByProduct.size(), recordsByProduct.keySet());

        int earliestOriginYear = DataProcessor.findEarliestOriginYear(records);
        int latestDevelopmentYear = DataProcessor.findLatestDevelopmentYear(records);
        int numberOfDevelopmentYears = DataProcessor.getNumberOfDevelopmentYears(records);
        logger.info("  Year range: {} to {} ({} development years)",
                   earliestOriginYear, latestDevelopmentYear, numberOfDevelopmentYears);

        // STEP 3: Build triangles for each product
        logger.info("Step 3/5: Building claims triangles...");
        Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
        for (Map.Entry<String, List<ClaimRecord>> entry : recordsByProduct.entrySet()) {
            String product = entry.getKey();
            List<ClaimRecord> productRecords = entry.getValue();

            ClaimsTriangle triangle = TriangleBuilder.buildTriangle(
                product,
                productRecords,
                earliestOriginYear,
                latestDevelopmentYear
            );
            triangles.put(product, triangle);
            logger.info("  Built triangle for '{}' ({} records)", product, productRecords.size());
        }

        // STEP 4: Calculate cumulative values for each triangle
        logger.info("Step 4/5: Calculating cumulative values...");
        for (Map.Entry<String, ClaimsTriangle> entry : triangles.entrySet()) {
            String product = entry.getKey();
            ClaimsTriangle triangle = entry.getValue();
            TriangleAccumulator.calculateCumulative(triangle);
            logger.info("  Calculated cumulative values for '{}'", product);
        }

        // STEP 5: Write cumulative claims to output CSV
        logger.info("Step 5/5: Writing cumulative claims to CSV...");
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
