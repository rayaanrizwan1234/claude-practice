# Claims Triangle Accumulator - Technical Test

## Problem Overview

This project converts incremental insurance claims data into cumulative claims data triangles for actuarial analysis and claims reserving.

### What are Claims Triangles?

Claims triangles are used in insurance to track and predict future claim payments. The data is organized by:
- **Origin Year**: The year when claims originally occurred
- **Development Year**: The year when payments were made
- **Incremental Value**: Amount paid in that specific development year

The goal is to accumulate these incremental values to show total payments over time, making it easier to spot patterns and estimate future claims.

## Input Format

CSV file with the following structure:

```
Product, Origin Year, Development Year, Incremental Value
Comp, 1992, 1992, 110.0
Comp, 1992, 1993, 170.0
Comp, 1993, 1993, 200.0
Non-Comp, 1990, 1990, 45.2
Non-Comp, 1990, 1991, 64.8
Non-Comp, 1990, 1993, 37.0
Non-Comp, 1991, 1991, 50.0
Non-Comp, 1991, 1992, 75.0
Non-Comp, 1991, 1993, 25.0
Non-Comp, 1992, 1992, 55.0
Non-Comp, 1992, 1993, 85.0
Non-Comp, 1993, 1993, 100.0
```

## Output Format

CSV file with the following structure:

```
1990, 4
Comp, 0, 0, 0, 0, 0, 0, 0, 110, 280, 200
Non-Comp, 45.2, 110, 110, 147, 50, 125, 150, 55, 140, 100
```

**Line 1**: `<earliest_origin_year>, <number_of_development_years>`

**Subsequent lines**: `<Product>, <cumulative_values_for_triangle>`

The cumulative values are arranged as a flattened triangle:
- Row 1 (Origin Year 1990): 4 development years of data
- Row 2 (Origin Year 1991): 3 development years of data
- Row 3 (Origin Year 1992): 2 development years of data
- Row 4 (Origin Year 1993): 1 development year of data

## Data Transformation Logic

### Example Calculation for Non-Comp Product:

**Origin Year 1990:**
- Dev Year 1990 (Year 1): 45.2 (cumulative: 45.2)
- Dev Year 1991 (Year 2): 64.8 (cumulative: 45.2 + 64.8 = 110.0)
- Dev Year 1992 (Year 3): 0 (missing, cumulative: 110.0)
- Dev Year 1993 (Year 4): 37.0 (cumulative: 110.0 + 37.0 = 147.0)

**Origin Year 1991:**
- Dev Year 1991 (Year 1): 50.0 (cumulative: 50.0)
- Dev Year 1992 (Year 2): 75.0 (cumulative: 50.0 + 75.0 = 125.0)
- Dev Year 1993 (Year 3): 25.0 (cumulative: 125.0 + 25.0 = 150.0)

**Output**: `Non-Comp, 45.2, 110, 110, 147, 50, 125, 150, 55, 140, 100`

## Key Requirements

### Functional Requirements
1. Read incremental claims data from CSV file
2. Group data by product
3. Calculate cumulative values for each origin year and development year
4. Handle missing data (zero incremental values may be omitted)
5. Handle missing origin years (years with no claims may be omitted)
6. Output results in the specified format

### Technical Requirements
- **Language**: Java (object-oriented design)
- **Engineering Best Practices**:
  - Clean code and proper structure
  - Error handling and validation
  - Unit tests
  - Documentation
  - SOLID principles
- **Time Constraint**: 2-4 hours suggested duration

### Git Workflow & Best Practices
- **DO NOT work directly on the main branch**
- Create a development/feature branch for your work (e.g., `git checkout -b development` or `git checkout -b feature/claims-accumulator`)
- **Commit and push frequently** with clear, meaningful commit messages
- Each commit should represent a logical unit of work, and should be pushed to the remote repository to keep it synchronized
- Push your commits regularly using `git push` to ensure backup and allow for collaboration/review
- Create a pull request from your development branch to main for review
- Use the PR description to provide a self-review, highlighting:
  - Assumptions made
  - Trade-offs considered
  - Areas for future refactoring
  - Any limitations or known issues

### Edge Cases to Handle
1. **Missing incremental values**: If a value is 0, it may be omitted from input
2. **Multiple products**: Input may contain data for multiple insurance products
3. **Missing origin years**: Some years may have no claims data
4. **Data validation**:
   - Invalid CSV format
   - Non-numeric values
   - Inconsistent data (e.g., development year < origin year)
   - Empty files
5. **Large datasets**: May contain many origin years and products

## Implementation Approach

### Data Structures
- **ClaimRecord**: Represents a single row of input data
- **ClaimsTriangle**: Represents all data for one product
- **TriangleAccumulator**: Main logic for accumulation

### Algorithm
1. Parse CSV input into ClaimRecord objects
2. Group records by Product
3. For each product:
   - Identify earliest origin year and latest development year
   - Create a 2D structure (origin year × development year)
   - Fill in incremental values (default to 0 if missing)
   - Calculate cumulative sums for each origin year
   - Flatten the triangle for output
4. Write output CSV

### Validation Strategy
- Validate CSV header format
- Ensure years are valid integers
- Ensure incremental values are valid decimals
- Check that development year >= origin year
- Handle malformed rows gracefully

## Testing Strategy

### Unit Tests
- CSV parsing with valid data
- CSV parsing with invalid data
- Accumulation logic for single product
- Accumulation logic for multiple products
- Handling of missing values
- Handling of missing origin years
- Edge cases (empty file, single record, etc.)

### Integration Tests
- End-to-end test with example data from README
- Test with cumulative_claims.csv and problem.csv

## Files

### Input
- `files/problem.csv` - Example input data with incremental claims

### Expected Output
- `files/cumulative_claims.csv` - Expected output with cumulative claims

## Notes

- This project allows AI-generated code assistance
- Focus on demonstrating engineering best practices
- Use pull request for self-review and highlighting assumptions
- Document any trade-offs or future refactoring ideas in PR comments
- Always use @.claude/agents/test-generator.md sub agent after implementing each component to generate comprehensive test cases
- Always use @.claude/agents/pr-code-reviewer.md sub agent after each implementation phase