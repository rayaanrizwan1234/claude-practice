package com.wtw.claims.api.controller;

import com.wtw.claims.api.dto.response.AsyncJobResponse;
import com.wtw.claims.api.dto.response.JobStatusResponse;
import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import com.wtw.claims.api.service.AsyncJobService;
import com.wtw.claims.api.service.ClaimsProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

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
    private final AsyncJobService asyncJobService;

    /**
     * Constructs a new ClaimsController with the specified service.
     *
     * @param processingService the service for processing claims data
     * @param maxSyncFileSizeMb maximum file size for synchronous processing (in MB)
     */
    public ClaimsController(
            ClaimsProcessingService processingService,
            @Value("${claims.processing.sync.max-file-size-mb:10}") int maxSyncFileSizeMb,
            AsyncJobService asyncJobService
    ) {
        this.processingService = processingService;
        this.maxSyncFileSizeMb = maxSyncFileSizeMb;
        this.asyncJobService = asyncJobService;
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
     * Processes a claims CSV file asynchronously.
     *
     * <p>This endpoint accepts CSV file uploads for large files and returns immediately
     * with a job ID. Use the job status and result endpoints to track progress and
     * retrieve results.
     *
     * @param file the CSV file to process (max 50MB)
     * @return ResponseEntity with 202 ACCEPTED and job tracking information
     * @throws IOException if an I/O error occurs reading the file
     * @throws IllegalArgumentException if validation fails
     * @throws IllegalStateException if maximum concurrent jobs limit is reached
     */
    @PostMapping(
        value = "/process/async",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AsyncJobResponse> processClaimsAsync(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        // Sanitize filename for logging
        String safeFilename = sanitizeFilename(file.getOriginalFilename());
        logger.info("Received async processing request: filename={}, size={} bytes",
            safeFilename, file.getSize());

        // Validate file is not empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        // Validate file size for async processing (50MB max)
        long maxAsyncBytes = 50L * 1024L * 1024L;
        if (file.getSize() > maxAsyncBytes) {
            throw new IllegalArgumentException(
                String.format("File size %d bytes exceeds maximum allowed 50 MB for async processing",
                    file.getSize()));
        }

        // Validate file extension
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("File must be a CSV file");
        }

        // Validate content type (optional warning)
        String contentType = file.getContentType();
        if (contentType != null && !contentType.contains("csv")
                && !contentType.equals("text/plain")
                && !contentType.equals("application/octet-stream")) {
            logger.warn("Unexpected content type for CSV file: {}", contentType);
        }

        // Submit job for async processing
        UUID jobId = asyncJobService.submitJob(file.getBytes());
        String statusUrl = "/api/v1/claims/jobs/" + jobId;

        JobStatusResponse jobStatus = asyncJobService.getJobStatus(jobId);

        AsyncJobResponse response = new AsyncJobResponse(
            jobStatus.jobId(),
            jobStatus.status(),
            jobStatus.createdAt(),
            statusUrl
        );

        logger.info("Async job submitted: jobId={}, statusUrl={}", jobId, statusUrl);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Retrieves the status of an asynchronous processing job.
     *
     * <p>This endpoint allows clients to poll for job status updates.
     *
     * <p><strong>Usage:</strong></p>
     * <pre>
     * curl http://localhost:8080/api/v1/claims/jobs/{jobId}
     * </pre>
     *
     * @param jobId the unique job identifier
     * @return ResponseEntity with job status information
     * @throws com.wtw.claims.api.exception.JobNotFoundException if job doesn't exist
     */
    @GetMapping(value = "/jobs/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        logger.debug("Job status request for jobId={}", jobId);

        JobStatusResponse status = asyncJobService.getJobStatus(jobId);

        return ResponseEntity.ok(status);
    }

    /**
     * Retrieves the processing result for a completed asynchronous job.
     *
     * <p>This endpoint returns the full processing results once the job completes.
     * If the job is not yet complete, a 400 Bad Request is returned.
     *
     * <p><strong>Usage:</strong></p>
     * <pre>
     * curl http://localhost:8080/api/v1/claims/jobs/{jobId}/result
     * </pre>
     *
     * @param jobId the unique job identifier
     * @return ResponseEntity with processing results
     * @throws com.wtw.claims.api.exception.JobNotFoundException if job doesn't exist
     * @throws com.wtw.claims.api.exception.JobNotCompletedException if job not complete
     */
    @GetMapping(value = "/jobs/{jobId}/result", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProcessingResultResponse> getJobResult(@PathVariable UUID jobId) {
        logger.debug("Job result request for jobId={}", jobId);

        ProcessingResultResponse result = asyncJobService.getJobResult(jobId);

        logger.info("Job result retrieved: jobId={}, products={}, records={}",
            jobId, result.results().size(), result.metadata().totalRecords());

        return ResponseEntity.ok(result);
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