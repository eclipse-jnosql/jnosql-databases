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
import io.weaviate.client6.v1.api.collections.WeaviateObject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Converts between {@link CommunicationEntity} and Weaviate objects.
 * Null values are not written; Weaviate reports them as missing and indexes them as null.
 */
final class WeaviateObjectConverter {

    private WeaviateObjectConverter() {
    }

    static boolean isNull(Element element) {
        return WeaviateValueWriteDecorator.convert(element.value()) == null;
    }

    static Object storable(Element element) {
        return WeaviateValueConverter.convert(element.value());
    }

    static Element vectorElement(CommunicationEntity entity) {
        List<Element> vectors = entity.elements().stream()
                .filter(element -> WeaviateValueWriteDecorator.convert(element.value()) instanceof Vector)
                .toList();
        if (vectors.size() != 1) {
            throw new IllegalArgumentException("The entity " + entity.name()
                    + " must have exactly one vector element but has " + vectors.size());
        }
        return vectors.get(0);
    }

    static float[] values(Element vectorElement) {
        Object vector = WeaviateValueWriteDecorator.convert(vectorElement.value());
        if (vector instanceof DenseVector dense) {
            return dense.values();
        }
        throw new UnsupportedOperationException("Weaviate only supports dense vectors but got: "
                + vector.getClass().getName());
    }

    static String ensureId(CommunicationEntity entity) {
        Optional<Object> id = entity.find(WeaviateNames.ID_FIELD)
                .map(element -> WeaviateValueWriteDecorator.convert(element.value()));
        if (id.isPresent()) {
            return id.get().toString();
        }
        String generated = UUID.randomUUID().toString();
        entity.add(WeaviateNames.ID_FIELD, generated);
        return generated;
    }

    static Map<String, Object> properties(CommunicationEntity entity, Element vectorElement) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Element element : entity.elements()) {
            if (element == vectorElement || isNull(element)) {
                continue;
            }
            properties.put(WeaviateNames.property(element.name()), storable(element));
        }
        return properties;
    }

    static WeaviateObject<Map<String, Object>> toObject(CommunicationEntity entity, Element vectorElement) {
        String id = ensureId(entity);
        Map<String, Object> properties = properties(entity, vectorElement);
        String vectorName = WeaviateNames.vectorName(vectorElement.name());
        return WeaviateObject.of(builder -> builder
                .uuid(WeaviateNames.objectId(id))
                .properties(properties)
                .vectors(Vectors.of(vectorName, values(vectorElement))));
    }

    static CommunicationEntity toEntity(String name, WeaviateObject<Map<String, Object>> object,
                                        WeaviateCollectionSchema schema) {
        List<Element> elements = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Map<String, Object> properties = object.properties() == null ? Map.of() : object.properties();
        properties.forEach((property, value) -> {
            if (value == null) {
                return;
            }
            Object converted = WeaviateValueConverter.fromStored(value, schema.propertyType(property));
            elements.add(Element.of(WeaviateNames.field(property), converted));
            seen.add(property);
        });
        if (!seen.contains(WeaviateNames.ID_PROPERTY) && object.uuid() != null) {
            elements.add(Element.of(WeaviateNames.ID_FIELD, object.uuid()));
        }
        Vectors vectors = object.vectors();
        if (vectors != null && vectors.contains(schema.vectorName())) {
            elements.add(Element.of(schema.vectorName(), DenseVector.of(vectors.getSingle(schema.vectorName()))));
        }
        return CommunicationEntity.of(name, elements);
    }
}
