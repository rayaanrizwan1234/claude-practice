package com.wtw.claims.api.dto.response;

import java.util.List;

/**
 * Complete response for a claims processing operation.
 *
 * @param metadata metadata about the processing operation
 * @param results list of product results with cumulative values
 * @param csvOutput the complete CSV output string
 */
public record ProcessingResultResponse(
    ProcessingMetadata metadata,
    List<ProductResult> results,
    String csvOutput
) {}