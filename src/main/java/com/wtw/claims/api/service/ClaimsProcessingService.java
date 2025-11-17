package com.wtw.claims.api.service;

import com.wtw.claims.api.dto.response.ProcessingMetadata;
import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import com.wtw.claims.api.dto.response.ProductResult;
import com.wtw.claims.model.ClaimsTriangle;
import com.wtw.claims.processor.TriangleAccumulator;
import com.wtw.claims.reader.ClaimsReader;
import com.wtw.claims.reader.ClaimsReader.YearRange;
import com.wtw.claims.writer.ClaimsWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for processing claims CSV data and producing cumulative triangles.
 *
 * <p>This service orchestrates the complete claims processing pipeline:
 * <ol>
 *   <li>Reading and parsing CSV input</li>
 *   <li>Building claims triangles for each product</li>
 *   <li>Calculating cumulative values</li>
 *   <li>Formatting output and building response</li>
 * </ol>
 *
 * <p>The service uses a two-pass approach for memory efficiency:
 * <ol>
 *   <li>First pass: scan for year range (determines triangle dimensions)</li>
 *   <li>Second pass: stream records and build triangles</li>
 * </ol>
 *
 * @author Claims Processing Team
 * @version 2.0.0
 */
@Service
public class ClaimsProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(ClaimsProcessingService.class);

    private final ClaimsReader claimsReader;
    private final ClaimsWriter claimsWriter;

    /**
     * Constructs a new ClaimsProcessingService with default reader and writer.
     */
    public ClaimsProcessingService() {
        this.claimsReader = new ClaimsReader();
        this.claimsWriter = new ClaimsWriter();
    }

    /**
     * Constructs a new ClaimsProcessingService with custom reader and writer.
     *
     * @param claimsReader the reader for parsing CSV data
     * @param claimsWriter the writer for formatting output
     * @throws NullPointerException if any parameter is null
     */
    public ClaimsProcessingService(ClaimsReader claimsReader, ClaimsWriter claimsWriter) {
        if (claimsReader == null) {
            throw new NullPointerException("ClaimsReader cannot be null");
        }
        if (claimsWriter == null) {
            throw new NullPointerException("ClaimsWriter cannot be null");
        }
        this.claimsReader = claimsReader;
        this.claimsWriter = claimsWriter;
    }

    /**
     * Processes claims CSV data and returns cumulative triangle results.
     *
     * <p>This method performs the complete processing pipeline:
     * <ol>
     *   <li>Reads CSV content into memory (to allow two-pass processing)</li>
     *   <li>Scans for year range to determine triangle dimensions</li>
     *   <li>Streams records to build triangles for each product</li>
     *   <li>Calculates cumulative values for each triangle</li>
     *   <li>Formats output and builds comprehensive response</li>
     * </ol>
     *
     * @param inputStream the InputStream containing CSV data
     * @return ProcessingResultResponse containing metadata, results, and formatted CSV output
     * @throws IOException if an I/O error occurs reading the stream
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     * @throws NullPointerException if inputStream is null
     */
    public ProcessingResultResponse processClaims(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new NullPointerException("InputStream cannot be null");
        }

        long startTime = System.currentTimeMillis();
        logger.info("Starting claims processing");

        // Read entire content into memory for two-pass processing
        byte[] csvBytes = inputStream.readAllBytes();
        logger.debug("Read {} bytes from input stream", csvBytes.length);

        // First pass: scan for year range
        YearRange yearRange = claimsReader.scanForYearRange(
            new ByteArrayInputStream(csvBytes)
        );
        int earliestOriginYear = yearRange.minOriginYear();
        int latestDevYear = yearRange.maxDevYear();
        int numberOfDevelopmentYears = yearRange.getNumberOfDevelopmentYears();

        logger.info("Year range: {} to {} ({} development years)",
            earliestOriginYear, latestDevYear, numberOfDevelopmentYears);

        // Second pass: stream records and build triangles
        Map<String, ClaimsTriangle> triangles = new TreeMap<>();
        AtomicInteger recordCount = new AtomicInteger(0);

        claimsReader.streamClaims(
            new ByteArrayInputStream(csvBytes),
            record -> {
                String product = record.product();

                // Get or create triangle for this product
                ClaimsTriangle triangle = triangles.computeIfAbsent(
                    product,
                    p -> new ClaimsTriangle(p, earliestOriginYear, latestDevYear)
                );

                // Add incremental value to triangle
                triangle.addIncrementalValue(
                    record.originYear(),
                    record.developmentYear(),
                    record.incrementalValue()
                );

                recordCount.incrementAndGet();
            }
        );

        logger.info("Processed {} records for {} products",
            recordCount.get(), triangles.size());

        // Calculate cumulative values for each triangle
        for (ClaimsTriangle triangle : triangles.values()) {
            TriangleAccumulator.calculateCumulative(triangle);
        }

        logger.debug("Calculated cumulative values for all triangles");

        // Build response
        List<ProductResult> productResults = new ArrayList<>();
        for (Map.Entry<String, ClaimsTriangle> entry : triangles.entrySet()) {
            String productName = entry.getKey();
            ClaimsTriangle triangle = entry.getValue();
            List<Double> cumulativeValues = claimsWriter.getCumulativeValuesAsList(triangle);
            productResults.add(new ProductResult(productName, cumulativeValues));
        }

        // Format CSV output
        String csvOutput = claimsWriter.formatCumulativeClaims(
            triangles,
            earliestOriginYear,
            numberOfDevelopmentYears
        );

        // Build metadata
        long processingTimeMs = System.currentTimeMillis() - startTime;
        List<String> productsProcessed = new ArrayList<>(triangles.keySet());

        ProcessingMetadata metadata = new ProcessingMetadata(
            earliestOriginYear,
            numberOfDevelopmentYears,
            recordCount.get(),
            productsProcessed,
            processingTimeMs
        );

        logger.info("Claims processing completed in {} ms", processingTimeMs);

        return new ProcessingResultResponse(metadata, productResults, csvOutput);
    }

    /**
     * Processes claims from a CSV string.
     *
     * <p>This is a convenience method primarily useful for testing.</p>
     *
     * @param csvContent the CSV content as a string
     * @return ProcessingResultResponse containing metadata, results, and formatted CSV output
     * @throws IOException if an I/O error occurs
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     * @throws NullPointerException if csvContent is null
     */
    public ProcessingResultResponse processClaims(String csvContent) throws IOException {
        if (csvContent == null) {
            throw new NullPointerException("CSV content cannot be null");
        }
        return processClaims(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
    }
}