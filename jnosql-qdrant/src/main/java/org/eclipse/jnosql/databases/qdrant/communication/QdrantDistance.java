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
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.grpc.Collections.Distance;

import java.util.Locale;
import java.util.Objects;

/**
 * Defines the distance metrics supported by Qdrant vector collections.
 *
 * <p>
 * The distance metric determines how Qdrant compares stored vectors with a
 * query vector. The metric is part of the vector collection/index configuration
 * and is not selected per search operation.
 * </p>
 *
 * <p>
 * A typical configuration may use:
 * </p>
 *
 * <pre>{@code
 * QdrantDistance distance = QdrantDistance.COSINE;
 * }</pre>
 *
 * <p>
 * Conceptually:
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
 * Each value maps to the equivalent Qdrant client {@link Distance}.
 * </p>
 */
public enum QdrantDistance {


    /**
     * Cosine distance.
     *
     * <p>
     * Cosine focuses mainly on the direction of the vectors rather than their
     * magnitude. This is useful when the relative pattern matters more than the
     * absolute amount.
     * </p>
     *
     * <p>
     * Consider a recommendation system where vector dimensions represent movie
     * genres:
     * </p>
     *
     * <pre>
     *                 Action  Comedy  Drama
     *
     * Alice              10       2      1
     * Bob               100      20     10
     * Carol               2      10      8
     * </pre>
     *
     * <p>
     * Alice and Bob have very different numbers of watched movies, but their
     * preference proportions are essentially the same. Both strongly prefer
     * action movies over comedy and drama.
     * </p>
     *
     * <p>
     * Cosine therefore considers Alice and Bob highly similar because their
     * vectors point in the same direction. Carol has a different preference
     * pattern and would be considered less similar.
     * </p>
     *
     * <p>
     * This behavior is often useful for embeddings and recommendation systems,
     * where the pattern or semantic direction matters more than the absolute
     * magnitude.
     * </p>
     *
     * <pre>
     *                 A · B
     * cosine(A,B) = ----------
     *                |A| |B|
     * </pre>
     */
    COSINE(Distance.Cosine),
    /**
     * Euclidean distance, also known as L2 distance.
     *
     * <p>
     * Euclidean distance measures the straight-line distance between two vectors.
     * Unlike cosine, the magnitude of the vectors matters.
     * </p>
     *
     * <p>
     * Consider the same movie-preference example:
     * </p>
     *
     * <pre>
     *                 Action  Comedy  Drama
     *
     * Alice              10       2      1
     * Bob               100      20     10
     * Carol              11       3      1
     * </pre>
     *
     * <p>
     * Alice and Bob have essentially the same preference proportions, so cosine
     * would consider them very similar.
     * </p>
     *
     * <p>
     * Euclidean distance behaves differently because it considers the absolute
     * values. Bob is much farther from Alice because he watched many more movies
     * in every category.
     * </p>
     *
     * <p>
     * Carol, on the other hand, is very close to Alice because their absolute
     * values are also similar:
     * </p>
     *
     * <pre>
     * Alice  [10, 2, 1]
     * Carol  [11, 3, 1]
     * </pre>
     *
     * <p>
     * Therefore, Euclidean distance is useful when the magnitude or intensity
     * represented by the vector is meaningful, not only its direction.
     * </p>
     *
     * <pre>
     * euclid(A,B) = sqrt(
     *     (A1 - B1)^2 +
     *     (A2 - B2)^2 +
     *     ... +
     *     (An - Bn)^2
     * )
     * </pre>
     */
    EUCLID(Distance.Euclid),
    /**
     * Dot-product similarity.
     *
     * <p>
     * Dot product considers both direction and magnitude. Two users with the same
     * preference pattern can receive different scores if one has much stronger
     * activity than the other.
     * </p>
     *
     * <p>
     * Consider:
     * </p>
     *
     * <pre>
     *                 Action  Comedy  Drama
     *
     * Query               1       1      1
     * Alice              10       2      1
     * Bob               100      20     10
     * Carol               2      10      8
     * </pre>
     *
     * <p>
     * Alice and Bob have the same preference proportions, but Bob has a much larger
     * magnitude because he watched many more movies.
     * </p>
     *
     * <p>
     * With dot product, Bob produces a much larger score than Alice:
     * </p>
     *
     * <pre>
     * Query · Alice = 1*10 + 1*2 + 1*1   = 13
     * Query · Bob   = 1*100 + 1*20 + 1*10 = 130
     * </pre>
     *
     * <p>
     * This means dot product can be useful when both the preference direction and
     * the strength or intensity represented by the vector matter.
     * </p>
     *
     * <pre>
     * dot(A,B) =
     *     A1 * B1 +
     *     A2 * B2 +
     *     ... +
     *     An * Bn
     * </pre>
     */
    DOT(Distance.Dot),
    /**
     * Manhattan distance, also known as L1 distance.
     *
     * <p>
     * Manhattan distance adds the absolute difference between each dimension.
     * Each movie category contributes independently to the final distance.
     * </p>
     *
     * <p>
     * Consider:
     * </p>
     *
     * <pre>
     *                 Action  Comedy  Drama
     *
     * Alice              10       2      1
     * Bob               100      20     10
     * Carol              11       3      1
     * </pre>
     *
     * <p>
     * Carol is close to Alice because the difference in each category is small:
     * </p>
     *
     * <pre>
     * |10 - 11| + |2 - 3| + |1 - 1|
     * = 1 + 1 + 0
     * = 2
     * </pre>
     *
     * <p>
     * Bob is much farther away:
     * </p>
     *
     * <pre>
     * |10 - 100| + |2 - 20| + |1 - 10|
     * = 90 + 18 + 9
     * = 117
     * </pre>
     *
     * <p>
     * Unlike cosine, Manhattan distance cares about the absolute difference in
     * activity. Unlike Euclidean distance, it simply adds the differences instead
     * of combining them using squared geometric distance.
     * </p>
     *
     * <p>
     * It is useful when each dimension represents an independent difference that
     * should contribute directly to the total distance.
     * </p>
     *
     * <pre>
     * manhattan(A,B) =
     *     |A1 - B1| +
     *     |A2 - B2| +
     *     ... +
     *     |An - Bn|
     * </pre>
     */
    MANHATTAN(Distance.Manhattan);

    private final Distance distance;

    QdrantDistance(Distance distance) {
        this.distance = distance;
    }

    /**
     * @return the Qdrant client representation of this metric
     */
    Distance toDistance() {
        return distance;
    }

    /**
     * Parses a configuration value, ignoring case and surrounding whitespace.
     * The Qdrant name for the metric ({@code Cosine}, {@code Euclid}, {@code Dot}, {@code Manhattan}) is accepted.
     *
     * @param value the configuration value
     * @return the matching metric
     * @throws NullPointerException     when the value is null
     * @throws IllegalArgumentException when the value is not a supported metric
     */
    public static QdrantDistance parse(String value) {
        Objects.requireNonNull(value, "value is required");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (QdrantDistance candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("Unsupported Qdrant distance: '" + value
                + "'. Supported values: COSINE, EUCLID, DOT, MANHATTAN");
    }
}
