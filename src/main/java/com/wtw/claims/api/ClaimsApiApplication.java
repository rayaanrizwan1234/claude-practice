package com.wtw.claims.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Spring Boot application entry point for the Claims Triangle Accumulator REST API.
 *
 * <p>This class serves as the main entry point for the web API version of the application.
 * The original CLI entry point ({@link com.wtw.claims.ClaimsApplication}) remains unchanged
 * and can still be used for command-line processing.</p>
 *
 * <p><strong>Running the API:</strong></p>
 * <pre>
 * mvn spring-boot:run
 * </pre>
 *
 * <p><strong>Running the CLI (unchanged):</strong></p>
 * <pre>
 * mvn exec:java -Dexec.args="input.csv output.csv"
 * </pre>
 *
 * @author Claims Processing Team
 * @version 2.0.0
 */
@SpringBootApplication(scanBasePackages = "com.wtw.claims")
@EnableAsync
public class ClaimsApiApplication {

    /**
     * Main entry point for the Spring Boot application.
     *
     * @param args command line arguments (typically none for web applications)
     */
    public static void main(String[] args) {
        SpringApplication.run(ClaimsApiApplication.class, args);
    }
}