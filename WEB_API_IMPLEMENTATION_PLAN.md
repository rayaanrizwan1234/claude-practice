# Claims Triangle Accumulator - Web API Implementation Plan

## Executive Summary

This document outlines a comprehensive plan for adding a REST API to the Claims Triangle Accumulator project. The API will enable HTTP-based claims processing while preserving existing CLI functionality.

**Estimated Total Effort:** 25-36 hours
**Recommended Framework:** Spring Boot 3.2.x
**Target Java Version:** 21 (already configured)

---

## Table of Contents

1. [Framework Selection](#1-framework-selection)
2. [API Design](#2-api-design)
3. [Architecture & Integration](#3-architecture--integration)
4. [New Components](#4-new-components)
5. [Implementation Phases](#5-implementation-phases)
6. [AI Agent Workflow](#6-ai-agent-workflow)
7. [Testing Strategy](#7-testing-strategy)
8. [Security Considerations](#8-security-considerations)
9. [Deployment & Containerization](#9-deployment--containerization)
10. [Configuration Changes](#10-configuration-changes)
11. [Risk Assessment](#11-risk-assessment)
12. [Success Criteria](#12-success-criteria)

---

## 1. Framework Selection

### Primary Recommendation: Spring Boot 3.2.x

**Rationale:**
- **Ecosystem Maturity**: Most comprehensive ecosystem with extensive documentation
- **Java 21 Support**: Full compatibility with virtual threads and modern features
- **Testing Support**: Excellent utilities (MockMvc, TestRestTemplate)
- **Enterprise Features**: Built-in security, actuator for health checks, validation
- **Community**: Largest community and long-term support
- **Learning Curve**: Widely adopted, well-documented

**Alternative Considered: Quarkus**
- Faster startup time (~300ms vs Spring Boot's ~2s)
- Lower memory footprint
- Better for cloud-native/Kubernetes deployments
- Smaller ecosystem and documentation

**Decision**: Spring Boot for maturity and testing support; reconsider Quarkus if deployment becomes a priority.

---

## 2. API Design

### Base URL
```
/api/v1/claims
```

### Core Endpoints

#### 2.1 Synchronous Processing
```http
POST /api/v1/claims/process
Content-Type: multipart/form-data

Request:
  - file: CSV file upload (max 10MB for sync)

Response (200 OK):
{
  "metadata": {
    "earliestOriginYear": 1990,
    "numberOfDevelopmentYears": 4,
    "totalRecords": 12,
    "productsProcessed": ["Comp", "Non-Comp"],
    "processingTimeMs": 145
  },
  "results": [
    {
      "product": "Comp",
      "cumulativeValues": [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 110.0, 280.0, 200.0]
    },
    {
      "product": "Non-Comp",
      "cumulativeValues": [45.2, 110.0, 110.0, 147.0, 50.0, 125.0, 150.0, 55.0, 140.0, 100.0]
    }
  ],
  "csvOutput": "1990,4\nComp,0.0,0.0,...\nNon-Comp,45.2,..."
}
```

#### 2.2 Asynchronous Processing (Large Files)
```http
POST /api/v1/claims/process/async
Content-Type: multipart/form-data

Request:
  - file: Large CSV file upload (max 50MB)

Response (202 Accepted):
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "QUEUED",
  "createdAt": "2025-11-16T10:30:00Z",
  "statusUrl": "/api/v1/claims/jobs/550e8400-e29b-41d4-a716-446655440000"
}
```

#### 2.3 Job Status
```http
GET /api/v1/claims/jobs/{jobId}

Response (200 OK):
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",  // QUEUED | PROCESSING | COMPLETED | FAILED
  "progress": 75,
  "recordsProcessed": 7500,
  "estimatedRecordsTotal": 10000,
  "startedAt": "2025-11-16T10:30:05Z",
  "updatedAt": "2025-11-16T10:30:45Z"
}
```

#### 2.4 Job Result
```http
GET /api/v1/claims/jobs/{jobId}/result

Response (200 OK):
  Same structure as synchronous response

Response (400 Bad Request - Job Not Complete):
{
  "error": "JOB_NOT_COMPLETED",
  "message": "Job is still processing",
  "currentStatus": "PROCESSING"
}
```

#### 2.5 Health Check Endpoints
```http
GET /api/v1/health          - Overall health
GET /api/v1/health/ready    - Readiness probe (for K8s)
GET /api/v1/health/live     - Liveness probe (for K8s)
```

### Error Response Format
```json
{
  "timestamp": "2025-11-16T10:35:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Invalid CSV format at line 5: Development year cannot be before origin year",
  "path": "/api/v1/claims/process",
  "details": {
    "line": 5,
    "column": "Development Year",
    "value": "1989",
    "constraint": "developmentYear >= originYear"
  }
}
```

### HTTP Status Codes
- `200 OK` - Successful synchronous processing
- `202 Accepted` - Async job queued
- `400 Bad Request` - Invalid input (CSV format, validation errors)
- `404 Not Found` - Job ID not found
- `413 Payload Too Large` - File exceeds size limit
- `429 Too Many Requests` - Rate limit exceeded (future)
- `500 Internal Server Error` - Unexpected server error

---

## 3. Architecture & Integration

### Current Architecture (Preserved)
```
CSV File → ClaimsReader → DataProcessor → TriangleBuilder → TriangleAccumulator → ClaimsWriter → CSV File
```

### New Web API Layer
```
HTTP Request (MultipartFile)
        ↓
  ClaimsController (REST endpoints)
        ↓
  ClaimsProcessingService (orchestration)
        ↓
  [Existing Business Logic - REUSED]
        ↓
  ProcessingResultResponse (JSON)
        ↓
HTTP Response
```

### Components to Reuse AS-IS (No Modifications)

| Component | Package | Usage in Web API |
|-----------|---------|------------------|
| `ClaimRecord` | `model/` | Core data model (Java record) |
| `ClaimsTriangle` | `model/` | Triangle data structure |
| `TriangleAccumulator` | `processor/` | Core business logic |
| `TriangleBuilder` | `processor/` | Triangle construction |
| `DataProcessor` | `processor/` | Utility methods |
| `ClaimRecordValidator` | `validator/` | Input validation |
| `ClaimsReader.YearRange` | `reader/` | Year range metadata |

### Components Requiring Modification

#### ClaimsReader.java - Add InputStream Support

```java
// EXISTING (keep as-is)
public List<ClaimRecord> readClaims(Path filePath) throws IOException { ... }
public void streamClaims(Path filePath, Consumer<ClaimRecord> consumer) throws IOException { ... }
public YearRange scanForYearRange(Path filePath) throws IOException { ... }

// NEW OVERLOADS TO ADD
public List<ClaimRecord> readClaims(InputStream inputStream) throws IOException {
    // Wrap InputStream in BufferedReader, reuse CSV parsing logic
}

public void streamClaims(InputStream inputStream, Consumer<ClaimRecord> consumer) throws IOException {
    // Stream from InputStream instead of file
}

public YearRange scanForYearRange(InputStream inputStream) throws IOException {
    // Scan InputStream for year range
}

// Optional: For testing flexibility
public List<ClaimRecord> readClaims(String csvContent) throws IOException {
    return readClaims(new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8)));
}
```

#### ClaimsWriter.java - Add String Output Support

```java
// EXISTING (keep as-is)
public void writeCumulativeClaims(Path outputPath, ...) throws IOException { ... }

// NEW METHODS TO ADD
public String formatCumulativeClaims(
    Map<String, ClaimsTriangle> triangles,
    int earliestOriginYear,
    int numberOfDevelopmentYears
) {
    // Return formatted CSV as string instead of writing to file
    StringBuilder output = new StringBuilder();
    output.append(formatHeader(earliestOriginYear, numberOfDevelopmentYears));
    // ... format each product
    return output.toString();
}

public String formatHeader(int earliestOriginYear, int numberOfDevelopmentYears) {
    return String.format("%d, %d%n", earliestOriginYear, numberOfDevelopmentYears);
}

public String formatProductLine(String productName, ClaimsTriangle triangle) {
    // Format single product line
}

public List<Double> getCumulativeValuesAsList(ClaimsTriangle triangle) {
    // Return flattened cumulative values as list (for JSON response)
    return triangle.flattenCumulative();
}
```

---

## 4. New Components

### Package Structure

```
com.wtw.claims/
├── ClaimsApplication.java              (EXISTING - CLI entry point, unchanged)
├── api/                                 (NEW - Web API package)
│   ├── ClaimsApiApplication.java       (Spring Boot main class)
│   ├── controller/
│   │   └── ClaimsController.java       (REST endpoints)
│   ├── service/
│   │   ├── ClaimsProcessingService.java
│   │   └── AsyncJobService.java
│   ├── dto/
│   │   ├── request/
│   │   │   └── ProcessingOptions.java  (future: options like precision)
│   │   └── response/
│   │       ├── ProcessingResultResponse.java
│   │       ├── ProductResult.java
│   │       ├── ProcessingMetadata.java
│   │       ├── AsyncJobResponse.java
│   │       ├── JobStatusResponse.java
│   │       └── ErrorResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InvalidCsvException.java
│   │   ├── JobNotFoundException.java
│   │   └── ProcessingException.java
│   └── config/
│       ├── WebConfig.java
│       ├── AsyncConfig.java
│       └── SecurityConfig.java
├── model/                               (EXISTING)
├── reader/                              (EXISTING - add InputStream methods)
├── processor/                           (EXISTING)
├── writer/                              (EXISTING - add string methods)
└── validator/                           (EXISTING)
```

### Key Class Implementations

#### ClaimsApiApplication.java
```java
package com.wtw.claims.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "com.wtw.claims")
@EnableAsync
public class ClaimsApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClaimsApiApplication.class, args);
    }
}
```

#### ClaimsController.java
```java
package com.wtw.claims.api.controller;

@RestController
@RequestMapping("/api/v1/claims")
@Validated
@Slf4j
public class ClaimsController {

    private final ClaimsProcessingService processingService;
    private final AsyncJobService asyncJobService;

    public ClaimsController(ClaimsProcessingService processingService,
                           AsyncJobService asyncJobService) {
        this.processingService = processingService;
        this.asyncJobService = asyncJobService;
    }

    @PostMapping("/process")
    public ResponseEntity<ProcessingResultResponse> processSync(
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Received sync processing request for file: {}", file.getOriginalFilename());

        ProcessingResultResponse result = processingService.processClaims(file.getInputStream());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/process/async")
    public ResponseEntity<AsyncJobResponse> processAsync(
            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Received async processing request for file: {}", file.getOriginalFilename());

        AsyncJobResponse job = asyncJobService.submitJob(file.getInputStream());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        JobStatusResponse status = asyncJobService.getJobStatus(jobId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/jobs/{jobId}/result")
    public ResponseEntity<ProcessingResultResponse> getJobResult(@PathVariable UUID jobId) {
        ProcessingResultResponse result = asyncJobService.getJobResult(jobId);
        return ResponseEntity.ok(result);
    }
}
```

#### ClaimsProcessingService.java
```java
package com.wtw.claims.api.service;

@Service
@Slf4j
public class ClaimsProcessingService {

    private final ClaimsReader reader;
    private final ClaimsWriter writer;

    public ClaimsProcessingService() {
        this.reader = new ClaimsReader();
        this.writer = new ClaimsWriter();
    }

    public ProcessingResultResponse processClaims(InputStream csvInputStream) throws IOException {
        long startTime = System.currentTimeMillis();

        // Step 1: Buffer the input stream (need to read twice: scan + process)
        byte[] csvData = csvInputStream.readAllBytes();

        // Step 2: Scan for year range
        ClaimsReader.YearRange yearRange = reader.scanForYearRange(
            new ByteArrayInputStream(csvData)
        );
        log.info("Year range: {} to {}", yearRange.minOriginYear(), yearRange.maxDevYear());

        // Step 3: Stream and build triangles
        Map<String, ClaimsTriangle> triangles = new LinkedHashMap<>();
        AtomicInteger recordCount = new AtomicInteger();

        reader.streamClaims(new ByteArrayInputStream(csvData), record -> {
            ClaimsTriangle triangle = triangles.computeIfAbsent(
                record.product(),
                product -> new ClaimsTriangle(product, yearRange.minOriginYear(), yearRange.maxDevYear())
            );
            triangle.addIncrementalValue(
                record.originYear(),
                record.developmentYear(),
                record.incrementalValue()
            );
            recordCount.incrementAndGet();
        });

        // Step 4: Calculate cumulative values
        triangles.values().forEach(TriangleAccumulator::calculateCumulative);

        // Step 5: Build response
        long processingTime = System.currentTimeMillis() - startTime;

        return buildResponse(triangles, yearRange, recordCount.get(), processingTime);
    }

    private ProcessingResultResponse buildResponse(
            Map<String, ClaimsTriangle> triangles,
            ClaimsReader.YearRange yearRange,
            int totalRecords,
            long processingTimeMs) {

        List<ProductResult> results = triangles.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new ProductResult(
                entry.getKey(),
                entry.getValue().flattenCumulative()
            ))
            .toList();

        ProcessingMetadata metadata = new ProcessingMetadata(
            yearRange.minOriginYear(),
            yearRange.getNumberOfDevelopmentYears(),
            totalRecords,
            results.stream().map(ProductResult::product).toList(),
            processingTimeMs
        );

        String csvOutput = writer.formatCumulativeClaims(
            triangles,
            yearRange.minOriginYear(),
            yearRange.getNumberOfDevelopmentYears()
        );

        return new ProcessingResultResponse(metadata, results, csvOutput);
    }
}
```

#### DTOs (Java Records)
```java
// ProcessingResultResponse.java
public record ProcessingResultResponse(
    ProcessingMetadata metadata,
    List<ProductResult> results,
    String csvOutput
) {}

// ProcessingMetadata.java
public record ProcessingMetadata(
    int earliestOriginYear,
    int numberOfDevelopmentYears,
    int totalRecords,
    List<String> productsProcessed,
    long processingTimeMs
) {}

// ProductResult.java
public record ProductResult(
    String product,
    List<Double> cumulativeValues
) {}

// AsyncJobResponse.java
public record AsyncJobResponse(
    UUID jobId,
    String status,
    Instant createdAt,
    String statusUrl
) {}

// JobStatusResponse.java
public record JobStatusResponse(
    UUID jobId,
    String status,
    Integer progress,
    Integer recordsProcessed,
    Integer estimatedRecordsTotal,
    Instant startedAt,
    Instant updatedAt
) {}

// ErrorResponse.java
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, Object> details
) {}
```

#### GlobalExceptionHandler.java
```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.error("Validation error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            ex.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "FILE_TOO_LARGE",
            "File size exceeds maximum allowed limit",
            request.getRequestURI(),
            Map.of("maxSize", "50MB")
        );

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(error);
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobNotFound(
            JobNotFoundException ex, HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.NOT_FOUND.value(),
            "JOB_NOT_FOUND",
            ex.getMessage(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error", ex);

        ErrorResponse error = new ErrorResponse(
            Instant.now(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## 5. Git Best Practices

### Commit Frequency
- **Commit after each phase completion** - Every phase should result in a meaningful commit
- **Push immediately after commit** - Keep remote repository synchronized
- **Atomic commits** - Each commit should represent a logical, complete unit of work
- **Descriptive commit messages** - Use conventional commit format with clear descriptions

### Branch Strategy
- Work on a feature/development branch (e.g., `development` or `feature/web-api`)
- Never work directly on `main` branch
- Push regularly to ensure backup and enable collaboration

### Commit Message Format
```
Phase N: <Brief description>

- Bullet point of major changes
- Another change
- Test results (e.g., "All 446 tests pass")

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Required Git Actions Per Phase
1. **Before starting:** `git status` to ensure clean state
2. **After completing phase:**
   - `git add <changed files>`
   - `git commit -m "<descriptive message>"`
   - `git push`
3. **Verify:** `git status` to confirm push succeeded

---

## 6. Implementation Phases

### Phase 1: Foundation (4-6 hours)

**Objective:** Set up Spring Boot infrastructure and adapt I/O layers

**Tasks:**
1. Update `pom.xml`:
   - Add Spring Boot parent POM
   - Add spring-boot-starter-web
   - Add spring-boot-starter-validation
   - Add spring-boot-starter-actuator
   - Update slf4j dependencies (Spring Boot uses Logback)

2. Create `ClaimsApiApplication.java`

3. Create `application.yml`:
   ```yaml
   server:
     port: 8080
   spring:
     servlet:
       multipart:
         max-file-size: 50MB
         max-request-size: 50MB
   ```

4. Modify `ClaimsReader`:
   - Add `readClaims(InputStream)` method
   - Add `streamClaims(InputStream, Consumer)` method
   - Add `scanForYearRange(InputStream)` method

5. Modify `ClaimsWriter`:
   - Add `formatCumulativeClaims()` returning String
   - Add helper methods for formatting

6. Test that CLI functionality still works (all 446 tests pass)

7. **🤖 AI Agent Workflow:**
   - Invoke **test-generator agent** for new Reader/Writer methods
   - Invoke **pr-code-reviewer agent** for all modified files
   - Address all critical and important issues

**Deliverables:**
- Spring Boot application starts successfully
- Existing tests pass
- Reader/Writer have new overloads
- ✅ Test suite for new InputStream methods (generated by test-generator agent)
- ✅ Code review completed (by pr-code-reviewer agent)
- **🔀 Git: Commit and push all Phase 1 changes**
  ```bash
  git add .
  git commit -m "Phase 1: Add Spring Boot foundation and InputStream support"
  git push
  ```

---

### Phase 2: Core API (6-8 hours)

**Objective:** Implement synchronous processing endpoint

**Tasks:**
1. Create DTO classes:
   - `ProcessingResultResponse`
   - `ProductResult`
   - `ProcessingMetadata`
   - `ErrorResponse`

2. Create `ClaimsProcessingService`:
   - Orchestrate reading, processing, accumulation
   - Build response objects

3. Create `ClaimsController`:
   - `POST /api/v1/claims/process` endpoint
   - Handle MultipartFile uploads

4. Create `GlobalExceptionHandler`:
   - Handle validation errors
   - Handle file size errors
   - Handle unexpected errors

5. Write unit tests:
   - Service layer tests
   - Controller tests with MockMvc

6. **🤖 AI Agent Workflow:**
   - Invoke **test-generator agent** for:
     - ClaimsProcessingService
     - ClaimsController
     - All DTOs
     - GlobalExceptionHandler
   - Invoke **pr-code-reviewer agent** for:
     - Service implementation
     - Controller implementation
     - Exception handling
     - REST API design review

**Deliverables:**
- Synchronous endpoint working
- Proper error handling
- Unit test coverage >90%
- ✅ Comprehensive test suite (generated by test-generator agent)
- ✅ Code review completed with all critical issues resolved (by pr-code-reviewer agent)
- **🔀 Git: Commit and push all Phase 2 changes**
  ```bash
  git add .
  git commit -m "Phase 2: Implement synchronous REST API endpoint"
  git push
  ```

---

### Phase 3: Testing & Validation (4-6 hours)

**Objective:** Ensure API reliability through comprehensive testing

**Tasks:**
1. Integration tests with TestRestTemplate:
   - File upload success scenarios
   - Multiple products
   - Error scenarios (invalid CSV, malformed data)

2. Compare API output to CLI output:
   - Process same files
   - Verify identical results

3. Input validation testing:
   - File type validation
   - File size limits
   - Empty files
   - Malformed headers

4. Performance baseline:
   - Measure response time for various file sizes
   - Identify bottlenecks

5. **🤖 AI Agent Workflow:**
   - Invoke **test-generator agent** to enhance integration tests:
     - Additional edge cases
     - Performance tests
     - Concurrent request tests
   - Invoke **pr-code-reviewer agent** for:
     - Test quality and coverage
     - Test maintainability
     - Identify missing scenarios

**Deliverables:**
- Full integration test suite
- Validation that API matches CLI output
- Performance metrics baseline
- ✅ Enhanced test suite with edge cases (by test-generator agent)
- ✅ Test code review completed (by pr-code-reviewer agent)
- **🔀 Git: Commit and push all Phase 3 changes**
  ```bash
  git add .
  git commit -m "Phase 3: Add comprehensive integration tests and validation"
  git push
  ```

---

### Phase 4: Health & Monitoring (2-3 hours)

**Objective:** Add production readiness features

**Tasks:**
1. Configure Spring Boot Actuator:
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics
     endpoint:
       health:
         show-details: always
   ```

2. Add custom health indicators (optional):
   - Check available memory
   - Check disk space for temp files

3. Expose health endpoints:
   - `/actuator/health`
   - `/actuator/info`

4. Add application info:
   ```yaml
   info:
     app:
       name: Claims Triangle Accumulator API
       version: 2.0.0
       java-version: 21
   ```

5. **🤖 AI Agent Workflow:**
   - Invoke **test-generator agent** for:
     - Health check endpoint tests
     - Custom health indicator tests (if any)
     - Metrics validation tests
   - Invoke **pr-code-reviewer agent** for:
     - Actuator configuration review
     - Security of exposed endpoints
     - Production readiness assessment

**Deliverables:**
- Health check endpoints working
- Kubernetes-ready probes
- Application metrics available
- ✅ Health check tests (by test-generator agent)
- ✅ Configuration review completed (by pr-code-reviewer agent)
- **🔀 Git: Commit and push all Phase 4 changes**
  ```bash
  git add .
  git commit -m "Phase 4: Configure health monitoring and Actuator endpoints"
  git push
  ```

---

### Phase 5: Async Processing (4-6 hours)

**Objective:** Support large file processing without blocking

**Tasks:**
1. Create `AsyncJobService`:
   - Submit jobs to executor
   - Track job status (in-memory ConcurrentHashMap)
   - Store results

2. Configure async:
   ```java
   @Configuration
   @EnableAsync
   public class AsyncConfig {
       @Bean
       public Executor taskExecutor() {
           ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
           executor.setCorePoolSize(4);
           executor.setMaxPoolSize(10);
           executor.setQueueCapacity(50);
           executor.setThreadNamePrefix("claims-async-");
           return executor;
       }
   }
   ```

3. Add async endpoints:
   - `POST /api/v1/claims/process/async`
   - `GET /api/v1/claims/jobs/{jobId}`
   - `GET /api/v1/claims/jobs/{jobId}/result`

4. Implement job cleanup:
   - Scheduled task to remove old jobs
   - Configurable retention period

5. Test async workflow:
   - Job submission
   - Status polling
   - Result retrieval
   - Timeout handling

6. **🤖 AI Agent Workflow:**
   - Invoke **test-generator agent** for:
     - AsyncJobService (concurrency, thread safety, cleanup)
     - Async controller endpoints
     - Job lifecycle tests
     - Race condition scenarios
   - Invoke **pr-code-reviewer agent** for:
     - Thread safety of job storage
     - Memory leak prevention
     - Executor configuration
     - Error handling in async context

**Deliverables:**
- Async processing working
- Job tracking functional
- Cleanup mechanism in place
- ✅ Async service tests including concurrency scenarios (by test-generator agent)
- ✅ Thread safety and memory management review completed (by pr-code-reviewer agent)
- **🔀 Git: Commit and push all Phase 5 changes**
  ```bash
  git add .
  git commit -m "Phase 5: Implement async processing with job management"
  git push
  ```

---

### Phase 6: Security & Documentation (3-4 hours)

**Objective:** Harden API and document endpoints

**Tasks:**
1. Basic security configuration:
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           http
               .csrf(csrf -> csrf.disable())  // Stateless API
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/actuator/health/**").permitAll()
                   .requestMatchers("/api/v1/**").permitAll()
               )
               .sessionManagement(session ->
                   session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
               );
           return http.build();
       }
   }
   ```

2. File upload validation:
   - Verify file extension is .csv
   - Check content type
   - Scan first line for valid header

3. Add OpenAPI documentation:
   - Add springdoc-openapi dependency
   - Annotate controllers with `@Operation`, `@ApiResponse`
   - Access Swagger UI at `/swagger-ui.html`

4. Optional: Rate limiting
   - Bucket4j or custom filter
   - Limit requests per IP/minute

5. **🤖 AI Agent Workflow:**
   - Invoke **test-generator agent** for:
     - Security configuration tests
     - File upload validation tests
     - OpenAPI schema validation
     - Error message sanitization tests
   - Invoke **pr-code-reviewer agent** for:
     - OWASP Top 10 vulnerability assessment
     - Security header configuration
     - Sensitive data exposure review
     - Input validation completeness

**Deliverables:**
- Secure configuration
- Input validation
- API documentation (Swagger)
- ✅ Security test suite (by test-generator agent)
- ✅ Security-focused code review completed (by pr-code-reviewer agent)
- **🔀 Git: Commit and push all Phase 6 changes**
  ```bash
  git add .
  git commit -m "Phase 6: Add security configuration and OpenAPI documentation"
  git push
  ```

---

### Phase 7: Containerization (2-3 hours)

**Objective:** Package application for deployment

**Tasks:**
1. Create `Dockerfile`:
   ```dockerfile
   FROM eclipse-temurin:21-jdk-jammy AS build
   WORKDIR /app
   COPY . .
   RUN ./mvnw clean package -DskipTests

   FROM eclipse-temurin:21-jre-jammy
   WORKDIR /app
   COPY --from=build /app/target/*.jar app.jar

   RUN groupadd -r appuser && useradd -r -g appuser appuser
   USER appuser

   HEALTHCHECK --interval=30s --timeout=3s \
     CMD curl -f http://localhost:8080/actuator/health || exit 1

   EXPOSE 8080
   ENTRYPOINT ["java", "-jar", "app.jar"]
   ```

2. Create `docker-compose.yml`:
   ```yaml
   version: '3.8'
   services:
     claims-api:
       build: .
       ports:
         - "8080:8080"
       environment:
         - SPRING_PROFILES_ACTIVE=docker
       volumes:
         - ./logs:/app/logs
   ```

3. Test containerized deployment:
   - Build image
   - Run container
   - Test all endpoints
   - Verify health checks

4. Update documentation:
   - Docker build instructions
   - Environment variables
   - Production deployment guide

5. **🤖 AI Agent Workflow:**
   - Invoke **test-generator agent** for:
     - Container startup tests
     - Health check validation
     - Environment configuration tests
     - Graceful shutdown tests
   - Invoke **pr-code-reviewer agent** for:
     - Dockerfile best practices review
     - Security hardening (non-root user, minimal image)
     - Resource configuration appropriateness
     - Production readiness assessment

**Deliverables:**
- Working Docker image
- Docker Compose for local development
- Deployment documentation
- ✅ Container deployment tests (by test-generator agent)
- ✅ Docker/deployment configuration review completed (by pr-code-reviewer agent)
- **🔀 Git: Commit and push all Phase 7 changes**
  ```bash
  git add .
  git commit -m "Phase 7: Add Docker containerization and deployment configuration"
  git push
  ```

---

## 6. AI Agent Workflow

### Overview

Each implementation phase **MUST** include automated test generation and code review using specialized AI agents. This ensures comprehensive test coverage and maintains high code quality standards throughout development.

### Required Agents

1. **Test Generator Agent** (`@.claude/agents/test-generator.md`)
   - Generates comprehensive test suites for new components
   - Covers unit tests, integration tests, and edge cases
   - Follows JUnit 5 and AssertJ best practices

2. **PR Code Reviewer Agent** (`@.claude/agents/pr-code-reviewer.md`)
   - Conducts thorough security analysis
   - Reviews code quality and best practices
   - Identifies performance bottlenecks
   - Provides actionable recommendations

### Phase-by-Phase Agent Usage

#### Phase 1: Foundation

**After implementing Reader/Writer modifications:**

```
# Test Generation
Use test-generator agent to create tests for:
- ClaimsReader.readClaims(InputStream)
- ClaimsReader.streamClaims(InputStream, Consumer)
- ClaimsReader.scanForYearRange(InputStream)
- ClaimsWriter.formatCumulativeClaims()
- ClaimsWriter.formatHeader()
- ClaimsWriter.formatProductLine()

Expected test coverage:
- Valid InputStream parsing
- Empty InputStream handling
- Malformed CSV content
- Large file streaming
- Memory efficiency validation
```

```
# Code Review
Use pr-code-reviewer agent to review:
- src/main/java/com/wtw/claims/reader/ClaimsReader.java (new methods)
- src/main/java/com/wtw/claims/writer/ClaimsWriter.java (new methods)
- src/main/java/com/wtw/claims/api/ClaimsApiApplication.java

Focus areas:
- Thread safety of new methods
- Resource management (InputStream closing)
- Backward compatibility with existing methods
- Error handling consistency
```

---

#### Phase 2: Core API

**After implementing service and controller:**

```
# Test Generation
Use test-generator agent to create tests for:
- ClaimsProcessingService
  - processClaims() with various inputs
  - Error scenarios (invalid CSV, empty file)
  - Response structure validation

- ClaimsController
  - File upload handling (MockMvc)
  - HTTP status codes
  - Response body structure
  - Error response format

- DTOs (ProcessingResultResponse, ProductResult, etc.)
  - JSON serialization/deserialization
  - Null handling
  - Edge cases

Expected test coverage: >90% for all new classes
```

```
# Code Review
Use pr-code-reviewer agent to review:
- src/main/java/com/wtw/claims/api/service/ClaimsProcessingService.java
- src/main/java/com/wtw/claims/api/controller/ClaimsController.java
- src/main/java/com/wtw/claims/api/dto/**/*.java
- src/main/java/com/wtw/claims/api/exception/GlobalExceptionHandler.java

Focus areas:
- REST API design best practices
- Exception handling completeness
- DTO immutability and validation
- Logging appropriateness
- Memory management in file processing
```

---

#### Phase 3: Testing & Validation

**After completing integration tests:**

```
# Test Generation
Use test-generator agent to enhance integration tests:
- End-to-end API workflows
- Performance benchmark tests
- Concurrent request handling tests
- File size boundary tests
- API versioning tests

Additional scenarios:
- Network timeout simulation
- Partial file upload handling
- Content-Type validation
- Request parameter validation
```

```
# Code Review
Use pr-code-reviewer agent to review:
- All integration test classes
- Test data management
- Test isolation and cleanup
- Assertion quality and completeness

Focus areas:
- Test reliability (no flaky tests)
- Test coverage gaps
- Test performance (execution time)
- Test maintainability
```

---

#### Phase 4: Health & Monitoring

**After configuring Actuator:**

```
# Test Generation
Use test-generator agent to create tests for:
- Health check endpoint responses
- Custom health indicators (if any)
- Actuator endpoint security
- Metrics accuracy

Test scenarios:
- Health check returns UP when healthy
- Health check returns DOWN on failures
- Info endpoint returns correct application details
- Metrics are accurate and accessible
```

```
# Code Review
Use pr-code-reviewer agent to review:
- src/main/resources/application.yml (Actuator configuration)
- Custom health indicator implementations
- Exposed endpoint security

Focus areas:
- Sensitive information exposure
- Health check accuracy
- Appropriate metric collection
- Production readiness
```

---

#### Phase 5: Async Processing

**After implementing async job management:**

```
# Test Generation
Use test-generator agent to create tests for:
- AsyncJobService
  - Job submission
  - Status tracking
  - Result retrieval
  - Job cleanup/expiration
  - Concurrent job handling

- Async controller endpoints
  - 202 Accepted responses
  - Job status polling
  - Error handling for failed jobs
  - Timeout scenarios

Test scenarios:
- Multiple concurrent async jobs
- Job timeout handling
- Memory cleanup verification
- Thread pool behavior
- Race condition testing
```

```
# Code Review
Use pr-code-reviewer agent to review:
- src/main/java/com/wtw/claims/api/service/AsyncJobService.java
- src/main/java/com/wtw/claims/api/config/AsyncConfig.java
- Async controller methods

Focus areas:
- Thread safety of job storage
- Memory leak prevention
- Proper executor configuration
- Job cleanup mechanisms
- Error propagation in async context
```

---

#### Phase 6: Security & Documentation

**After security configuration:**

```
# Test Generation
Use test-generator agent to create tests for:
- SecurityConfig
  - Endpoint access control
  - CSRF disabled for API
  - Session management (stateless)

- File upload validation
  - File type checking
  - File size limits
  - Malicious content detection

- OpenAPI documentation
  - Schema validation
  - Endpoint documentation completeness

Test scenarios:
- Unauthorized access attempts
- File upload with wrong content type
- Oversized file rejection
- Security headers presence
```

```
# Code Review
Use pr-code-reviewer agent to review:
- src/main/java/com/wtw/claims/api/config/SecurityConfig.java
- File validation logic
- OpenAPI annotations on controllers
- Error message sanitization

Focus areas:
- OWASP Top 10 vulnerabilities
- Sensitive data exposure in errors
- Input validation completeness
- Security header configuration
- API documentation accuracy
```

---

#### Phase 7: Containerization

**After Docker configuration:**

```
# Test Generation
Use test-generator agent to create tests for:
- Container health checks
- Environment variable configuration
- Log file externalization
- Application startup in container

Test scenarios:
- Docker build succeeds
- Container starts with health check passing
- Environment-specific configuration works
- Graceful shutdown handling
```

```
# Code Review
Use pr-code-reviewer agent to review:
- Dockerfile
- docker-compose.yml
- Kubernetes manifests (if created)
- Production configuration files

Focus areas:
- Docker best practices (multi-stage build, non-root user)
- Image size optimization
- Security hardening
- Resource limits appropriateness
- Health check configuration
```

---

### Agent Invocation Checklist

For each phase, complete this checklist:

- [ ] **Implementation Complete**
  - All code for the phase is written
  - Basic manual testing confirms functionality

- [ ] **Test Generator Agent**
  - Invoke with: "Generate comprehensive test suite for [component]"
  - Review generated tests
  - Add to test suite
  - Verify all tests pass

- [ ] **PR Code Reviewer Agent**
  - Invoke with: "Review the [component] implementation"
  - Address all 🔴 Critical issues
  - Address 🟡 Important improvements
  - Consider 🟢 Suggestions
  - Document any deferred items with rationale

- [ ] **Test Coverage Verification**
  - Run: `mvn jacoco:report`
  - Verify coverage meets targets (>90% for new code)
  - Address any coverage gaps

- [ ] **Regression Testing**
  - Run: `mvn clean test`
  - All 446+ tests must pass
  - No performance degradation

### Example Agent Prompts

**Test Generation:**
```
Generate comprehensive tests for ClaimsProcessingService including:
- Happy path scenarios with valid CSV input
- Error scenarios (malformed CSV, empty file, invalid data)
- Edge cases (single record, many products, missing values)
- Performance considerations (large files)
Focus on AAA pattern, clear assertions, and comprehensive coverage.
```

**Code Review:**
```
Review the ClaimsController implementation focusing on:
- REST API best practices
- Input validation completeness
- Error handling and response formats
- Security considerations for file uploads
- Performance implications
Provide specific recommendations with code examples.
```

### Quality Gates

Each phase cannot proceed to the next until:

1. ✅ Test Generator agent has created comprehensive tests
2. ✅ All generated tests pass
3. ✅ PR Code Reviewer agent has reviewed all new code
4. ✅ All Critical (🔴) issues are resolved
5. ✅ All Important (🟡) issues are addressed or deferred with justification
6. ✅ Test coverage meets minimum threshold (>90%)
7. ✅ All existing tests still pass (regression check)
8. ✅ Documentation is updated

---

## 7. Testing Strategy

### Test Pyramid

```
                    ▲
                   ╱ ╲
                  ╱   ╲        Integration Tests
                 ╱─────╲       (End-to-End API)
                ╱       ╲
               ╱         ╲
              ╱───────────╲    Unit Tests
             ╱             ╲   (Service, Controller)
            ╱_______________╲
```

### Unit Tests

**Controller Tests (MockMvc)**
```java
@WebMvcTest(ClaimsController.class)
class ClaimsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimsProcessingService processingService;

    @Test
    void shouldProcessValidCsvFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.csv", "text/csv", csvContent.getBytes()
        );

        when(processingService.processClaims(any())).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/claims/process")
                .file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metadata.totalRecords").value(12))
            .andExpect(jsonPath("$.results[0].product").value("Comp"));
    }
}
```

**Service Tests**
```java
@ExtendWith(MockitoExtension.class)
class ClaimsProcessingServiceTest {

    @Test
    void shouldProcessClaimsCorrectly() throws Exception {
        // Test with actual business logic
        ClaimsProcessingService service = new ClaimsProcessingService();

        String csvContent = "Product, Origin Year, Development Year, Incremental Value\n" +
                           "Comp, 1992, 1992, 110.0\n";

        ProcessingResultResponse result = service.processClaims(
            new ByteArrayInputStream(csvContent.getBytes())
        );

        assertThat(result.metadata().totalRecords()).isEqualTo(1);
        assertThat(result.results()).hasSize(1);
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ClaimsApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldProcessRealCsvFile() {
        // Arrange
        Resource resource = new ClassPathResource("files/problem.csv");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Act
        ResponseEntity<ProcessingResultResponse> response = restTemplate.postForEntity(
            "/api/v1/claims/process",
            new HttpEntity<>(body, headers),
            ProcessingResultResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().metadata().totalRecords()).isEqualTo(12);
        assertThat(response.getBody().results()).extracting("product")
            .containsExactly("Comp", "Non-Comp");
    }

    @Test
    void shouldReturnErrorForInvalidCsv() {
        // Test error handling
    }

    @Test
    void shouldReturnErrorForFileTooLarge() {
        // Test file size limits
    }
}
```

### Test Coverage Goals

- **Overall Coverage**: >80%
- **Service Layer**: >90%
- **Controller Layer**: >90%
- **Error Handling**: 100%
- **Existing Tests**: All 446 must pass (regression)

---

## 8. Security Considerations

### Phase 1: Basic Security

1. **Stateless API Configuration**
   - Disable CSRF (API uses no browser sessions)
   - No session management
   - Stateless JWT in future

2. **File Upload Security**
   - Size limits (50MB max)
   - File type validation (.csv only)
   - Content validation (check header structure)

3. **Input Validation**
   - Leverage existing `ClaimRecordValidator`
   - Bean Validation (JSR-380) for requests
   - Sanitize error messages (no internal details)

### Phase 2: Enhanced Security (Future)

1. **Authentication**
   - JWT token-based authentication
   - API key management for service-to-service
   - OAuth2 integration (if needed)

2. **Authorization**
   - Role-based access control
   - Rate limiting per client
   - Request quotas

3. **Data Protection**
   - HTTPS only (configure in production)
   - Sensitive data masking in logs
   - Audit trail for processing requests

4. **Security Headers**
   ```java
   http.headers(headers -> headers
       .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
       .frameOptions(frame -> frame.deny())
       .xssProtection(xss -> xss.disable())  // Browser XSS filter
   );
   ```

---

## 9. Deployment & Containerization

### Docker Configuration

**Dockerfile** (Multi-stage build)
```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN mkdir -p /app/logs && chown -R appuser:appuser /app
USER appuser

# Copy JAR
COPY --from=build /app/target/claims-triangle-accumulator-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**docker-compose.yml**
```yaml
version: '3.8'

services:
  claims-api:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: claims-api
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - JAVA_OPTS=-Xmx512m -Xms256m
    volumes:
      - ./logs:/app/logs
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s
    restart: unless-stopped

volumes:
  app-logs:
    driver: local
```

### Environment-Specific Configuration

**application-docker.yml**
```yaml
server:
  port: 8080
  address: 0.0.0.0

logging:
  file:
    name: /app/logs/claims-api.log
  level:
    com.wtw.claims: INFO

spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

### Kubernetes (Future)

**deployment.yaml** (example)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: claims-api
spec:
  replicas: 3
  selector:
    matchLabels:
      app: claims-api
  template:
    metadata:
      labels:
        app: claims-api
    spec:
      containers:
        - name: claims-api
          image: claims-api:latest
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
```

---

## 10. Configuration Changes

### Updated pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Add Spring Boot Parent -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.wtw.claims</groupId>
    <artifactId>claims-triangle-accumulator</artifactId>
    <version>2.0.0</version>  <!-- Version bump for API addition -->
    <packaging>jar</packaging>

    <name>Claims Triangle Accumulator</name>
    <description>Converts incremental insurance claims data into cumulative claims triangles</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <junit.version>5.10.0</junit.version>
        <assertj.version>3.24.2</assertj.version>
    </properties>

    <dependencies>
        <!-- EXISTING: CSV Parsing Library -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-csv</artifactId>
            <version>1.10.0</version>
        </dependency>

        <!-- NEW: Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- NEW: Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- NEW: Spring Boot Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- NEW: Spring Boot Security (optional) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>

        <!-- NEW: OpenAPI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>

        <!-- Logging (Spring Boot uses Logback by default) -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>

        <!-- EXISTING: Testing Dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- NEW: Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <!-- Maven Surefire Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.1.2</version>
            </plugin>

            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>

            <!-- Keep existing exec plugin for CLI -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>com.wtw.claims.ClaimsApplication</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <!-- Profile for running CLI version -->
        <profile>
            <id>cli</id>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-jar-plugin</artifactId>
                        <version>3.3.0</version>
                        <configuration>
                            <archive>
                                <manifest>
                                    <mainClass>com.wtw.claims.ClaimsApplication</mainClass>
                                </manifest>
                            </archive>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: claims-triangle-accumulator-api
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 50MB
      file-size-threshold: 2KB
  jackson:
    serialization:
      write-dates-as-timestamps: false
      indent-output: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  info:
    env:
      enabled: true

info:
  app:
    name: Claims Triangle Accumulator API
    description: REST API for processing insurance claims triangles
    version: 2.0.0
    java-version: ${java.version}

# Custom application properties
claims:
  processing:
    async:
      enabled: true
      max-concurrent-jobs: 10
      job-timeout-minutes: 30
      cleanup-interval-minutes: 60
    sync:
      max-file-size-mb: 10

logging:
  level:
    com.wtw.claims: INFO
    org.springframework.web: INFO
    org.springframework.security: INFO
```

---

## 11. Risk Assessment

### Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Memory issues with large file uploads | Medium | High | Use streaming approach (already implemented), set conservative limits |
| Breaking existing CLI functionality | Low | High | Keep CLI code separate, comprehensive regression tests |
| Spring Boot compatibility issues | Low | Medium | Use stable LTS version (3.2.x), test thoroughly |
| Performance degradation | Medium | Medium | Baseline testing, monitoring, async for large files |
| Security vulnerabilities | Medium | High | Input validation, file type checks, regular security audits |

### Mitigation Strategies

1. **Memory Management**
   - Leverage existing streaming approach in ClaimsReader
   - Set file size limits (10MB sync, 50MB async)
   - Monitor heap usage with Actuator metrics
   - Consider temp file storage for very large uploads

2. **Backward Compatibility**
   - CLI entry point (`ClaimsApplication.main()`) remains unchanged
   - Add new methods to Reader/Writer, don't modify existing signatures
   - All 446 existing tests must pass (regression gate)

3. **Performance**
   - Baseline performance metrics before deployment
   - Use async processing for files >10MB
   - Configure thread pools appropriately
   - Monitor response times with Actuator

4. **Security**
   - Validate file uploads (type, size, content)
   - Sanitize error messages (no internal stack traces to client)
   - Use HTTPS in production
   - Regular dependency security scanning (OWASP, Snyk)

---

## 12. Success Criteria

### Functional Requirements

- [ ] REST API processes CSV files correctly (matching CLI output)
- [ ] All existing 446 tests pass (no regression)
- [ ] Synchronous endpoint handles files up to 10MB
- [ ] Async endpoint handles files up to 50MB
- [ ] Health check endpoints functional
- [ ] Error responses are informative and consistent

### Non-Functional Requirements

- [ ] Response time < 5 seconds for 10MB file
- [ ] API handles 10 concurrent requests without errors
- [ ] Memory usage stays within configured limits (512MB)
- [ ] New code has >90% test coverage
- [ ] API documentation complete (Swagger/OpenAPI)

### Deployment Requirements

- [ ] Docker image builds successfully
- [ ] Container starts and responds to health checks
- [ ] Logs are externalized and accessible
- [ ] Configuration is externalized (env vars/config files)

### Documentation Requirements

- [ ] README updated with API usage
- [ ] API endpoints documented (OpenAPI spec)
- [ ] Deployment guide complete
- [ ] Architecture decisions documented

---

## Timeline Summary

| Phase | Duration | Key Deliverables |
|-------|----------|------------------|
| **Phase 1: Foundation** | 4-6 hours | Spring Boot setup, I/O layer adaptations |
| **Phase 2: Core API** | 6-8 hours | Sync endpoint, DTOs, error handling |
| **Phase 3: Testing** | 4-6 hours | Integration tests, validation |
| **Phase 4: Health & Monitoring** | 2-3 hours | Actuator, health checks |
| **Phase 5: Async Processing** | 4-6 hours | Async jobs, status tracking |
| **Phase 6: Security & Docs** | 3-4 hours | Security config, OpenAPI |
| **Phase 7: Containerization** | 2-3 hours | Docker, deployment docs |
| **Total** | **25-36 hours** | Production-ready REST API |

---

## Next Steps

1. **Review this plan** with stakeholders
2. **Set up feature branch** for Web API development
3. **Start with Phase 1** - minimal viable API
4. **Iterate** through phases with regular testing
5. **Demo** after each phase completion
6. **Deploy** to staging environment for final testing

---

**Document Version:** 1.0
**Created:** November 2025
**Author:** Claims Triangle Accumulator Team
**Last Updated:** November 2025

---

*This plan provides a comprehensive roadmap for adding REST API capabilities to the Claims Triangle Accumulator while maintaining existing functionality and adhering to best practices for production-grade software.*
