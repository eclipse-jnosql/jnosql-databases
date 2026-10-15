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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MilvusThresholdTest {

    @Test
    @DisplayName("Should send the threshold unchanged for similarity metrics")
    void shouldApplySimilarity() {
        for (MilvusMetric metric : new MilvusMetric[]{MilvusMetric.COSINE, MilvusMetric.INNER_PRODUCT}) {
            Map<String, Object> params = new HashMap<>();
            MilvusThreshold.apply(metric, 0.5f, params);

            assertThat(params).containsEntry("radius", 0.5d);
        }
    }

    @Test
    @DisplayName("Should square the Euclidean distance because Milvus L2 scores are squared")
    void shouldSquareEuclidean() {
        Map<String, Object> params = new HashMap<>();
        MilvusThreshold.apply(MilvusMetric.EUCLIDEAN, 2f, params);

        assertThat(params).containsEntry("radius", 4d);
    }

    @Test
    @DisplayName("Should accept a zero Euclidean threshold")
    void shouldAcceptZeroEuclidean() {
        Map<String, Object> params = new HashMap<>();
        MilvusThreshold.apply(MilvusMetric.EUCLIDEAN, 0f, params);

        assertThat(params).containsEntry("radius", 0d);
    }

    @Test
    @DisplayName("Should reject a negative Euclidean threshold")
    void shouldRejectNegativeEuclidean() {
        assertThatThrownBy(() -> MilvusThreshold.apply(MilvusMetric.EUCLIDEAN, -1f, new HashMap<>()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject an unknown metric")
    void shouldRejectUnknownMetric() {
        assertThatThrownBy(() -> MilvusThreshold.apply(null, 0.5f, new HashMap<>()))
                .isInstanceOf(IllegalStateException.class);
    }
}
