package com.wtw.claims.api.service;

import com.wtw.claims.api.dto.response.AsyncJobResponse;
import com.wtw.claims.api.dto.response.JobStatusResponse;
import com.wtw.claims.api.dto.response.ProcessingResultResponse;
import com.wtw.claims.api.exception.JobNotCompletedException;
import com.wtw.claims.api.exception.JobNotFoundException;
import com.wtw.claims.api.model.JobRecord;
import com.wtw.claims.api.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Service for managing asynchronous claims processing jobs.
 *
 * <p>This service provides:
 * <ul>
 *   <li>Job submission and queuing with size limits</li>
 *   <li>Asynchronous job processing using CompletableFuture</li>
 *   <li>Job status tracking with thread-safe operations</li>
 *   <li>Result storage and retrieval</li>
 * </ul>
 *
 * <p>Thread Safety: Uses ConcurrentHashMap for thread-safe storage and atomic
 * compute operations for state transitions.
 *
 * <p>Memory Management: Enforces maximum job limit and clears CSV data after
 * processing to prevent memory exhaustion.
 */
@Service
public class AsyncJobService {
    private static final Logger log = LoggerFactory.getLogger(AsyncJobService.class);

    // Maximum number of concurrent jobs allowed (prevents memory exhaustion)
    private static final int MAX_JOBS = 100;

    // Maximum CSV file size (50MB as per spec)
    private static final int MAX_CSV_SIZE = 50 * 1024 * 1024;

    private final ClaimsProcessingService processingService;
    private final Executor taskExecutor;
    private final ConcurrentHashMap<UUID, JobRecord> jobStore;
    private final ConcurrentHashMap<UUID, ProcessingResultResponse> resultStore;

    public AsyncJobService(ClaimsProcessingService processingService,
                          @Qualifier("claimsTaskExecutor") Executor taskExecutor) {
        this.processingService = processingService;
        this.taskExecutor = taskExecutor;
        this.jobStore = new ConcurrentHashMap<>();
        this.resultStore = new ConcurrentHashMap<>();
    }

    /**
     * Submits a new asynchronous claims processing job.
     *
     * <p>The CSV data is buffered in memory and processing begins asynchronously.
     * The method returns immediately with a job ID for status tracking.
     *
     * @param csvData Buffered CSV file content
     * @return Job ID for tracking the processing
     * @throws IllegalArgumentException if csvData is null, empty, or exceeds size limit
     * @throws IllegalStateException if maximum concurrent jobs limit is reached
     */
    public UUID submitJob(byte[] csvData) {
        // Input validation
        if (csvData == null) {
            throw new IllegalArgumentException("CSV data cannot be null");
        }
        if (csvData.length == 0) {
            throw new IllegalArgumentException("CSV data cannot be empty");
        }
        if (csvData.length > MAX_CSV_SIZE) {
            throw new IllegalArgumentException(
                String.format("CSV data size %d bytes exceeds maximum %d bytes",
                    csvData.length, MAX_CSV_SIZE));
        }

        // Check job limit to prevent memory exhaustion
        int currentJobs = jobStore.size();
        if (currentJobs >= MAX_JOBS) {
            throw new IllegalStateException(
                String.format("Maximum concurrent jobs limit reached (%d). Please try again later.", MAX_JOBS)
            );
        }

        UUID jobId = UUID.randomUUID();
        JobRecord record = new JobRecord(jobId, csvData);

        jobStore.put(jobId, record);

        log.info("Job {} submitted: {} bytes", jobId, csvData.length);

        // Submit for async processing using CompletableFuture
        // This avoids the self-invocation problem with @Async
        CompletableFuture.runAsync(() -> processJob(jobId), taskExecutor);

        return jobId;
    }

    /**
     * Processes a job asynchronously in a background thread.
     *
     * <p>This method:
     * <ol>
     *   <li>Updates job status to PROCESSING atomically</li>
     *   <li>Invokes the claims processing service</li>
     *   <li>Stores the result and updates status to COMPLETED</li>
     *   <li>Clears CSV data to free memory</li>
     *   <li>On error, stores error message and marks as FAILED</li>
     * </ol>
     *
     * @param jobId The job to process
     */
    private void processJob(UUID jobId) {
        if (jobId == null) {
            log.error("Job ID cannot be null");
            return;
        }

        // Atomically get and mark as processing
        JobRecord jobRecord = jobStore.computeIfPresent(jobId, (id, record) -> {
            record.markAsProcessing();
            return record;
        });

        if (jobRecord == null) {
            log.error("Job record not found for ID: {}. May have been removed by cleanup.", jobId);
            return;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(jobRecord.getCsvData())) {
            log.info("Starting processing for job {}", jobId);

            ProcessingResultResponse result = processingService.processClaims(inputStream);

            // Atomically store result and update status to completed
            jobStore.computeIfPresent(jobId, (id, record) -> {
                resultStore.put(jobId, result);
                record.markAsCompleted();
                record.clearCsvData(); // Free memory
                return record;
            });

            log.info("Job {} completed successfully in {} ms",
                jobId, result.metadata().processingTimeMs());

        } catch (Exception e) {
            String errorMsg = "Processing failed: " +
                (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());

            log.error("Job {} failed: {}", jobId, errorMsg, e);

            // Atomically update status to failed and clear CSV data
            jobStore.computeIfPresent(jobId, (id, record) -> {
                resultStore.remove(jobId); // Remove any partial results
                record.markAsFailed(errorMsg);
                record.clearCsvData(); // Free memory even on failure
                return record;
            });
        }
    }

    /**
     * Retrieves the current status of a job.
     *
     * @param jobId The job ID to query (must not be null)
     * @return Response containing current job status and timestamps
     * @throws IllegalArgumentException if jobId is null
     * @throws JobNotFoundException if the job ID does not exist
     */
    public JobStatusResponse getJobStatus(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("Job ID cannot be null");
        }

        JobRecord job = jobStore.get(jobId);

        if (job == null) {
            throw new JobNotFoundException(jobId);
        }

        return new JobStatusResponse(
            job.getJobId(),
            job.getStatus().name(),
            job.getCreatedAt(),
            job.getStartedAt(),
            job.getUpdatedAt(),
            job.getCompletedAt(),
            job.getErrorMessage()
        );
    }

    /**
     * Retrieves the processing result for a completed job.
     *
     * @param jobId The job ID to retrieve results for (must not be null)
     * @return The processing result
     * @throws IllegalArgumentException if jobId is null
     * @throws JobNotFoundException if the job ID does not exist
     * @throws JobNotCompletedException if the job has not completed successfully
     */
    public ProcessingResultResponse getJobResult(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("Job ID cannot be null");
        }

        JobRecord job = jobStore.get(jobId);

        if (job == null) {
            throw new JobNotFoundException(jobId);
        }

        if (job.getStatus() != JobStatus.COMPLETED) {
            throw new JobNotCompletedException(jobId, job.getStatus().name());
        }

        ProcessingResultResponse result = resultStore.get(jobId);

        if (result == null) {
            // Defensive check - this shouldn't happen but handle gracefully
            log.error("Job {} is marked COMPLETED but result not found in store", jobId);
            throw new IllegalStateException("Result not found for completed job " + jobId);
        }

        return result;
    }

    /**
     * Removes a job from storage (used by cleanup scheduler).
     *
     * @param jobId The job ID to remove (must not be null)
     * @return true if the job was removed, false if it didn't exist
     * @throws IllegalArgumentException if jobId is null
     */
    public boolean removeJob(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("Job ID cannot be null");
        }

        JobRecord removed = jobStore.remove(jobId);
        resultStore.remove(jobId);

        if (removed != null) {
            log.debug("Removed job {} (status: {})", jobId, removed.getStatus());
            return true;
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of all jobs (used by cleanup scheduler).
     *
     * <p>The returned map is a snapshot and modifications will not affect
     * the internal job store.
     *
     * @return Unmodifiable map of all jobs
     */
    public Map<UUID, JobRecord> getAllJobs() {
        return Collections.unmodifiableMap(jobStore);
    }
}
