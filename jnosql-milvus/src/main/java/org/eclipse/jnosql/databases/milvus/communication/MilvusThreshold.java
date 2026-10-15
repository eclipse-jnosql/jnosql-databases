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

import java.util.Map;

/**
 * Translates the {@code VectorSelectQuery} threshold into Milvus search parameters.
 *
 * <p>
 * Milvus filters by range using a {@code radius}, whose meaning depends on the metric of the index:
 * </p>
 * <ul>
 *     <li>{@link MilvusMetric#COSINE} and {@link MilvusMetric#INNER_PRODUCT}: a higher score is more similar,
 *     so the threshold is the minimum score and only results with a score above it are returned;</li>
 *     <li>{@link MilvusMetric#EUCLIDEAN}: a lower distance is more similar, so the threshold is the maximum
 *     distance and only results with a distance below it are returned.</li>
 * </ul>
 * <p>
 * The boundary is exclusive in Milvus, so a result whose score equals the threshold is not returned.
 * </p>
 */
final class MilvusThreshold {

    static final String RADIUS = "radius";

    private MilvusThreshold() {
    }

    static void apply(MilvusMetric metric, float threshold, Map<String, Object> searchParams) {
        if (metric == null) {
            throw new IllegalStateException("The metric of the collection is unknown");
        }
        double radius = threshold;
        if (metric == MilvusMetric.EUCLIDEAN) {
            if (threshold < 0) {
                throw new IllegalArgumentException("The Euclidean threshold must be nonnegative");
            }
            radius = (double) threshold * threshold;
        }
        searchParams.put(RADIUS, radius);
    }
}
