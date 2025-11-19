package com.wtw.claims.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned when querying the status of an asynchronous claims processing job.
 *
 * <p>This record provides clients with real-time information about job progress:
 * <ul>
 *   <li>Current job status (QUEUED, PROCESSING, COMPLETED, FAILED)</li>
 *   <li>Lifecycle timestamps for tracking job duration</li>
 *   <li>Error message if the job failed</li>
 * </ul>
 *
 * @param jobId Unique identifier for the job
 * @param status Current status (QUEUED, PROCESSING, COMPLETED, FAILED)
 * @param createdAt Timestamp when the job was submitted
 * @param startedAt Timestamp when processing began (null if still QUEUED)
 * @param updatedAt Timestamp of the last status update
 * @param completedAt Timestamp when processing finished (null if not yet complete)
 * @param errorMessage Error message if status is FAILED (null otherwise)
 */
public record JobStatusResponse(
    UUID jobId,
    String status,
    Instant createdAt,
    Instant startedAt,
    Instant updatedAt,
    Instant completedAt,
    String errorMessage
) {
}
