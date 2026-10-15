/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.milvus.communication;

import io.milvus.v2.common.IndexParam.MetricType;

import java.util.Locale;
import java.util.Objects;

/**
 * Defines the metrics supported by the Milvus vector index.
 *
 * <p>
 * The metric determines how Milvus compares stored vectors with a query vector.
 * It belongs to the collection index configuration, so it is chosen when the collection is created
 * ({@link MilvusConfigurations#METRIC}) and it is not selected per search operation.
 * </p>
 *
 * <pre>
 * query vector
 *      |
 *      v
 * configured metric
 *      |
 *      v
 * stored vectors
 *      |
 *      v
 * nearest matches
 * </pre>
 *
 * <p>
 * The search threshold follows the metric: with {@link #COSINE} and {@link #INNER_PRODUCT} it is the
 * minimum similarity a result must exceed, and with {@link #EUCLIDEAN} it is the maximum distance a result
 * must stay under.
 * </p>
 */
public enum MilvusMetric {

    /**
     * Cosine similarity, from {@code -1} (opposite) to {@code 1} (same direction).
     *
     * <p>
     * It compares the direction of the vectors and ignores their magnitude. It suits embeddings where the
     * pattern matters more than the amount: a user who rated three movies 10, 2 and 1 is close to a user who
     * rated them 5, 1 and 0.5, because both prefer the same movie.
     * </p>
     *
     * <pre>{@code
     * cos(a, b) = (a · b) / (|a| * |b|)
     * }</pre>
     *
     * <p>Higher scores are more similar.</p>
     */
    COSINE(MetricType.COSINE, "COSINE"),

    /**
     * Euclidean (L2) distance, the straight-line distance between two points.
     *
     * <p>
     * It compares the absolute position of the vectors, so magnitude matters: two products with the
     * same attributes but very different prices are far from each other.
     * </p>
     *
     * <pre>{@code
     * l2(a, b) = sqrt(sum((a[i] - b[i])^2))
     * }</pre>
     *
     * <p>Lower distances are more similar. It is also accepted as {@code L2}.</p>
     */
    EUCLIDEAN(MetricType.L2, "L2"),

    /**
     * Inner (dot) product, which grows with both the alignment and the magnitude of the vectors.
     *
     * <p>
     * It suits embeddings where a stronger signal means a stronger match, such as vectors already
     * normalized by the model or recommendation scores that encode relevance in their length.
     * </p>
     *
     * <pre>{@code
     * ip(a, b) = sum(a[i] * b[i])
     * }</pre>
     *
     * <p>Higher scores are more similar. It is also accepted as {@code IP}.</p>
     */
    INNER_PRODUCT(MetricType.IP, "IP");

    private final MetricType metricType;

    private final String alias;

    MilvusMetric(MetricType metricType, String alias) {
        this.metricType = metricType;
        this.alias = alias;
    }

    MetricType toMetricType() {
        return metricType;
    }

    /**
     * Parses a metric by its name or its Milvus name ({@code L2}, {@code IP}), ignoring the case.
     *
     * @param value the metric name
     * @return the metric
     * @throws NullPointerException     when the value is null
     * @throws IllegalArgumentException when the value is not a supported metric
     */
    public static MilvusMetric parse(String value) {
        Objects.requireNonNull(value, "value is required");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (MilvusMetric metric : values()) {
            if (metric.name().equals(normalized) || metric.alias.equals(normalized)) {
                return metric;
            }
        }
        throw new IllegalArgumentException("Unsupported Milvus metric: " + value);
    }
}
