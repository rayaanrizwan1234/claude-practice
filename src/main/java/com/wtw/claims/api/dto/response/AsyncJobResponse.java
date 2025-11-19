package com.wtw.claims.api.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned when an asynchronous claims processing job is submitted.
 *
 * <p>This record provides the client with:
 * <ul>
 *   <li>A unique job identifier for tracking</li>
 *   <li>The initial job status (typically QUEUED)</li>
 *   <li>The timestamp when the job was created</li>
 *   <li>A URL endpoint to poll for job status updates</li>
 * </ul>
 *
 * @param jobId Unique identifier for the submitted job
 * @param status Current status of the job (QUEUED, PROCESSING, COMPLETED, FAILED)
 * @param createdAt Timestamp when the job was created
 * @param statusUrl URL endpoint to poll for job status (e.g., /api/v1/claims/jobs/{jobId})
 */
public record AsyncJobResponse(
    UUID jobId,
    String status,
    Instant createdAt,
    String statusUrl
) {
}
