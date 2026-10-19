/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.PointIdFactory;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.NamedVectorsOutput;
import io.qdrant.client.grpc.Points.VectorOutput;
import io.qdrant.client.grpc.Points.VectorsOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantPointConverterTest {

    private static final String SPARSE_DIMENSIONS = QdrantVectors.SPARSE_DIMENSIONS_FIELD;

    private VectorsOutput dense() {
        return VectorsOutput.newBuilder().setVector(VectorOutput.newBuilder()
                .setDense(io.qdrant.client.grpc.Points.DenseVector.newBuilder().addData(1f).addData(2f))).build();
    }

    private VectorsOutput sparse() {
        return VectorsOutput.newBuilder().setVectors(NamedVectorsOutput.newBuilder().putVectors("sparse",
                VectorOutput.newBuilder().setSparse(io.qdrant.client.grpc.Points.SparseVector.newBuilder()
                        .addIndices(2).addIndices(8).addValues(7f).addValues(3f)).build())).build();
    }

    @Nested
    @DisplayName("When converting a point to an entity")
    class WhenConverting {

        @Test
        @DisplayName("Should keep a user property named like the sparse metadata on a dense point")
        void shouldKeepPropertyOnDense() {
            var entity = QdrantPointConverter.toEntity("c", PointIdFactory.id(1L),
                    Map.of(SPARSE_DIMENSIONS, ValueFactory.value(7L)), dense());

            assertThat(entity.find(SPARSE_DIMENSIONS).orElseThrow().get()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Should consume the sparse metadata on a sparse point")
        void shouldConsumeOnSparse() {
            var entity = QdrantPointConverter.toEntity("c", PointIdFactory.id(1L),
                    Map.of(SPARSE_DIMENSIONS, ValueFactory.value(10L)), sparse());

            assertThat(entity.find(SPARSE_DIMENSIONS)).isEmpty();
            assertThat(entity.elements()).extracting(e -> e.get())
                    .anyMatch(v -> v instanceof org.eclipse.jnosql.mapping.vector.SparseVector sv
                            && sv.dimensions() == 10);
        }
    }
}
