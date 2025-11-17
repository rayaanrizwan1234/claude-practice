package com.wtw.claims.api.dto.response;

import java.util.List;

/**
 * Result for a single product's cumulative claims triangle.
 *
 * @param product the product name
 * @param cumulativeValues the flattened cumulative values for the triangle
 */
public record ProductResult(
    String product,
    List<Double> cumulativeValues
) {}