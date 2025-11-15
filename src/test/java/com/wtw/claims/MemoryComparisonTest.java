package com.wtw.claims;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Performance tests comparing memory usage of the streaming approach.
 * These tests are disabled by default as they're meant for manual verification.
 *
 * <p>To run these tests, remove the {@code @Disabled} annotation and execute:
 * <pre>
 * mvn test -Dtest=MemoryComparisonTest
 * </pre>
 *
 * <p><strong>Expected Results:</strong></p>
 * <ul>
 *   <li>10K records: ~5-15 MB memory usage</li>
 *   <li>100K records: ~20-60 MB memory usage</li>
 * </ul>
 */
@Disabled("Performance tests - run manually when needed")
class MemoryComparisonTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Memory usage test with 10K records")
    void memoryUsage_with10KRecords() throws IOException, InterruptedException {
        // Generate large test file
        Path testFile = generateLargeTestFile(10_000);
        Path outputFile = tempDir.resolve("output.csv");

        // Measure memory usage
        Runtime runtime = Runtime.getRuntime();
        System.gc(); // Suggest garbage collection
        Thread.sleep(100); // Give GC time

        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        // Run streaming approach
        ClaimsApplication.processClaims(testFile, outputFile);

        System.gc(); // Suggest garbage collection
        Thread.sleep(100);
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        System.out.println("=".repeat(70));
        System.out.println("Memory Usage Test - 10,000 Records");
        System.out.println("=".repeat(70));
        System.out.println("Records: 10,000");
        System.out.println("Memory used (streaming): " + (memUsed / 1024 / 1024) + " MB");
        System.out.println("File size: " + (Files.size(testFile) / 1024) + " KB");
        System.out.println("Output file size: " + (Files.size(outputFile) / 1024) + " KB");
        System.out.println("=".repeat(70));

        // This is just for observation - not a hard assertion
        // Typical results: 5-15 MB for 10K records with streaming
    }

    @Test
    @DisplayName("Memory usage test with 100K records")
    void memoryUsage_with100KRecords() throws IOException, InterruptedException {
        Path testFile = generateLargeTestFile(100_000);
        Path outputFile = tempDir.resolve("output.csv");

        Runtime runtime = Runtime.getRuntime();
        System.gc();
        Thread.sleep(100);

        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        ClaimsApplication.processClaims(testFile, outputFile);

        System.gc();
        Thread.sleep(100);
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        System.out.println("=".repeat(70));
        System.out.println("Memory Usage Test - 100,000 Records");
        System.out.println("=".repeat(70));
        System.out.println("Records: 100,000");
        System.out.println("Memory used (streaming): " + (memUsed / 1024 / 1024) + " MB");
        System.out.println("File size: " + (Files.size(testFile) / 1024 / 1024) + " MB");
        System.out.println("Output file size: " + (Files.size(outputFile) / 1024) + " KB");
        System.out.println("=".repeat(70));

        // Typical results: 20-60 MB for 100K records with streaming
    }

    @Test
    @DisplayName("Memory usage test with 1M records")
    void memoryUsage_with1MRecords() throws IOException, InterruptedException {
        Path testFile = generateLargeTestFile(1_000_000);
        Path outputFile = tempDir.resolve("output.csv");

        Runtime runtime = Runtime.getRuntime();
        System.gc();
        Thread.sleep(100);

        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        ClaimsApplication.processClaims(testFile, outputFile);

        long endTime = System.currentTimeMillis();
        System.gc();
        Thread.sleep(100);
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsed = memAfter - memBefore;

        System.out.println("=".repeat(70));
        System.out.println("Memory Usage Test - 1,000,000 Records");
        System.out.println("=".repeat(70));
        System.out.println("Records: 1,000,000");
        System.out.println("Memory used (streaming): " + (memUsed / 1024 / 1024) + " MB");
        System.out.println("Processing time: " + (endTime - startTime) + " ms");
        System.out.println("File size: " + (Files.size(testFile) / 1024 / 1024) + " MB");
        System.out.println("Output file size: " + (Files.size(outputFile) / 1024 / 1024) + " MB");
        System.out.println("=".repeat(70));

        // Expected: ~100 MB for 1M records (per STREAMING_IMPLEMENTATION_PLAN.md)
    }

    /**
     * Generates a large test CSV file with the specified number of records.
     *
     * <p>Test data characteristics:</p>
     * <ul>
     *   <li>100 different products (Product_0 through Product_99)</li>
     *   <li>Origin years: 1990-1999 (cycling)</li>
     *   <li>Development years: origin year + 0 to 4 years</li>
     *   <li>Incremental values: 100.0 to 1099.0</li>
     * </ul>
     *
     * @param recordCount the number of records to generate
     * @return path to the generated test file
     * @throws IOException if an I/O error occurs
     */
    private Path generateLargeTestFile(int recordCount) throws IOException {
        Path file = tempDir.resolve("large_test.csv");
        List<String> lines = new ArrayList<>();

        lines.add("Product, Origin Year, Development Year, Incremental Value");

        for (int i = 0; i < recordCount; i++) {
            int productNum = i % 100; // 100 different products
            int originYear = 1990 + (i % 10);
            int devYear = originYear + (i % 5);
            double value = 100.0 + (i % 1000);

            lines.add(String.format("Product_%d, %d, %d, %.1f",
                productNum, originYear, devYear, value));
        }

        Files.write(file, lines);
        return file;
    }
}