package com.wtw.claims.api.model;

/**
 * Represents the lifecycle states of an asynchronous claims processing job.
 *
 * <p>State transitions:
 * <ul>
 *   <li>QUEUED → PROCESSING (when job execution starts)</li>
 *   <li>PROCESSING → COMPLETED (when processing succeeds)</li>
 *   <li>PROCESSING → FAILED (when processing encounters an error)</li>
 * </ul>
 */
public enum JobStatus {
    /**
     * Job has been submitted and is waiting in the queue for processing.
     */
    QUEUED,

    /**
     * Job is currently being processed by a worker thread.
     */
    PROCESSING,

    /**
     * Job has completed successfully and results are available.
     */
    COMPLETED,

    /**
     * Job encountered an error during processing and cannot complete.
     */
    FAILED
}
