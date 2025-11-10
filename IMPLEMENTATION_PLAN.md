# Claims Triangle Accumulator - Implementation Plan

## Overview
This document provides a detailed, methodical implementation plan for the Claims Triangle Accumulator project. Each phase is broken down into small, manageable steps with code reviews after each phase.

---

## PHASE 1: Project Setup ✅ COMPLETED

### Tasks
- [x] Create Maven project structure (pom.xml, directories, .gitignore)
- [x] Create feature branch: `git checkout -b development`
- [x] Initial commit: "Initialize Maven project structure"
- [x] Code review with @pr-code-reviewer agent

### Status
**Completed** - Committed and pushed to `origin/development`

---

## PHASE 2: Read CSV Data - Model Layer (30 min)

### 2.1 Create ClaimRecord Model
**File:** `src/main/java/com/wtw/claims/model/ClaimRecord.java`

**Requirements:**
- Immutable class (final class, final fields)
- Fields:
  - `String product`
  - `int originYear`
  - `int developmentYear`
  - `double incrementalValue`
- Constructor with validation:
  - Product must not be null or empty
  - Trim whitespace from product
- Getters for all fields
- Override `equals()`, `hashCode()`, `toString()`
- Comprehensive JavaDoc

### 2.2 Generate Tests
**File:** `src/test/java/com/wtw/claims/model/ClaimRecordTest.java`

Use **@test-generator** agent or create manually:
- Constructor tests (valid, null product, empty product, whitespace trimming)
- Getter tests
- Equals tests (same object, equal objects, different fields, null, different class)
- HashCode tests (equal objects same hash, equals-hashCode contract)
- ToString test

### 2.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add ClaimRecord model with tests"

---

## PHASE 3: Read CSV Data - Validation Layer (30 min)

### 3.1 Create Custom Exception
**File:** `src/main/java/com/wtw/claims/exception/InvalidClaimRecordException.java`

```java
public class InvalidClaimRecordException extends Exception {
    public InvalidClaimRecordException(String message) {
        super(message);
    }
}
```

### 3.2 Create Validator
**File:** `src/main/java/com/wtw/claims/validator/ClaimRecordValidator.java`

**Methods:**
- `void validateRecord(ClaimRecord record)` - throws InvalidClaimRecordException
- `boolean isValidYear(int year)` - check reasonable year range (e.g., 1900-2100)
- `boolean isValidDevelopmentYear(int origin, int dev)` - dev >= origin
- `boolean isValidValue(double value)` - non-negative (or allow negative based on requirements)

### 3.3 Generate Tests
**File:** `src/test/java/com/wtw/claims/validator/ClaimRecordValidatorTest.java`

Use **@test-generator** agent:
- Valid record passes
- Development year < origin year fails
- Invalid years fail
- Invalid values fail (if applicable)

### 3.4 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add validation layer with tests"

---

## PHASE 4: Read CSV Data - CSV Reader (45 min)

### 4.1 Implement CSV Reader
**File:** `src/main/java/com/wtw/claims/io/ClaimsReader.java`

**Method signature:**
```java
public List<ClaimRecord> readClaims(String filePath) throws IOException
```

**Implementation:**
- Use Apache Commons CSV library
- Parse CSV with header: "Product, Origin Year, Development Year, Incremental Value"
- Trim whitespace from all fields
- Parse integers and doubles
- Handle parse exceptions gracefully (log and skip or throw)
- Validate header format

**Dependencies:**
- Already in pom.xml: `org.apache.commons:commons-csv:1.10.0`

### 4.2 Generate Tests
**File:** `src/test/java/com/wtw/claims/io/ClaimsReaderTest.java`

Use **@test-generator** agent:
- Read valid CSV (use `files/problem.csv`)
- Handle missing header
- Handle malformed rows (invalid numbers)
- Handle empty file
- Handle file not found
- Verify whitespace trimming

**Test Resources:**
Create test CSV files in `src/test/resources/`:
- `valid_claims.csv` - small valid dataset
- `invalid_header.csv` - wrong header
- `malformed_data.csv` - non-numeric values
- `empty.csv` - empty file

### 4.3 Manual Testing
```bash
# Create simple test in main method or run specific test
mvn test -Dtest=ClaimsReaderTest
```

Print parsed records to verify correctness.

### 4.4 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add CSV reader with error handling and tests"

---

## PHASE 5: Store Data - ClaimsTriangle Model (30 min)

### 5.1 Create ClaimsTriangle Class
**File:** `src/main/java/com/wtw/claims/model/ClaimsTriangle.java`

**Fields:**
```java
private final String productName;
private final int earliestOriginYear;
private final int latestDevelopmentYear;
private final Map<Integer, Map<Integer, Double>> incrementalData;  // originYear -> (devYear -> value)
private final Map<Integer, Map<Integer, Double>> cumulativeData;   // originYear -> (devYear -> cumulative)
```

**Methods:**
- Constructor
- `void addIncrementalValue(int originYear, int devYear, double value)`
- `double getIncrementalValue(int originYear, int devYear)` - return 0.0 if missing
- `double getCumulativeValue(int originYear, int devYear)`
- Getters for productName, earliestOriginYear, latestDevelopmentYear

### 5.2 Generate Tests
**File:** `src/test/java/com/wtw/claims/model/ClaimsTriangleTest.java`

Use **@test-generator** agent:
- Add and retrieve incremental values
- Handle missing values (return 0.0)
- Multiple origin/dev years

### 5.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add ClaimsTriangle data model with tests"

---

## PHASE 6: Store Data - Group by Product (30 min)

### 6.1 Create Data Processor
**File:** `src/main/java/com/wtw/claims/processor/DataProcessor.java`

**Methods:**
```java
public Map<String, List<ClaimRecord>> groupByProduct(List<ClaimRecord> records)
public int findEarliestOriginYear(List<ClaimRecord> records)
public int findLatestDevelopmentYear(List<ClaimRecord> records)
```

**Implementation:**
- Group records by product using streams or loops
- Find global min origin year across ALL products
- Find global max development year across ALL products

### 6.2 Generate Tests
**File:** `src/test/java/com/wtw/claims/processor/DataProcessorTest.java`

Use **@test-generator** agent:
- Group single product
- Group multiple products
- Find min/max years correctly
- Handle empty list

### 6.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add product grouping and year range logic with tests"

---

## PHASE 7: Store Data - Build Triangles (30 min)

### 7.1 Implement Triangle Builder
**File:** `src/main/java/com/wtw/claims/processor/TriangleBuilder.java`

**Method signature:**
```java
public ClaimsTriangle buildTriangle(String product, List<ClaimRecord> records,
                                     int globalMinYear, int globalMaxYear)
```

**Implementation:**
- Create ClaimsTriangle for the product
- Iterate through all combinations of (originYear, devYear) where devYear >= originYear
- Fill in incremental values from records
- Missing cells default to 0.0

### 7.2 Generate Tests
**File:** `src/test/java/com/wtw/claims/processor/TriangleBuilderTest.java`

Use **@test-generator** agent:
- Build triangle with all years present
- Build triangle with missing development years (gaps)
- Build triangle with missing origin years
- Verify 0.0 for missing data

### 7.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add triangle building logic with tests"

---

## PHASE 8: Calculate Cumulative Values (45 min)

### 8.1 Implement Accumulator
**File:** `src/main/java/com/wtw/claims/processor/TriangleAccumulator.java`

**Method signature:**
```java
public void calculateCumulative(ClaimsTriangle triangle)
```

**Algorithm:**
```java
for each origin year from earliest to latest:
    cumulative = 0.0
    for each development year from origin year to latest:
        incremental = triangle.getIncrementalValue(origin, dev)
        cumulative += incremental
        triangle.setCumulativeValue(origin, dev, cumulative)
```

**Key Logic:**
- For Non-Comp Origin 1990:
  - Dev 1990: incremental=45.2, cumulative=45.2
  - Dev 1991: incremental=64.8, cumulative=45.2+64.8=110.0
  - Dev 1992: incremental=0 (missing), cumulative=110.0
  - Dev 1993: incremental=37.0, cumulative=110.0+37.0=147.0

### 8.2 Generate Tests
**File:** `src/test/java/com/wtw/claims/processor/TriangleAccumulatorTest.java`

Use **@test-generator** agent:
- Single origin year accumulation
- Multiple origin years
- Missing values don't break cumulative
- **Verify against expected output:**
  - Non-Comp Origin 1990: [45.2, 110.0, 110.0, 147.0]
  - Non-Comp Origin 1991: [50.0, 125.0, 150.0]
  - Comp Origin 1992: [110.0, 280.0]

### 8.3 Manual Testing
Print cumulative values for problem.csv data and verify manually.

### 8.4 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add cumulative calculation logic with tests"

---

## PHASE 9: Flatten Triangle for Output (30 min)

### 9.1 Implement Flattener
**Add method to ClaimsTriangle:**
```java
public List<Double> flattenCumulative()
```

**Implementation:**
- Iterate through origin years (earliest to latest)
- For each origin year, iterate development years (origin to latest)
- Add cumulative value to list
- Return flattened list (row-major order)

**Example for Comp:**
```
Origin 1990: [0, 0, 0, 0]
Origin 1991: [0, 0, 0]
Origin 1992: [110.0, 280.0]
Origin 1993: [200.0]
Flattened: [0, 0, 0, 0, 0, 0, 0, 110.0, 280.0, 200.0]
```

### 9.2 Generate Tests
**File:** Update `ClaimsTriangleTest.java`

Use **@test-generator** agent:
- Single row triangle
- Multi-row triangle
- Verify order: Comp should have 10 values, Non-Comp 10 values
- Verify values match expected from cumulative_claims.csv

### 9.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add triangle flattening logic with tests"

---

## PHASE 10: Write Output CSV (30 min)

### 10.1 Implement CSV Writer
**File:** `src/main/java/com/wtw/claims/io/ClaimsWriter.java`

**Method signature:**
```java
public void writeCumulativeClaims(String filePath,
                                   Map<String, ClaimsTriangle> triangles,
                                   int earliestOriginYear,
                                   int numberOfDevelopmentYears) throws IOException
```

**Output Format:**
- Line 1: `{earliestYear},{numDevYears}` (NO spaces after comma)
- Product lines: `{Product},{val1},{val2},...` (NO spaces)
- Decimal format: Keep `.0` (use `String.format("%.1f", value)` or similar)
- Sort products alphabetically

**Example:**
```
1990,4
Comp,0.0,0.0,0.0,0.0,0.0,0.0,0.0,110.0,280.0,200.0
Non-Comp,45.2,110.0,110.0,147.0,50.0,125.0,150.0,55.0,140.0,100.0
```

### 10.2 Generate Tests
**File:** `src/test/java/com/wtw/claims/io/ClaimsWriterTest.java`

Use **@test-generator** agent:
- Header line format correct
- Product lines format correct
- No spaces after commas
- Decimal formatting (preserve .0)
- Products sorted alphabetically
- File written correctly

### 10.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add CSV output writer with tests"

---

## PHASE 11: Application Entry Point (30 min)

### 11.1 Create Main Class
**File:** `src/main/java/com/wtw/claims/ClaimsApplication.java`

**Main method:**
```java
public static void main(String[] args) {
    if (args.length != 2) {
        System.err.println("Usage: java ClaimsApplication <input-file> <output-file>");
        System.exit(1);
    }

    String inputFile = args[0];
    String outputFile = args[1];

    try {
        // 1. Read claims
        // 2. Group by product
        // 3. Build triangles
        // 4. Calculate cumulative
        // 5. Write output
        System.out.println("Processing complete: " + outputFile);
    } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }
}
```

**Orchestration:**
1. ClaimsReader.readClaims(inputFile)
2. DataProcessor.groupByProduct(records)
3. Find min year, max year
4. For each product: TriangleBuilder.buildTriangle()
5. For each triangle: TriangleAccumulator.calculateCumulative()
6. ClaimsWriter.writeCumulativeClaims(outputFile, triangles, minYear, numYears)

### 11.2 Manual Testing
```bash
# Build the project
mvn clean install

# Run with problem.csv
mvn exec:java -Dexec.mainClass="com.wtw.claims.ClaimsApplication" -Dexec.args="files/problem.csv output.csv"

# Compare output
diff output.csv files/cumulative_claims.csv
```

If diff shows no differences, the implementation is correct!

### 11.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Verify: `diff output.csv files/cumulative_claims.csv` (should be identical)
- Commit: "Add application entry point and orchestration"

---

## PHASE 12: Integration Testing (30 min)

### 12.1 Create Integration Test
**File:** `src/test/java/com/wtw/claims/integration/EndToEndTest.java`

**Test:**
```java
@Test
@DisplayName("Should process problem.csv and produce expected cumulative_claims.csv output")
void endToEnd_withProblemCsv_producesExpectedOutput() throws Exception {
    // Arrange
    String inputFile = "files/problem.csv";
    String expectedOutputFile = "files/cumulative_claims.csv";
    String actualOutputFile = "target/test-output.csv";

    // Act
    ClaimsApplication.main(new String[]{inputFile, actualOutputFile});

    // Assert
    List<String> expectedLines = Files.readAllLines(Paths.get(expectedOutputFile));
    List<String> actualLines = Files.readAllLines(Paths.get(actualOutputFile));

    assertThat(actualLines).isEqualTo(expectedLines);
}
```

### 12.2 Generate Additional Tests
Use **@test-generator** agent for edge case integration tests:
- Empty input file
- Single product
- Single origin year

### 12.3 Code Review & Commit
- Use **@pr-code-reviewer** agent
- Run tests: `mvn test`
- Commit: "Add end-to-end integration tests"

---

## PHASE 13: Documentation (30 min)

### 13.1 Create RUNME.md
**File:** `RUNME.md`

**Contents:**
```markdown
# Claims Triangle Accumulator - How to Run

## Prerequisites
- Java 11 or higher
- Maven 3.6.0 or higher

## Build the Project
```bash
mvn clean install
```

## Run the Application

### Using Maven Exec Plugin
```bash
mvn exec:java -Dexec.mainClass="com.wtw.claims.ClaimsApplication" -Dexec.args="<input-file> <output-file>"
```

### Example
```bash
mvn exec:java -Dexec.mainClass="com.wtw.claims.ClaimsApplication" -Dexec.args="files/problem.csv output.csv"
```

### Using JAR
```bash
java -jar target/claims-triangle-accumulator-1.0.0.jar <input-file> <output-file>
```

## Run Tests
```bash
mvn test
```

## Input Format
CSV file with header: `Product, Origin Year, Development Year, Incremental Value`

Example:
```
Product, Origin Year, Development Year, Incremental Value
Comp, 1992, 1992, 110.0
Comp, 1992, 1993, 170.0
```

## Output Format
Line 1: `<earliest_origin_year>,<number_of_development_years>`
Subsequent lines: `<Product>,<cumulative_values>`

Example:
```
1990,4
Comp,0.0,0.0,0.0,0.0,0.0,0.0,0.0,110.0,280.0,200.0
```
```

### 13.2 Add JavaDoc
Review all classes and ensure:
- Class-level JavaDoc with description
- Method-level JavaDoc with @param, @return, @throws
- Complex logic has inline comments

### 13.3 Final Code Review
- Use **@pr-code-reviewer** agent for comprehensive review of entire codebase
- Address any final issues

### 13.4 Commit
- Commit: "Add documentation and final polish"

---

## PHASE 14: Pull Request (15 min)

### 14.1 Create PR: development → main

**PR Title:**
`Implement Claims Triangle Accumulator`

**PR Description:**

```markdown
## Summary
Implements a Java application that converts incremental insurance claims data into cumulative claims triangles for actuarial analysis.

## Implementation Approach
- **Architecture**: Layered design with separation of concerns
  - Model layer: ClaimRecord, ClaimsTriangle
  - I/O layer: ClaimsReader, ClaimsWriter
  - Processing layer: DataProcessor, TriangleBuilder, TriangleAccumulator
  - Validation layer: ClaimRecordValidator
- **Libraries**: Apache Commons CSV for robust CSV parsing
- **Testing**: Comprehensive unit tests and integration tests (JUnit 5 + AssertJ)

## Key Features
- Handles missing data (treated as 0)
- Validates input data
- Supports multiple products
- Produces correctly formatted output

## Assumptions Made
1. **Missing data = 0**: When incremental value is missing for a development year, it's treated as 0.0
2. **Alphabetical sorting**: Products are sorted alphabetically in output
3. **Decimal format**: Output preserves decimal point (110.0 not 110)
4. **Global year range**: Uses earliest origin year and latest development year across ALL products
5. **Product names**: Trimmed of whitespace, case-sensitive
6. **Development year >= origin year**: Validated in validator

## Trade-offs
1. **In-memory processing**: All data loaded into memory. Suitable for reasonable dataset sizes. For very large files, streaming approach would be better.
2. **Apache Commons CSV vs native parsing**: Used Commons CSV for robustness and maintainability over hand-rolled parsing.
3. **Exception handling**: Fail-fast approach - first error stops processing. Could be enhanced to collect all errors.
4. **Double precision**: Using double for monetary values. In production, BigDecimal would be more appropriate for financial data.

## Testing
- **Unit test coverage**: All core classes have comprehensive unit tests
- **Integration test**: End-to-end test verifies problem.csv → cumulative_claims.csv
- **Test results**: All tests passing
- **Manual verification**: `diff output.csv files/cumulative_claims.csv` shows no differences

## Future Enhancements
1. **Streaming for large files**: Process data in chunks to handle very large datasets
2. **Configurable decimal precision**: Allow users to specify decimal places in output
3. **Additional output formats**: JSON, XML, or database output
4. **Batch processing**: Process multiple input files at once
5. **Data validation report**: Generate detailed report of validation errors instead of failing fast
6. **BigDecimal for currency**: Use BigDecimal instead of double for financial accuracy
7. **Logging framework**: More sophisticated logging (currently using SLF4J Simple)

## Known Limitations
1. **Year range**: Assumes all years fit in int (no Y10K problem handling!)
2. **Product names**: Case-sensitive, no normalization
3. **CSV format**: Expects exact header format, no flexibility
4. **Error recovery**: Stops on first error, doesn't attempt to continue
5. **Memory usage**: All data in memory - not suitable for files > available heap

## Files Changed
- Core implementation: 8 Java classes
- Tests: 8 test classes
- Configuration: pom.xml, .gitignore
- Documentation: RUNME.md, JavaDoc

## Verification Steps
```bash
# Build and test
mvn clean install

# Run with example data
mvn exec:java -Dexec.mainClass="com.wtw.claims.ClaimsApplication" -Dexec.args="files/problem.csv output.csv"

# Verify output matches expected
diff output.csv files/cumulative_claims.csv
# (should show no differences)
```

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

### 14.2 Self-Review
- Review the diff on GitHub
- Add inline comments on key design decisions:
  - Why nested maps for ClaimsTriangle
  - Why global year range instead of per-product
  - Validation choices
  - Error handling approach

---

## Summary

**Total Estimated Time:** ~6 hours (14 phases)

**Key Deliverables:**
1. ✅ Working Java application (Maven project)
2. ✅ Comprehensive unit tests (JUnit 5 + AssertJ)
3. ✅ Integration test verifying problem.csv → cumulative_claims.csv
4. ✅ Clean, documented code following SOLID principles
5. ✅ RUNME.md with build/run instructions
6. ✅ Pull request with detailed self-review

**Verification:**
- All tests pass: `mvn test`
- Output matches expected: `diff output.csv files/cumulative_claims.csv`
- Code follows Java best practices
- Git history shows logical, incremental commits

---

## Current Progress

- [x] Phase 1: Project Setup
- [ ] Phase 2: ClaimRecord Model
- [ ] Phase 3: Validation Layer
- [ ] Phase 4: CSV Reader
- [ ] Phase 5: ClaimsTriangle Model
- [ ] Phase 6: Product Grouping
- [ ] Phase 7: Triangle Builder
- [ ] Phase 8: Cumulative Calculation
- [ ] Phase 9: Triangle Flattening
- [ ] Phase 10: CSV Writer
- [ ] Phase 11: Application Entry Point
- [ ] Phase 12: Integration Testing
- [ ] Phase 13: Documentation
- [ ] Phase 14: Pull Request
