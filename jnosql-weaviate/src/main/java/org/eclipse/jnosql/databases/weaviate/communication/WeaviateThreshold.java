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

/**
 * Translates the {@code VectorSelectQuery} threshold into the Weaviate near-vector {@code distance} limit.
 *
 * <p>
 * Weaviate always filters by the maximum distance a result may have, and the distance has a different scale
 * for each distance of the vector index:
 * </p>
 * <ul>
 *     <li>{@link WeaviateDistance#COSINE}: Weaviate measures {@code 1 - cosine similarity}, so a threshold
 *     of minimum similarity {@code s} becomes the maximum distance {@code 1 - s};</li>
 *     <li>{@link WeaviateDistance#DOT}: Weaviate measures the negative dot product, so a threshold of minimum
 *     product {@code p} becomes the maximum distance {@code -p};</li>
 *     <li>{@link WeaviateDistance#EUCLIDEAN}: Weaviate measures the squared distance, so a threshold of maximum
 *     distance {@code d} becomes {@code d * d};</li>
 *     <li>{@link WeaviateDistance#MANHATTAN} and {@link WeaviateDistance#HAMMING}: the threshold is the maximum
 *     distance and it is sent as is.</li>
 * </ul>
 * <p>
 * The boundary is inclusive, so a result whose distance equals the limit is returned.
 * </p>
 */
final class WeaviateThreshold {

    private WeaviateThreshold() {
    }

    static float maxDistance(WeaviateDistance distance, float threshold) {
        if (distance == null) {
            throw new IllegalStateException("The distance of the collection is unknown");
        }
        return switch (distance) {
            case COSINE -> 1f - threshold;
            case DOT -> -threshold;
            case EUCLIDEAN -> {
                if (threshold < 0) {
                    throw new IllegalArgumentException("The Euclidean threshold must be nonnegative");
                }
                yield threshold * threshold;
            }
            case MANHATTAN, HAMMING -> {
                if (threshold < 0) {
                    throw new IllegalArgumentException("The " + distance + " threshold must be nonnegative");
                }
                yield threshold;
            }
        };
    }
}
