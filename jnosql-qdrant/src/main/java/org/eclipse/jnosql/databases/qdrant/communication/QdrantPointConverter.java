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

import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.VectorsOutput;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts between {@link CommunicationEntity} and Qdrant points.
 * The vector is stored as the unnamed point vector, and the name of the entity element
 * that carried it is kept in a reserved payload key.
 */
final class QdrantPointConverter {

    static final String VECTOR_FIELD = "_vector_field";

    static final String DEFAULT_VECTOR_NAME = "vector";

    private QdrantPointConverter() {
    }

    static Element vectorElement(CommunicationEntity entity) {
        List<Element> vectors = entity.elements().stream()
                .filter(element -> ValueUtil.convert(element.value()) instanceof Vector)
                .toList();
        if (vectors.size() != 1) {
            throw new IllegalArgumentException("The entity " + entity.name()
                    + " must have exactly one vector element but has " + vectors.size());
        }
        return vectors.get(0);
    }

    static float[] values(Element vectorElement) {
        Object vector = ValueUtil.convert(vectorElement.value());
        if (vector instanceof DenseVector dense) {
            return dense.values();
        }
        throw new UnsupportedOperationException("Qdrant only supports dense vectors but got: "
                + vector.getClass().getName());
    }

    static Object ensureId(CommunicationEntity entity) {
        return entity.find(QdrantPointIds.ID_FIELD)
                .map(element -> ValueUtil.convert(element.value()))
                .filter(id -> id != null)
                .orElseGet(() -> {
                    String generated = UUID.randomUUID().toString();
                    entity.add(QdrantPointIds.ID_FIELD, generated);
                    return generated;
                });
    }

    static PointStruct toPoint(CommunicationEntity entity) {
        Element vectorElement = vectorElement(entity);
        float[] values = values(vectorElement);
        Object id = ensureId(entity);
        List<Element> payloadElements = entity.elements().stream()
                .filter(element -> element != vectorElement)
                .toList();
        Map<String, Value> payload = new LinkedHashMap<>(QdrantPayloadConverter.toPayload(payloadElements));
        payload.put(QdrantPointIds.ID_FIELD, QdrantPayloadConverter.toValue(QdrantPointIds.storedValue(id)));
        payload.put(VECTOR_FIELD, ValueFactory.value(vectorElement.name()));
        return PointStruct.newBuilder()
                .setId(QdrantPointIds.of(id))
                .setVectors(VectorsFactory.vectors(values))
                .putAllPayload(payload)
                .build();
    }

    static CommunicationEntity toEntity(String collection, PointId id, Map<String, Value> payload,
                                        VectorsOutput vectors) {
        Map<String, Value> fields = new LinkedHashMap<>(payload);
        Value vectorField = fields.remove(VECTOR_FIELD);
        String vectorName = vectorField == null ? DEFAULT_VECTOR_NAME : vectorField.getStringValue();
        List<Element> elements = new ArrayList<>(QdrantPayloadConverter.toElements(fields));
        if (elements.stream().noneMatch(element -> QdrantPointIds.ID_FIELD.equals(element.name()))) {
            elements.add(0, Element.of(QdrantPointIds.ID_FIELD, String.valueOf(QdrantPointIds.value(id))));
        }
        if (vectors != null && vectors.hasVector()) {
            float[] values = denseValues(vectors);
            if (values.length > 0) {
                elements.add(Element.of(vectorName, DenseVector.of(values)));
            }
        }
        return CommunicationEntity.of(collection, elements);
    }

    private static float[] denseValues(VectorsOutput vectors) {
        var output = vectors.getVector();
        List<Float> data = output.hasDense() ? output.getDense().getDataList() : output.getDataList();
        float[] values = new float[data.size()];
        for (int index = 0; index < values.length; index++) {
            values[index] = data.get(index);
        }
        return values;
    }
}
