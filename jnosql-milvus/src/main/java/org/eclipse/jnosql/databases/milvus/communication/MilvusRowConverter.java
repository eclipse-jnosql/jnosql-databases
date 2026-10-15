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
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts between {@link CommunicationEntity} and Milvus rows.
 * Null values are not written: Milvus stores them as null for the nullable fields.
 */
final class MilvusRowConverter {

    static final String ID_FIELD = "_id";

    private static final String DYNAMIC_FIELD = "$meta";

    private MilvusRowConverter() {
    }

    static Element vectorElement(CommunicationEntity entity) {
        List<Element> vectors = entity.elements().stream()
                .filter(element -> MilvusValueWriteDecorator.convert(element.value()) instanceof Vector)
                .toList();
        if (vectors.size() != 1) {
            throw new IllegalArgumentException("The entity " + entity.name()
                    + " must have exactly one vector element but has " + vectors.size());
        }
        return vectors.get(0);
    }

    static float[] values(Element vectorElement) {
        Object vector = MilvusValueWriteDecorator.convert(vectorElement.value());
        if (vector instanceof DenseVector dense) {
            return dense.values();
        }
        throw new UnsupportedOperationException("Milvus only supports dense vectors but got: "
                + vector.getClass().getName());
    }

    static Object ensureId(CommunicationEntity entity) {
        return entity.find(ID_FIELD)
                .map(element -> MilvusValueWriteDecorator.convert(element.value()))
                .filter(id -> id != null)
                .orElseGet(() -> {
                    String generated = UUID.randomUUID().toString();
                    entity.add(ID_FIELD, generated);
                    return generated;
                });
    }

    static JsonObject toRow(CommunicationEntity entity, Element vectorElement) {
        JsonObject row = new JsonObject();
        for (Element element : entity.elements()) {
            if (element == vectorElement) {
                JsonArray array = new JsonArray();
                for (float value : values(vectorElement)) {
                    array.add(value);
                }
                row.add(element.name(), array);
                continue;
            }
            Object value = MilvusValueWriteDecorator.convert(element.value());
            if (value != null) {
                row.add(element.name(), toJson(value));
            }
        }
        return row;
    }

    static JsonElement toJson(Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof Value wrapped) {
            return toJson(MilvusValueWriteDecorator.convert(wrapped));
        }
        if (value instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        }
        if (value instanceof Number number) {
            return new JsonPrimitive(number);
        }
        if (value instanceof CharSequence || value instanceof Character || value instanceof UUID) {
            return new JsonPrimitive(value.toString());
        }
        if (value instanceof Enum<?> enumeration) {
            return new JsonPrimitive(enumeration.name());
        }
        if (value instanceof Element element) {
            JsonObject object = new JsonObject();
            object.add(element.name(), toJson(MilvusValueWriteDecorator.convert(element.value())));
            return object;
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject();
            map.forEach((key, entry) -> object.add(String.valueOf(key), toJson(entry)));
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            return fromIterable(iterable);
        }
        if (value.getClass().isArray() && !(value instanceof byte[])) {
            List<Object> items = new ArrayList<>();
            for (int index = 0; index < Array.getLength(value); index++) {
                items.add(Array.get(value, index));
            }
            return fromIterable(items);
        }
        throw new UnsupportedOperationException("Milvus cannot store a value of type " + value.getClass().getName());
    }

    private static JsonElement fromIterable(Iterable<?> iterable) {
        List<Object> items = new ArrayList<>();
        iterable.forEach(items::add);
        if (!items.isEmpty() && items.stream().allMatch(Element.class::isInstance)) {
            JsonObject object = new JsonObject();
            items.forEach(item -> {
                Element element = (Element) item;
                object.add(element.name(), toJson(MilvusValueWriteDecorator.convert(element.value())));
            });
            return object;
        }
        JsonArray array = new JsonArray();
        items.forEach(item -> array.add(toJson(item)));
        return array;
    }

    static CommunicationEntity toEntity(String collection, Map<String, Object> row, String vectorField) {
        List<Element> elements = new ArrayList<>();
        row.forEach((name, value) -> {
            if (DYNAMIC_FIELD.equals(name) || value == null) {
                return;
            }
            if (name.equals(vectorField)) {
                elements.add(Element.of(name, DenseVector.of(floats(value))));
            } else {
                Object converted = fromJson(value);
                if (converted != null) {
                    elements.add(Element.of(name, converted));
                }
            }
        });
        return CommunicationEntity.of(collection, elements);
    }

    private static float[] floats(Object value) {
        if (value instanceof List<?> list) {
            float[] values = new float[list.size()];
            for (int index = 0; index < values.length; index++) {
                values[index] = ((Number) list.get(index)).floatValue();
            }
            return values;
        }
        if (value instanceof JsonArray array) {
            float[] values = new float[array.size()];
            for (int index = 0; index < values.length; index++) {
                values[index] = array.get(index).getAsFloat();
            }
            return values;
        }
        throw new IllegalStateException("Unexpected vector value: " + value.getClass().getName());
    }

    static Object fromJson(Object value) {
        if (value instanceof JsonObject object) {
            List<Element> elements = new ArrayList<>();
            object.asMap().forEach((name, item) -> {
                Object converted = fromJson(item);
                if (converted != null) {
                    elements.add(Element.of(name, converted));
                }
            });
            return elements;
        }
        if (value instanceof JsonArray array) {
            List<Object> items = new ArrayList<>();
            array.forEach(item -> items.add(fromJson(item)));
            return items;
        }
        if (value instanceof JsonNull) {
            return null;
        }
        if (value instanceof JsonPrimitive primitive) {
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                double number = primitive.getAsDouble();
                String text = primitive.getAsString();
                return text.contains(".") || text.contains("e") || text.contains("E")
                        ? (Object) number : (Object) primitive.getAsLong();
            }
            return primitive.getAsString();
        }
        return value;
    }
}
