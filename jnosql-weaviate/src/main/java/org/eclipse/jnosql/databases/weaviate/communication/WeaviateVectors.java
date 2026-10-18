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

import io.weaviate.client6.v1.api.collections.Vectors;
import io.weaviate.client6.v1.api.collections.query.Target;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.MultiVector;
import org.eclipse.jnosql.mapping.vector.SparseVector;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.Arrays;
import java.util.List;

/**
 * Maps the {@link Vector} implementations to the native Weaviate representations:
 * <ul>
 *     <li>{@link DenseVector}: a single-vector named vector ({@code float[]});</li>
 *     <li>{@link MultiVector}: a multi-vector named vector ({@code float[][]}) in an HNSW index with
 *     multi-vector enabled, searched with the MaxSim aggregation;</li>
 *     <li>{@link SparseVector}: not supported, Weaviate has no native sparse vector (its sparse BM25 index is
 *     built from text and is not a vector that can be stored or queried), and it is not emulated.</li>
 * </ul>
 */
final class WeaviateVectors {

    enum Kind {
        DENSE, MULTI
    }

    private WeaviateVectors() {
    }

    static Kind kind(Vector vector) {
        return switch (vector) {
            case DenseVector ignored -> Kind.DENSE;
            case MultiVector ignored -> Kind.MULTI;
            case SparseVector ignored -> throw new UnsupportedOperationException(
                    "Weaviate does not support SparseVector: it has no native sparse vector representation");
            default -> throw new UnsupportedOperationException("Weaviate does not support the vector type: "
                    + vector.getClass().getName());
        };
    }

    static int dimension(Vector vector) {
        return switch (vector) {
            case DenseVector dense -> dense.dimensions();
            case MultiVector multi -> multi.dimensions();
            default -> throw new UnsupportedOperationException("Weaviate does not support the vector type: "
                    + vector.getClass().getName());
        };
    }

    static Vectors toVectors(String name, Vector vector) {
        return switch (vector) {
            case DenseVector dense -> Vectors.of(name, dense.values());
            case MultiVector multi -> Vectors.of(name, matrix(multi));
            default -> throw new UnsupportedOperationException("Weaviate does not support the vector type: "
                    + vector.getClass().getName());
        };
    }

    static Target.VectorTarget toTarget(String name, Vector vector) {
        return switch (vector) {
            case DenseVector dense -> Target.vector(name, dense.values());
            case MultiVector multi -> Target.vector(name, matrix(multi));
            default -> throw new UnsupportedOperationException("Weaviate does not support the vector type: "
                    + vector.getClass().getName());
        };
    }

    static Vector fromVectors(Kind kind, Vectors vectors, String name) {
        if (kind == Kind.MULTI) {
            float[][] matrix = vectors.getMulti(name);
            List<DenseVector> nested = Arrays.stream(matrix).map(DenseVector::of).toList();
            return MultiVector.of(nested.toArray(DenseVector[]::new));
        }
        return DenseVector.of(vectors.getSingle(name));
    }

    private static float[][] matrix(MultiVector multi) {
        return multi.vectors().stream().map(DenseVector::values).toArray(float[][]::new);
    }
}
