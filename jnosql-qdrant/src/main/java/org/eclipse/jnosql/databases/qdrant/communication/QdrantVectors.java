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

import io.qdrant.client.QueryFactory;
import io.qdrant.client.VectorFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Points.Query;
import io.qdrant.client.grpc.Points.Vectors;
import io.qdrant.client.grpc.Points.VectorOutput;
import io.qdrant.client.grpc.Points.VectorsOutput;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.MultiVector;
import org.eclipse.jnosql.mapping.vector.SparseVector;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts the {@link Vector} hierarchy to and from the native Qdrant representations.
 * <ul>
 *     <li>{@link DenseVector}: the unnamed dense vector;</li>
 *     <li>{@link MultiVector}: the unnamed dense vector configured as a multivector;</li>
 *     <li>{@link SparseVector}: the named sparse vector {@value #SPARSE_NAME}. Qdrant does not store the
 *     dimensions of a sparse vector, so they are kept in a reserved payload key.</li>
 * </ul>
 */
final class QdrantVectors {

    static final String SPARSE_NAME = "sparse";

    static final String SPARSE_DIMENSIONS_FIELD = "_sparse_dimensions";

    private QdrantVectors() {
    }

    /** The native layout of a Qdrant collection. */
    enum Kind {
        DENSE, SPARSE, MULTI;

        static Kind of(Object vector) {
            return switch (vector) {
                case DenseVector dense -> DENSE;
                case SparseVector sparse -> SPARSE;
                case MultiVector multi -> MULTI;
                default -> throw new UnsupportedOperationException("Unsupported vector implementation: "
                        + vector.getClass().getName());
            };
        }

        boolean named() {
            return this == SPARSE;
        }
    }

    /** The number of dimensions that a collection of dense or multi vectors expects. */
    static int dimension(Vector vector) {
        return switch (vector) {
            case DenseVector dense -> dense.dimensions();
            case MultiVector multi -> multi.dimensions();
            case SparseVector sparse -> sparse.dimensions();
            default -> throw new UnsupportedOperationException("Unsupported vector implementation: "
                    + vector.getClass().getName());
        };
    }

    static Vectors toVectors(Vector vector) {
        return switch (vector) {
            case DenseVector dense -> VectorsFactory.vectors(dense.values());
            case MultiVector multi -> VectorsFactory.vectors(VectorFactory.multiVector(matrix(multi)));
            case SparseVector sparse -> VectorsFactory.namedVectors(Map.of(SPARSE_NAME,
                    VectorFactory.vector(floats(sparse.values()), integers(sparse.indices()))));
            default -> throw new UnsupportedOperationException("Unsupported vector implementation: "
                    + vector.getClass().getName());
        };
    }

    static Query toQuery(Vector vector) {
        return switch (vector) {
            case DenseVector dense -> QueryFactory.nearest(dense.values());
            case MultiVector multi -> QueryFactory.nearest(matrix(multi));
            case SparseVector sparse -> QueryFactory.nearest(floats(sparse.values()), integers(sparse.indices()));
            default -> throw new UnsupportedOperationException("Unsupported vector implementation: "
                    + vector.getClass().getName());
        };
    }

    /** The name of the vector to query, empty for the unnamed vector. */
    static Optional<String> using(Vector vector) {
        return Kind.of(vector).named() ? Optional.of(SPARSE_NAME) : Optional.empty();
    }

    static Optional<Vector> toVector(VectorsOutput vectors, Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload) {
        if (vectors == null) {
            return Optional.empty();
        }
        if (vectors.hasVector()) {
            return fromOutput(vectors.getVector(), payload);
        }
        if (vectors.hasVectors()) {
            VectorOutput sparse = vectors.getVectors().getVectorsMap().get(SPARSE_NAME);
            return sparse == null ? Optional.empty() : fromOutput(sparse, payload);
        }
        return Optional.empty();
    }

    private static Optional<Vector> fromOutput(VectorOutput output, Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload) {
        if (output.hasMultiDense()) {
            List<DenseVector> vectors = output.getMultiDense().getVectorsList().stream()
                    .map(dense -> DenseVector.of(floats(dense.getDataList())))
                    .toList();
            return vectors.isEmpty() ? Optional.empty()
                    : Optional.of(MultiVector.of(vectors.toArray(DenseVector[]::new)));
        }
        if (output.hasSparse()) {
            var sparse = output.getSparse();
            var dimensions = payload.get(SPARSE_DIMENSIONS_FIELD);
            List<Float> values = sparse.getValuesList();
            List<Integer> indices = sparse.getIndicesList();
            int size = dimensions == null ? indices.stream().mapToInt(Integer::intValue).max().orElse(0) + 1
                    : (int) dimensions.getIntegerValue();
            SparseVector.Element[] elements = new SparseVector.Element[values.size()];
            for (int index = 0; index < elements.length; index++) {
                elements[index] = SparseVector.element(indices.get(index), values.get(index));
            }
            return Optional.of(SparseVector.of(size, elements));
        }
        List<Float> data = output.hasDense() ? output.getDense().getDataList() : output.getDataList();
        return data.isEmpty() ? Optional.empty() : Optional.of(DenseVector.of(floats(data)));
    }

    private static float[][] matrix(MultiVector multi) {
        return multi.vectors().stream().map(DenseVector::values).toArray(float[][]::new);
    }

    private static List<Float> floats(float[] values) {
        List<Float> list = new ArrayList<>(values.length);
        for (float value : values) {
            list.add(value);
        }
        return list;
    }

    private static List<Integer> integers(int[] values) {
        List<Integer> list = new ArrayList<>(values.length);
        for (int value : values) {
            list.add(value);
        }
        return list;
    }

    private static float[] floats(List<Float> values) {
        float[] array = new float[values.size()];
        for (int index = 0; index < array.length; index++) {
            array[index] = values.get(index);
        }
        return array;
    }
}
