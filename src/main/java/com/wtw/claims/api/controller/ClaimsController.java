package com.wtw.claims.api.controller;

import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import com.wtw.claims.api.service.ClaimsProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * REST controller for claims processing operations.
 *
 * <p>This controller provides endpoints for:
 * <ul>
 *   <li>Synchronous processing of claims CSV files</li>
 *   <li>Future: Asynchronous processing for large files</li>
 * </ul>
 *
 * <p><strong>API Version:</strong> v1</p>
 *
 * @author Claims Processing Team
 * @version 2.0.0
 */
@RestController
@RequestMapping("/api/v1/claims")
public class ClaimsController {

    private static final Logger logger = LoggerFactory.getLogger(ClaimsController.class);

    private final ClaimsProcessingService processingService;
    private final int maxSyncFileSizeMb;

    /**
     * Constructs a new ClaimsController with the specified service.
     *
     * @param processingService the service for processing claims data
     * @param maxSyncFileSizeMb maximum file size for synchronous processing (in MB)
     */
    public ClaimsController(
            ClaimsProcessingService processingService,
            @Value("${claims.processing.sync.max-file-size-mb:10}") int maxSyncFileSizeMb
    ) {
        this.processingService = processingService;
        this.maxSyncFileSizeMb = maxSyncFileSizeMb;
    }

    /**
     * Processes a claims CSV file synchronously.
     *
     * <p>This endpoint accepts a CSV file upload and processes it immediately,
     * returning the cumulative triangle results. The response includes:
     * <ul>
     *   <li>Processing metadata (timing, record counts, etc.)</li>
     *   <li>Individual product results with cumulative values</li>
     *   <li>Complete CSV output string</li>
     * </ul>
     *
     * <p><strong>Usage:</strong></p>
     * <pre>
     * curl -X POST http://localhost:8080/api/v1/claims/process \
     *   -F "file=@claims.csv" \
     *   -H "Content-Type: multipart/form-data"
     * </pre>
     *
     * <p><strong>File requirements:</strong></p>
     * <ul>
     *   <li>Must be a valid CSV file</li>
     *   <li>Must contain header: Product, Origin Year, Development Year, Incremental Value</li>
     *   <li>Maximum file size: configured in application.yml (default 50MB)</li>
     * </ul>
     *
     * @param file the CSV file to process
     * @return ResponseEntity containing the processing results
     * @throws IOException if an I/O error occurs reading the file
     * @throws IllegalArgumentException if the CSV format is invalid or data is malformed
     */
    @PostMapping(
        value = "/process",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ProcessingResultResponse> processClaimsSync(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        // Sanitize filename for logging (prevent log injection)
        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        logger.info("Received claims processing request: filename={}, size={} bytes",
            safeFilename, file.getSize());

        // Validate file is not empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Validate file size for synchronous processing
        long maxBytes = maxSyncFileSizeMb * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                String.format("File size %d bytes exceeds maximum allowed %d MB for synchronous processing",
                    file.getSize(), maxSyncFileSizeMb));
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("File must be a CSV file");
        }

        // Validate content type (optional warning - CSV files may have various content types)
        String contentType = file.getContentType();
        if (contentType != null && !contentType.contains("csv")
                && !contentType.equals("text/plain")
                && !contentType.equals("application/octet-stream")) {
            logger.warn("Unexpected content type for CSV file: {}", contentType);
        }

        // Process with try-with-resources to ensure InputStream is closed
        ProcessingResultResponse response;
        try (InputStream inputStream = file.getInputStream()) {
            response = processingService.processClaims(inputStream);
        }

        logger.info("Claims processing completed successfully: {} products, {} records",
            response.results().size(), response.metadata().totalRecords());

        return ResponseEntity.ok(response);
    }

    /**
     * Sanitizes a filename to prevent log injection attacks.
     *
     * @param filename the original filename
     * @return sanitized filename safe for logging
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }
        // Remove potentially dangerous characters, keep only safe ones
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}