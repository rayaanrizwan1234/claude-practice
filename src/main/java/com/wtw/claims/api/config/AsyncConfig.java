package com.wtw.claims.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous task execution in claims processing.
 *
 * <p>This configuration provides:
 * <ul>
 *   <li>Thread pool executor for async claims processing jobs</li>
 *   <li>Configurable pool sizes and queue capacity</li>
 *   <li>Graceful shutdown handling</li>
 *   <li>Exception handling for uncaught async exceptions</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${claims.processing.async.max-concurrent-jobs:10}")
    private int maxConcurrentJobs;

    /**
     * Creates the thread pool executor for claims processing.
     */
    @Bean(name = "claimsTaskExecutor")
    public Executor claimsTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core threads always alive (half of max for efficiency)
        executor.setCorePoolSize(Math.max(1, maxConcurrentJobs / 2));

        // Maximum threads allowed (from configuration)
        executor.setMaxPoolSize(maxConcurrentJobs);

        // Queue capacity for pending jobs (2x max provides burst capacity)
        executor.setQueueCapacity(maxConcurrentJobs * 2);

        // Thread naming for easier debugging and monitoring
        executor.setThreadNamePrefix("claims-async-");

        // Rejection policy: run in caller thread (provides natural backpressure)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Graceful shutdown: wait for tasks to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Allow core threads to timeout when idle (conserve resources)
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        executor.initialize();

        log.info("Initialized claims task executor: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * Provides exception handler for uncaught exceptions in async methods.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            private static final Logger asyncLog = LoggerFactory.getLogger("AsyncExceptionHandler");

            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                asyncLog.error("Uncaught exception in async method '{}': {}",
                    method.getName(), ex.getMessage(), ex);
            }
        };
    }
}
