package com.wtw.claims.api.dto.response;

import java.util.List;

/**
 * Metadata about the claims processing operation.
 *
 * @param earliestOriginYear the earliest origin year in the data
 * @param numberOfDevelopmentYears the total number of development years
 * @param totalRecords the total number of claim records processed
 * @param productsProcessed the list of product names processed
 * @param processingTimeMs the processing time in milliseconds
 */
public record ProcessingMetadata(
    int earliestOriginYear,
    int numberOfDevelopmentYears,
    int totalRecords,
    List<String> productsProcessed,
    long processingTimeMs
) {

    public ProcessingMetadata {
        productsProcessed = productsProcessed != null ? List.copyOf(productsProcessed) : List.of();
    }
}