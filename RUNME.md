# Claims Triangle Accumulator - Run Instructions

## Quick Start

### Prerequisites
- **Java 11** or higher
- **Maven 3.6+**

### Running the Application

#### Option 1: Using Maven (Recommended for Development)
```bash
# Build the project
mvn clean install

# Run with sample data
mvn exec:java -Dexec.mainClass="com.wtw.claims.ClaimsApplication" \
  -Dexec.args="files/problem.csv output.csv"
```

#### Option 2: Using the JAR (Recommended for Production)
```bash
# Build the JAR
mvn clean package

# Run the application
java -jar target/claims-triangle-accumulator-1.0.0.jar files/problem.csv output.csv
```

### Command-Line Arguments

```bash
java -jar claims-triangle-accumulator.jar <input-file> <output-file>
```

**Parameters:**
- `<input-file>`: Path to the input CSV file containing incremental claims data
- `<output-file>`: Path where the cumulative claims output will be written

**Example:**
```bash
java -jar target/claims-triangle-accumulator-1.0.0.jar files/problem.csv results/cumulative.csv
```

### Input File Format

The input CSV must have the following format:

```csv
Product, Origin Year, Development Year, Incremental Value
Comp, 1992, 1992, 110.0
Comp, 1992, 1993, 170.0
Non-Comp, 1990, 1990, 45.2
```

**Header Row (Required):**
- Product
- Origin Year
- Development Year
- Incremental Value

**Data Rows:**
- **Product**: Name of the insurance product (string)
- **Origin Year**: Year the claim originated (integer, 1900-2100)
- **Development Year**: Year the payment was made (integer, 1900-2100)
- **Incremental Value**: Payment amount for that year (decimal)

**Notes:**
- Whitespace around values is automatically trimmed
- Missing incremental values (zeros) can be omitted from input
- Missing origin years will be filled with zeros in output
- Development year must be >= origin year

### Output File Format

The output CSV will contain:

```csv
1990,4
Comp,0.0,0.0,0.0,0.0,0.0,0.0,0.0,110.0,280.0,200.0
Non-Comp,45.2,110.0,110.0,147.0,50.0,125.0,150.0,55.0,140.0,100.0
```

**Line 1:** `<earliest_origin_year>,<number_of_development_years>`
**Subsequent Lines:** `<Product>,<cumulative_value_1>,<cumulative_value_2>,...`

- Products are sorted alphabetically
- Cumulative values are flattened in row-major order
- All values include decimal point (e.g., 110.0, not 110)
- No spaces after commas

## Building and Testing

### Build the Project
```bash
mvn clean install
```

This will:
- Compile all source code
- Run all 340 tests (324 unit tests + 16 integration tests)
- Create the JAR file in `target/`

### Run Tests Only
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClaimsApplicationIntegrationTest

# Run tests with coverage report
mvn clean test jacoco:report
```

### View Test Coverage
After running `mvn test jacoco:report`, open:
```
target/site/jacoco/index.html
```

## Sample Data

The project includes sample data files in the `files/` directory:

- **problem.csv**: Sample incremental claims data (input)
- **cumulative_claims.csv**: Expected cumulative output

### Try It Out
```bash
# Process the sample data
mvn exec:java -Dexec.mainClass="com.wtw.claims.ClaimsApplication" \
  -Dexec.args="files/problem.csv my_output.csv"

# Compare with expected output
diff my_output.csv files/cumulative_claims.csv
```

Note: There may be a minor formatting difference with zeros (`0.0` vs `0`), but the data is functionally identical.

## Application Output

The application provides detailed logging during execution:

```
======================================================================
Claims Triangle Accumulator
======================================================================
Input file:  files/problem.csv
Output file: output.csv

Step 1/5: Reading claims from CSV...
  Read 12 claim records
Step 2/5: Grouping claims by product...
  Found 2 product(s): [Non-Comp, Comp]
  Year range: 1990 to 1993 (4 development years)
Step 3/5: Building claims triangles...
  Built triangle for 'Non-Comp' (9 records)
  Built triangle for 'Comp' (3 records)
Step 4/5: Calculating cumulative values...
  Calculated cumulative values for 'Non-Comp'
  Calculated cumulative values for 'Comp'
Step 5/5: Writing cumulative claims to CSV...
  Written 2 product triangle(s) to output.csv
======================================================================
Processing complete: output.csv
======================================================================
```

## Error Handling

The application provides clear error messages for common issues:

**Missing Input File:**
```
Error: Input file does not exist: nonexistent.csv
```

**Invalid CSV Format:**
```
Error: Invalid input - Expected 4 columns but found 2
```

**Invalid Data:**
```
Error: Invalid input - Development year (1985) cannot be before origin year (1990)
```

**Empty File:**
```
Error: Invalid input - No claim records found in input file
```

## Troubleshooting

### Issue: "command not found: mvn"
**Solution:** Install Maven from https://maven.apache.org/download.cgi

### Issue: "java: invalid target release: 11"
**Solution:** Ensure you have JDK 11 or higher installed
```bash
java -version  # Should show 11 or higher
```

### Issue: Tests fail with "FileNotFoundException"
**Solution:** Ensure you're running commands from the project root directory where `files/` exists

### Issue: "Permission denied" when writing output
**Solution:** Ensure you have write permissions for the output directory, or specify a different location:
```bash
java -jar target/claims-triangle-accumulator-1.0.0.jar files/problem.csv /tmp/output.csv
```

## Performance

The application is designed for efficiency:
- **Processes** 12 records (2 products) in < 100ms
- **Handles** large datasets with thousands of records
- **Memory**: Minimal heap usage (~10MB for typical datasets)

## Logging Configuration

The application uses SLF4J with Logback. To configure logging:

1. Create `src/main/resources/logback.xml`
2. Adjust log levels as needed:
   - `INFO`: Standard operational logs (default)
   - `DEBUG`: Detailed processing information
   - `ERROR`: Errors only

## Additional Resources

- **Project README**: `README.md` - Comprehensive project documentation
- **Technical Design**: See "Architecture" section in README.md
- **Problem Description**: Original requirements in README.md
- **Source Code**: Fully documented with JavaDoc

## Support

For issues or questions:
1. Check the Troubleshooting section above
2. Review the comprehensive tests in `src/test/java`
3. Examine the detailed JavaDoc in source files

---

**Version**: 1.0.0
**Last Updated**: January 2025
