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

import io.weaviate.client6.v1.api.collections.CollectionConfig;
import io.weaviate.client6.v1.api.collections.DataType;
import io.weaviate.client6.v1.api.collections.Property;
import io.weaviate.client6.v1.api.collections.Tokenization;
import io.weaviate.client6.v1.api.collections.VectorConfig;
import io.weaviate.client6.v1.api.collections.vectorindex.Hnsw;
import io.weaviate.client6.v1.api.collections.vectorindex.MultiVector;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The description of an existing Weaviate collection: its properties, its single self-provided vector and
 * the distance of the vector index. It also builds the collection from the first {@link CommunicationEntity}.
 */
final class WeaviateCollectionSchema {

    private final String collection;

    private final Map<String, String> properties;

    private final String vectorName;

    private final WeaviateDistance distance;

    private final WeaviateVectors.Kind kind;

    private WeaviateCollectionSchema(String collection, Map<String, String> properties, String vectorName,
                                     WeaviateDistance distance, WeaviateVectors.Kind kind) {
        this.collection = collection;
        this.properties = properties;
        this.vectorName = vectorName;
        this.distance = distance;
        this.kind = kind;
    }

    WeaviateVectors.Kind kind() {
        return kind;
    }

    String vectorName() {
        return vectorName;
    }

    WeaviateDistance distance() {
        return distance;
    }

    java.util.Set<String> propertyNames() {
        return properties.keySet();
    }

    boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    String propertyType(String name) {
        return properties.get(name);
    }

    static WeaviateCollectionSchema of(CollectionConfig config, WeaviateCollectionSettings settings) {
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property property : config.properties()) {
            properties.put(property.propertyName(), property.dataTypes().get(0));
        }
        if (config.vectors().size() != 1) {
            throw new IllegalStateException("The Weaviate collection " + config.collectionName()
                    + " must have exactly one named vector to be used by JNoSQL but has " + config.vectors().size());
        }
        Map.Entry<String, VectorConfig> vector = config.vectors().entrySet().iterator().next();
        WeaviateDistance distance = settings.distance();
        WeaviateVectors.Kind kind = WeaviateVectors.Kind.DENSE;
        if (vector.getValue().vectorIndex() instanceof Hnsw hnsw) {
            if (hnsw.distance() != null) {
                distance = WeaviateDistance.from(hnsw.distance());
            }
            if (hnsw.multiVector() != null && hnsw.multiVector().enabled()) {
                kind = WeaviateVectors.Kind.MULTI;
            }
        }
        return new WeaviateCollectionSchema(config.collectionName(), properties, vector.getKey(), distance, kind);
    }

    static CollectionConfig create(String collection, CommunicationEntity entity, Element vector,
                                   WeaviateCollectionSettings settings) {
        List<Property> properties = new ArrayList<>();
        for (Element element : entity.elements()) {
            if (element == vector || WeaviateObjectConverter.isNull(element)) {
                continue;
            }
            String name = WeaviateNames.property(element.name());
            Object value = WeaviateObjectConverter.storable(element);
            String type = WeaviateValueConverter.dataType(value);
            if (type == null) {
                throw new IllegalArgumentException("The type of the property " + element.name()
                        + " cannot be inferred from an empty collection");
            }
            properties.add(property(name, type));
        }
        String vectorName = WeaviateNames.vectorName(vector.name());
        boolean multi = WeaviateVectors.kind(vectorOf(vector)) == WeaviateVectors.Kind.MULTI;
        return CollectionConfig.of(collection, builder -> builder
                .properties(properties)
                .invertedIndex(index -> index.indexNulls(true))
                .vectorConfig(VectorConfig.selfProvided(vectorName,
                        self -> self.vectorIndex(Hnsw.of(hnsw -> {
                            hnsw.distance(settings.distance().toDistance());
                            if (multi) {
                                hnsw.multiVector(MultiVector.of());
                            }
                            return hnsw;
                        })))));
    }

    private static Vector vectorOf(Element element) {
        return (Vector) WeaviateValueWriteDecorator.convert(element.value());
    }

    private static Property property(String name, String type) {
        return switch (type) {
            case DataType.TEXT -> Property.text(name, p -> p.tokenization(Tokenization.FIELD));
            case DataType.TEXT_ARRAY -> Property.textArray(name, p -> p.tokenization(Tokenization.FIELD));
            case DataType.INT -> Property.integer(name, p -> p.indexRangeFilters(true));
            case DataType.INT_ARRAY -> Property.integerArray(name);
            case DataType.NUMBER -> Property.number(name, p -> p.indexRangeFilters(true));
            case DataType.NUMBER_ARRAY -> Property.numberArray(name);
            case DataType.BOOL -> Property.bool(name);
            case DataType.BOOL_ARRAY -> Property.boolArray(name);
            default -> throw new UnsupportedOperationException("Unsupported Weaviate type " + type);
        };
    }

    void validate(CommunicationEntity entity, Element vector) {
        WeaviateVectors.Kind actual = WeaviateVectors.kind(vectorOf(vector));
        if (actual != kind) {
            throw new IllegalArgumentException("The collection " + collection + " stores " + kind
                    + " vectors but the entity uses " + actual);
        }
        String name = WeaviateNames.vectorName(vector.name());
        if (!name.equals(vectorName)) {
            throw new IllegalArgumentException("The collection " + collection + " has the vector " + vectorName
                    + " but the entity provides " + name);
        }
        for (Element element : entity.elements()) {
            if (element == vector || WeaviateObjectConverter.isNull(element)) {
                continue;
            }
            String property = WeaviateNames.property(element.name());
            String type = properties.get(property);
            if (type == null) {
                throw new IllegalArgumentException("The collection " + collection + " has no property "
                        + element.name() + ". Weaviate collections have a fixed schema");
            }
            Object value = WeaviateObjectConverter.storable(element);
            if (!WeaviateValueConverter.isCompatible(type, value)) {
                throw new IllegalArgumentException("The property " + element.name() + " of the collection "
                        + collection + " is " + type + " but the value is " + value.getClass().getSimpleName());
            }
        }
    }
}
