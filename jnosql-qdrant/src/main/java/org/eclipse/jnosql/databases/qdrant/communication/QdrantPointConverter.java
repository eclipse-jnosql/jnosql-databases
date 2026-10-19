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
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.VectorsOutput;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.SparseVector;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                .filter(element -> QdrantValueWriteDecorator.convert(element.value()) instanceof Vector)
                .toList();
        if (vectors.size() != 1) {
            throw new IllegalArgumentException("The entity " + entity.name()
                    + " must have exactly one vector element but has " + vectors.size());
        }
        return vectors.get(0);
    }

    static Vector vector(Element vectorElement) {
        return (Vector) QdrantValueWriteDecorator.convert(vectorElement.value());
    }

    static void putSparseDimensions(Map<String, Value> payload, Vector vector) {
        if (vector instanceof SparseVector sparse) {
            payload.put(QdrantVectors.SPARSE_DIMENSIONS_FIELD, ValueFactory.value(sparse.dimensions()));
        }
    }

    static Object ensureId(CommunicationEntity entity) {
        return entity.find(QdrantPointIds.ID_FIELD)
                .map(element -> QdrantValueWriteDecorator.convert(element.value()))
                .filter(id -> id != null)
                .orElseGet(() -> {
                    String generated = UUID.randomUUID().toString();
                    entity.add(QdrantPointIds.ID_FIELD, generated);
                    return generated;
                });
    }

    static PointStruct toPoint(CommunicationEntity entity) {
        Element vectorElement = vectorElement(entity);
        Vector vector = vector(vectorElement);
        Object id = ensureId(entity);
        List<Element> payloadElements = entity.elements().stream()
                .filter(element -> element != vectorElement)
                .toList();
        Map<String, Value> payload = new LinkedHashMap<>(QdrantPayloadConverter.toPayload(payloadElements));
        payload.put(QdrantPointIds.ID_FIELD, QdrantPayloadConverter.toValue(QdrantPointIds.storedValue(id)));
        payload.put(VECTOR_FIELD, ValueFactory.value(vectorElement.name()));
        putSparseDimensions(payload, vector);
        return PointStruct.newBuilder()
                .setId(QdrantPointIds.of(id))
                .setVectors(QdrantVectors.toVectors(vector))
                .putAllPayload(payload)
                .build();
    }

    static CommunicationEntity toEntity(String collection, PointId id, Map<String, Value> payload,
                                        VectorsOutput vectors) {
        Map<String, Value> fields = new LinkedHashMap<>(payload);
        Value vectorField = fields.remove(VECTOR_FIELD);
        Value sparseDimensions = fields.get(QdrantVectors.SPARSE_DIMENSIONS_FIELD);
        Optional<Vector> vector = QdrantVectors.toVector(vectors, sparseDimensions == null ? Map.of()
                : Map.of(QdrantVectors.SPARSE_DIMENSIONS_FIELD, sparseDimensions));
        if (vector.orElse(null) instanceof SparseVector) {
            fields.remove(QdrantVectors.SPARSE_DIMENSIONS_FIELD);
        }
        String vectorName = vectorField == null ? DEFAULT_VECTOR_NAME : vectorField.getStringValue();
        List<Element> elements = new ArrayList<>(QdrantPayloadConverter.toElements(fields));
        if (elements.stream().noneMatch(element -> QdrantPointIds.ID_FIELD.equals(element.name()))) {
            elements.add(0, Element.of(QdrantPointIds.ID_FIELD, QdrantPointIds.value(id)));
        }
        vector.ifPresent(value -> elements.add(Element.of(vectorName, value)));
        return CommunicationEntity.of(collection, elements);
    }
}
