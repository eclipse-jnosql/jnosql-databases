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
package org.eclipse.jnosql.databases.weaviate.communication;

import io.weaviate.client6.v1.api.collections.vectorindex.Distance;

import java.util.Locale;
import java.util.Objects;

/**
 * Defines the distances supported by the Weaviate vector index.
 *
 * <p>
 * The distance determines how Weaviate compares stored vectors with a query vector.
 * It belongs to the collection vector index configuration, so it is chosen when the collection is created
 * ({@link WeaviateConfigurations#DISTANCE}) and it is not selected per search operation.
 * </p>
 *
 * <pre>
 * query vector
 *      |
 *      v
 * configured distance
 *      |
 *      v
 * stored vectors
 *      |
 *      v
 * nearest matches
 * </pre>
 *
 * <p>
 * The search threshold of the JNoSQL vector API keeps the same meaning for every distance:
 * with {@link #COSINE} and {@link #DOT} it is the minimum similarity a result must have, and with
 * {@link #EUCLIDEAN}, {@link #MANHATTAN} and {@link #HAMMING} it is the maximum distance a result may have.
 * </p>
 */
public enum WeaviateDistance {

    /**
     * Cosine similarity, from {@code -1} (opposite) to {@code 1} (same direction).
     *
     * <p>
     * It compares the direction of the vectors and ignores their magnitude. It suits text embeddings, where
     * two sentences about the same topic point in the same direction even if one of them is much longer.
     * </p>
     *
     * <pre>{@code
     * cos(a, b) = (a · b) / (|a| * |b|)
     * }</pre>
     *
     * <p>Higher similarities are more similar.</p>
     */
    COSINE(Distance.COSINE, "COSINE"),

    /**
     * Dot (inner) product, which grows with both the alignment and the magnitude of the vectors.
     *
     * <p>
     * It suits embeddings that are already normalized by the model, or recommendation scores where a longer
     * vector means a stronger match.
     * </p>
     *
     * <pre>{@code
     * dot(a, b) = sum(a[i] * b[i])
     * }</pre>
     *
     * <p>Higher products are more similar.</p>
     */
    DOT(Distance.DOT, "DOT"),

    /**
     * Euclidean (L2) distance, the straight-line distance between two points.
     *
     * <p>
     * It compares the absolute position of the vectors, so magnitude matters: two products with the same
     * attributes but very different prices are far from each other.
     * </p>
     *
     * <pre>{@code
     * l2(a, b) = sqrt(sum((a[i] - b[i])^2))
     * }</pre>
     *
     * <p>
     * Weaviate stores the squared distance ({@code l2-squared}), which ranks results the same way and avoids
     * the square root. Lower distances are more similar. It is also accepted as {@code L2_SQUARED}.
     * </p>
     */
    EUCLIDEAN(Distance.L2_SQUARED, "L2_SQUARED"),

    /**
     * Manhattan (L1) distance, the sum of the absolute differences of each dimension.
     *
     * <p>
     * It is the distance of walking a city grid, and it is less sensitive than {@link #EUCLIDEAN} to a single
     * dimension that differs a lot.
     * </p>
     *
     * <pre>{@code
     * l1(a, b) = sum(|a[i] - b[i]|)
     * }</pre>
     *
     * <p>Lower distances are more similar.</p>
     */
    MANHATTAN(Distance.MANHATTAN, "MANHATTAN"),

    /**
     * Hamming distance, the number of dimensions whose values differ.
     *
     * <p>
     * It suits vectors of flags or hashes, such as the set of features an item has or lacks.
     * </p>
     *
     * <pre>{@code
     * hamming(a, b) = count(a[i] != b[i])
     * }</pre>
     *
     * <p>Lower distances are more similar.</p>
     */
    HAMMING(Distance.HAMMING, "HAMMING");

    private final Distance distance;

    private final String alias;

    WeaviateDistance(Distance distance, String alias) {
        this.distance = distance;
        this.alias = alias;
    }

    Distance toDistance() {
        return distance;
    }

    static WeaviateDistance from(Distance distance) {
        for (WeaviateDistance value : values()) {
            if (value.distance == distance) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported Weaviate distance: " + distance);
    }

    /**
     * Parses a distance by its name or its Weaviate name ({@code L2_SQUARED}), ignoring the case.
     *
     * @param value the distance name
     * @return the distance
     * @throws NullPointerException     when the value is null
     * @throws IllegalArgumentException when the value is not a supported distance
     */
    public static WeaviateDistance parse(String value) {
        Objects.requireNonNull(value, "value is required");
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        for (WeaviateDistance distance : values()) {
            if (distance.name().equals(normalized) || distance.alias.equals(normalized)) {
                return distance;
            }
        }
        throw new IllegalArgumentException("Unsupported Weaviate distance: " + value);
    }
}
