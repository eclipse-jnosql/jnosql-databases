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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MilvusMetricTest {

    @Nested
    @DisplayName("When parsing a metric")
    class WhenTheParsing {

        @Test
        @DisplayName("Should parse the name ignoring the case")
        void shouldParseName() {
            assertThat(MilvusMetric.parse("cosine")).isEqualTo(MilvusMetric.COSINE);
            assertThat(MilvusMetric.parse(" Euclidean ")).isEqualTo(MilvusMetric.EUCLIDEAN);
            assertThat(MilvusMetric.parse("INNER_PRODUCT")).isEqualTo(MilvusMetric.INNER_PRODUCT);
        }

        @Test
        @DisplayName("Should parse the Milvus names")
        void shouldParseAliases() {
            assertThat(MilvusMetric.parse("l2")).isEqualTo(MilvusMetric.EUCLIDEAN);
            assertThat(MilvusMetric.parse("IP")).isEqualTo(MilvusMetric.INNER_PRODUCT);
        }

        @Test
        @DisplayName("Should reject an unknown metric")
        void shouldRejectUnknown() {
            assertThatIllegalArgumentException().isThrownBy(() -> MilvusMetric.parse("hamming"));
        }

        @Test
        @DisplayName("Should reject a null metric")
        void shouldRejectNull() {
            assertThatNullPointerException().isThrownBy(() -> MilvusMetric.parse(null));
        }
    }

    @Nested
    @DisplayName("When converting to the Milvus metric")
    class WhenTheConversion {

        @Test
        @DisplayName("Should map every metric")
        void shouldMap() {
            assertThat(MilvusMetric.COSINE.toMetricType()).isEqualTo(MetricType.COSINE);
            assertThat(MilvusMetric.EUCLIDEAN.toMetricType()).isEqualTo(MetricType.L2);
            assertThat(MilvusMetric.INNER_PRODUCT.toMetricType()).isEqualTo(MetricType.IP);
        }
    }
}
