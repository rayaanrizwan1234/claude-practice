package com.wtw.claims.api.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Internal representation of an asynchronous claims processing job.
 *
 * <p>This class tracks the complete lifecycle of a job, including:
 * <ul>
 *   <li>Job metadata (ID, status, timestamps)</li>
 *   <li>Buffered CSV data for asynchronous processing</li>
 *   <li>Error information if processing failed</li>
 * </ul>
 *
 * <p>Thread Safety: Instances should be stored in ConcurrentHashMap and updated
 * atomically using compute operations.
 */
public class JobRecord {
    private final UUID jobId;
    private volatile JobStatus status;
    private volatile byte[] csvData;  // Mutable to allow clearing after processing
    private final Instant createdAt;
    private volatile Instant startedAt;
    private volatile Instant updatedAt;
    private volatile Instant completedAt;
    private volatile String errorMessage;

    /**
     * Creates a new job record in QUEUED status.
     *
     * @param jobId Unique identifier for the job
     * @param csvData Buffered CSV file content for processing
     */
    public JobRecord(UUID jobId, byte[] csvData) {
        this.jobId = jobId;
        this.csvData = csvData;
        this.status = JobStatus.QUEUED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the job to PROCESSING status and records the start time.
     */
    public void markAsProcessing() {
        this.status = JobStatus.PROCESSING;
        this.startedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the job to COMPLETED status and records the completion time.
     */
    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the job to FAILED status with an error message.
     *
     * @param errorMessage Description of the failure
     */
    public void markAsFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Clears the CSV data from memory to free resources after processing.
     *
     * <p>This should be called after the job completes (successfully or with failure)
     * to prevent large byte arrays from consuming memory unnecessarily.
     */
    public void clearCsvData() {
        this.csvData = null;
    }

    // Getters
    public UUID getJobId() {
        return jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public byte[] getCsvData() {
        return csvData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Checks if the job has completed (either successfully or with failure).
     *
     * @return true if status is COMPLETED or FAILED
     */
    public boolean isFinished() {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }
}
