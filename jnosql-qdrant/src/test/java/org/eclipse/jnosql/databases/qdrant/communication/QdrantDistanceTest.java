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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class QdrantDistanceTest {

    @Nested
    @DisplayName("When converting a configuration value")
    class WhenTheConversion {

        @ParameterizedTest(name = "\"{0}\" should be {1}")
        @CsvSource({
                "Cosine, COSINE",
                "cosine, COSINE",
                "COSINE, COSINE",
                "Euclid, EUCLID",
                "euclid, EUCLID",
                "Dot, DOT",
                "dot, DOT",
                "Manhattan, MANHATTAN",
                "manhattan, MANHATTAN"
        })
        @DisplayName("Should ignore the case of supported metrics")
        void shouldParseIgnoringCase(String value, QdrantDistance expected) {
            assertThat(QdrantDistance.parse(value)).isEqualTo(expected);
        }

        @Test
        @DisplayName("Should ignore surrounding whitespace")
        void shouldParseTrimmingWhitespace() {
            assertThat(QdrantDistance.parse("  cosine ")).isEqualTo(QdrantDistance.COSINE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "euclidean", "l2", "unknown"})
        @DisplayName("Should reject unsupported metrics")
        void shouldRejectUnsupportedValue(String value) {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> QdrantDistance.parse(value))
                    .withMessageContaining("Unsupported Qdrant distance");
        }

        @Test
        @DisplayName("Should reject a null value")
        void shouldRejectNullValue() {
            assertThatNullPointerException().isThrownBy(() -> QdrantDistance.parse(null));
        }
    }

    @Nested
    @DisplayName("When mapping to the Qdrant client")
    class WhenTheMapping {

        @Test
        @DisplayName("Should map each metric to the matching Qdrant distance")
        void shouldMapToQdrantDistance() {
            assertThat(QdrantDistance.COSINE.toDistance()).isEqualTo(Distance.Cosine);
            assertThat(QdrantDistance.EUCLID.toDistance()).isEqualTo(Distance.Euclid);
            assertThat(QdrantDistance.DOT.toDistance()).isEqualTo(Distance.Dot);
            assertThat(QdrantDistance.MANHATTAN.toDistance()).isEqualTo(Distance.Manhattan);
        }
    }
}
