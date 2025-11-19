package com.wtw.claims.api.scheduler;

import com.wtw.claims.api.model.JobRecord;
import com.wtw.claims.api.service.AsyncJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Scheduled task to clean up old completed and failed jobs.
 *
 * <p>This scheduler prevents memory exhaustion by periodically removing jobs that:
 * <ul>
 *   <li>Have completed (successfully or with failure)</li>
 *   <li>Are older than the configured retention period</li>
 * </ul>
 *
 * <p>Configuration:
 * <ul>
 *   <li>cleanup-interval-minutes: How often to run cleanup (default: 60 minutes)</li>
 *   <li>job-retention-hours: How long to keep completed jobs (default: 24 hours)</li>
 * </ul>
 */
@Component
@EnableScheduling
public class JobCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobCleanupScheduler.class);

    private final AsyncJobService asyncJobService;

    @Value("${claims.processing.async.job-retention-hours:24}")
    private int jobRetentionHours;

    public JobCleanupScheduler(AsyncJobService asyncJobService) {
        this.asyncJobService = asyncJobService;
    }

    /**
     * Cleans up old completed and failed jobs.
     *
     * <p>Runs on a fixed delay (default: every 60 minutes).
     * The delay starts after the previous execution completes.
     *
     * <p>Jobs are removed if they:
     * <ul>
     *   <li>Have status COMPLETED or FAILED</li>
     *   <li>Were last updated more than job-retention-hours ago</li>
     * </ul>
     */
    @Scheduled(fixedDelayString = "${claims.processing.async.cleanup-interval-minutes:60}",
               timeUnit = java.util.concurrent.TimeUnit.MINUTES,
               initialDelayString = "${claims.processing.async.cleanup-initial-delay-minutes:10}")
    public void cleanupStaleJobs() {
        Instant cutoff = Instant.now().minus(jobRetentionHours, ChronoUnit.HOURS);

        log.info("Starting job cleanup: removing jobs older than {} (retention: {} hours)",
            cutoff, jobRetentionHours);

        int removedCount = 0;
        int totalJobs = 0;

        try {
            Map<UUID, JobRecord> allJobs = asyncJobService.getAllJobs();
            totalJobs = allJobs.size();

            for (Map.Entry<UUID, JobRecord> entry : allJobs.entrySet()) {
                JobRecord job = entry.getValue();

                // Only remove finished jobs that are old enough
                if (job.isFinished() && job.getUpdatedAt().isBefore(cutoff)) {
                    boolean removed = asyncJobService.removeJob(entry.getKey());
                    if (removed) {
                        removedCount++;
                        log.debug("Removed stale job: {} (status: {}, updated: {})",
                            entry.getKey(), job.getStatus(), job.getUpdatedAt());
                    }
                }
            }

            log.info("Job cleanup completed: removed {} of {} total jobs", removedCount, totalJobs);

            // Log warning if job count is high
            int remainingJobs = totalJobs - removedCount;
            if (remainingJobs > 80) {
                log.warn("High number of active jobs: {} jobs remaining (threshold: 100)",
                    remainingJobs);
            }

        } catch (Exception e) {
            log.error("Error during job cleanup: {}", e.getMessage(), e);
        }
    }
}
