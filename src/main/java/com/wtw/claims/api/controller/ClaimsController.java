package com.wtw.claims.api.controller;

import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import com.wtw.claims.api.service.ClaimsProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    /**
     * Constructs a new ClaimsController with the specified service.
     *
     * @param processingService the service for processing claims data
     */
    public ClaimsController(ClaimsProcessingService processingService) {
        this.processingService = processingService;
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
        logger.info("Received claims processing request: filename={}, size={} bytes",
            file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Validate file type (basic check)
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("File must be a CSV file");
        }

        ProcessingResultResponse response = processingService.processClaims(file.getInputStream());

        logger.info("Claims processing completed successfully: {} products, {} records",
            response.results().size(), response.metadata().totalRecords());

        return ResponseEntity.ok(response);
    }
}