package com.wtw.claims.api.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting to access a job that does not exist.
 *
 * <p>This occurs when:
 * <ul>
 *   <li>A client queries a job ID that was never created</li>
 *   <li>A job has been cleaned up after its retention period expired</li>
 *   <li>An invalid or malformed job ID is provided</li>
 * </ul>
 *
 * <p>This exception is mapped to HTTP 404 Not Found by the global exception handler.
 */
public class JobNotFoundException extends RuntimeException {

    private final UUID jobId;

    /**
     * Creates a new JobNotFoundException.
     *
     * @param jobId The job ID that was not found
     */
    public JobNotFoundException(UUID jobId) {
        super(String.format("Job with ID %s not found", jobId));
        this.jobId = jobId;
    }

    /**
     * Returns the job ID that was not found.
     *
     * @return The missing job ID
     */
    public UUID getJobId() {
        return jobId;
    }
}
