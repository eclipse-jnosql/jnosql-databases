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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.data.BaseVector;
import io.milvus.v2.service.vector.request.data.EmbeddingList;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.request.data.SparseFloatVec;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.MultiVector;
import org.eclipse.jnosql.mapping.vector.SparseVector;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Maps the {@link Vector} implementations to the native Milvus representations:
 * <ul>
 *     <li>{@link DenseVector}: a {@code FloatVector} field;</li>
 *     <li>{@link SparseVector}: a {@code SparseFloatVector} field, plus the reserved dynamic field
 *     {@value #SPARSE_DIMENSIONS} because Milvus does not store the dimensions of a sparse vector;</li>
 *     <li>{@link MultiVector}: an array of structs, each one with a {@code FloatVector}
 *     {@value #MULTI_FIELD} sub-field, searched as an embedding list with a MAX_SIM metric.</li>
 * </ul>
 */
final class MilvusVectors {

    static final String SPARSE_DIMENSIONS = "_sparse_dimensions";

    static final String MULTI_FIELD = "vector";

    private static final int MAX_CAPACITY = 4096;

    enum Kind {
        DENSE, SPARSE, MULTI
    }

    private MilvusVectors() {
    }

    static Kind kind(Vector vector) {
        return switch (vector) {
            case DenseVector ignored -> Kind.DENSE;
            case SparseVector ignored -> Kind.SPARSE;
            case MultiVector ignored -> Kind.MULTI;
            default -> throw new UnsupportedOperationException("Milvus does not support the vector type: "
                    + vector.getClass().getName());
        };
    }

    static int dimension(Vector vector) {
        return switch (vector) {
            case DenseVector dense -> dense.dimensions();
            case SparseVector ignored -> 0;
            case MultiVector multi -> multi.dimensions();
            default -> throw new UnsupportedOperationException("Milvus does not support the vector type: "
                    + vector.getClass().getName());
        };
    }

    static DataType dataType(Kind kind) {
        return switch (kind) {
            case DENSE -> DataType.FloatVector;
            case SPARSE -> DataType.SparseFloatVector;
            case MULTI -> DataType.Array;
        };
    }

    static int maxCapacity() {
        return MAX_CAPACITY;
    }

    static IndexParam.IndexType indexType(Kind kind) {
        return switch (kind) {
            case DENSE -> IndexParam.IndexType.AUTOINDEX;
            case SPARSE -> IndexParam.IndexType.SPARSE_INVERTED_INDEX;
            case MULTI -> IndexParam.IndexType.HNSW;
        };
    }

    static IndexParam.MetricType metricType(Kind kind, MilvusMetric metric) {
        return switch (kind) {
            case DENSE -> metric.toMetricType();
            case SPARSE -> IndexParam.MetricType.IP;
            case MULTI -> switch (metric) {
                case COSINE -> IndexParam.MetricType.MAX_SIM_COSINE;
                case EUCLIDEAN -> IndexParam.MetricType.MAX_SIM_L2;
                case INNER_PRODUCT -> IndexParam.MetricType.MAX_SIM_IP;
            };
        };
    }

    static MilvusMetric effectiveMetric(Kind kind, MilvusMetric metric) {
        return kind == Kind.SPARSE ? MilvusMetric.INNER_PRODUCT : metric;
    }

    static String indexField(Kind kind, String field) {
        return kind == Kind.MULTI ? field + "[" + MULTI_FIELD + "]" : field;
    }

    static JsonElement toJson(Vector vector) {
        JsonArray array = new JsonArray();
        switch (vector) {
            case DenseVector dense -> {
                for (float value : dense.values()) {
                    array.add(value);
                }
                return array;
            }
            case SparseVector sparse -> {
                JsonObject object = new JsonObject();
                int[] indices = sparse.indices();
                float[] values = sparse.values();
                for (int index = 0; index < indices.length; index++) {
                    object.addProperty(String.valueOf(indices[index]), values[index]);
                }
                return object;
            }
            case MultiVector multi -> {
                for (DenseVector nested : multi.vectors()) {
                    JsonArray values = new JsonArray();
                    for (float value : nested.values()) {
                        values.add(value);
                    }
                    JsonObject struct = new JsonObject();
                    struct.add(MULTI_FIELD, values);
                    array.add(struct);
                }
                return array;
            }
            default -> throw new UnsupportedOperationException("Milvus does not support the vector type: "
                    + vector.getClass().getName());
        }
    }

    static BaseVector toQuery(Vector vector) {
        return switch (vector) {
            case DenseVector dense -> new FloatVec(dense.values());
            case SparseVector sparse -> {
                SortedMap<Long, Float> map = new TreeMap<>();
                for (int index = 0; index < sparse.indices().length; index++) {
                    map.put((long) sparse.indices()[index], sparse.values()[index]);
                }
                yield new SparseFloatVec(map);
            }
            case MultiVector multi -> {
                EmbeddingList list = new EmbeddingList();
                multi.vectors().forEach(nested -> list.add(new FloatVec(nested.values())));
                yield list;
            }
            default -> throw new UnsupportedOperationException("Milvus does not support the vector type: "
                    + vector.getClass().getName());
        };
    }

    static Vector fromNative(Kind kind, Object value, Object dimensions) {
        return switch (kind) {
            case DENSE -> DenseVector.of(floats(value));
            case SPARSE -> sparse(value, dimensions);
            case MULTI -> multi(value);
        };
    }

    private static SparseVector sparse(Object value, Object dimensions) {
        Map<?, ?> map = value instanceof JsonObject object ? object.asMap() : (Map<?, ?>) value;
        SortedMap<Integer, Float> sorted = new TreeMap<>();
        map.forEach((key, item) -> sorted.put(Integer.parseInt(key.toString()), number(item).floatValue()));
        SparseVector.Element[] elements = sorted.entrySet().stream()
                .map(entry -> SparseVector.element(entry.getKey(), entry.getValue()))
                .toArray(SparseVector.Element[]::new);
        int size = dimensions == null ? sorted.isEmpty() ? 0 : sorted.lastKey() + 1 : number(dimensions).intValue();
        return SparseVector.of(size, elements);
    }

    private static MultiVector multi(Object value) {
        List<?> items = value instanceof JsonArray array ? array.asList() : (List<?>) value;
        List<DenseVector> vectors = new ArrayList<>();
        for (Object item : items) {
            Object nested = item instanceof JsonObject object ? object.get(MULTI_FIELD)
                    : ((Map<?, ?>) item).get(MULTI_FIELD);
            vectors.add(DenseVector.of(floats(nested)));
        }
        return MultiVector.of(vectors.toArray(DenseVector[]::new));
    }

    private static Number number(Object value) {
        if (value instanceof Number number) {
            return number;
        }
        if (value instanceof JsonElement element) {
            return element.getAsDouble();
        }
        return Double.parseDouble(value.toString());
    }

    private static float[] floats(Object value) {
        List<?> list = value instanceof JsonArray array ? array.asList() : (List<?>) value;
        float[] values = new float[list.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = number(list.get(index)).floatValue();
        }
        return values;
    }
}
