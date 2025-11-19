package com.wtw.claims.api.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting to retrieve results for a job that has not completed.
 *
 * <p>This occurs when a client requests results via GET /jobs/{id}/result but the job is still:
 * <ul>
 *   <li>QUEUED - waiting to be processed</li>
 *   <li>PROCESSING - currently being processed</li>
 *   <li>FAILED - completed with errors (results unavailable)</li>
 * </ul>
 *
 * <p>This exception is mapped to HTTP 400 Bad Request by the global exception handler.
 */
public class JobNotCompletedException extends RuntimeException {

    private final UUID jobId;
    private final String currentStatus;

    /**
     * Creates a new JobNotCompletedException.
     *
     * @param jobId The job ID that is not yet complete
     * @param currentStatus The current status of the job (QUEUED, PROCESSING, or FAILED)
     */
    public JobNotCompletedException(UUID jobId, String currentStatus) {
        super(String.format("Job %s is not completed. Current status: %s", jobId, currentStatus));
        this.jobId = jobId;
        this.currentStatus = currentStatus;
    }

    /**
     * Returns the job ID that is not yet complete.
     *
     * @return The job ID
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * Returns the current status of the incomplete job.
     *
     * @return The current job status
     */
    public String getCurrentStatus() {
        return currentStatus;
    }
}
