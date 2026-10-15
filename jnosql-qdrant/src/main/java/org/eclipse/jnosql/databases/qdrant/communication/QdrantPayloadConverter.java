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
import io.qdrant.client.grpc.JsonWithInt.ListValue;
import io.qdrant.client.grpc.JsonWithInt.Struct;
import io.qdrant.client.grpc.JsonWithInt.Value;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.Vector;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts {@link Element}s to Qdrant payload values and back.
 */
final class QdrantPayloadConverter {

    private QdrantPayloadConverter() {
    }

    static Map<String, Value> toPayload(List<Element> elements) {
        Map<String, Value> payload = new LinkedHashMap<>();
        elements.forEach(element -> payload.put(element.name(), toValue(QdrantValueWriteDecorator.convert(element.value()))));
        return payload;
    }

    static Value toValue(Object value) {
        if (value == null) {
            return ValueFactory.nullValue();
        }
        if (value instanceof Boolean bool) {
            return ValueFactory.value(bool);
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return ValueFactory.value(((Number) value).longValue());
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.bitLength() < Long.SIZE
                    ? ValueFactory.value(bigInteger.longValue())
                    : ValueFactory.value(bigInteger.toString());
        }
        if (value instanceof BigDecimal || value instanceof Float || value instanceof Double) {
            return ValueFactory.value(((Number) value).doubleValue());
        }
        if (value instanceof Vector) {
            throw new IllegalArgumentException("A vector is only supported as the entity vector, not inside a payload");
        }
        if (value instanceof Element element) {
            return struct(List.of(element));
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Value> fields = new LinkedHashMap<>();
            map.forEach((key, entry) -> fields.put(String.valueOf(key), toValue(entry)));
            return ValueFactory.value(fields);
        }
        if (value instanceof Iterable<?> iterable) {
            return fromIterable(iterable);
        }
        return ValueFactory.value(value instanceof Enum<?> enumeration ? enumeration.name() : value.toString());
    }

    static List<Element> toElements(Map<String, Value> payload) {
        List<Element> elements = new ArrayList<>();
        payload.forEach((name, value) -> {
            Object converted = toObject(value);
            if (converted != null) {
                elements.add(Element.of(name, converted));
            }
        });
        return elements;
    }

    private static Value fromIterable(Iterable<?> iterable) {
        List<Object> items = new ArrayList<>();
        iterable.forEach(items::add);
        if (!items.isEmpty() && items.stream().allMatch(Element.class::isInstance)) {
            return struct(items.stream().map(Element.class::cast).toList());
        }
        return ValueFactory.list(items.stream().map(QdrantPayloadConverter::toValue).toList());
    }

    private static Value struct(List<Element> elements) {
        Map<String, Value> fields = new LinkedHashMap<>();
        elements.forEach(element -> fields.put(element.name(), toValue(QdrantValueWriteDecorator.convert(element.value()))));
        return ValueFactory.value(fields);
    }

    private static Object toObject(Value value) {
        return switch (value.getKindCase()) {
            case DOUBLE_VALUE -> value.getDoubleValue();
            case INTEGER_VALUE -> value.getIntegerValue();
            case STRING_VALUE -> value.getStringValue();
            case BOOL_VALUE -> value.getBoolValue();
            case STRUCT_VALUE -> toElements(structFields(value.getStructValue()));
            case LIST_VALUE -> list(value.getListValue());
            case NULL_VALUE, KIND_NOT_SET -> null;
        };
    }

    private static Map<String, Value> structFields(Struct struct) {
        return struct.getFieldsMap();
    }

    private static List<Object> list(ListValue list) {
        List<Object> items = new ArrayList<>();
        list.getValuesList().forEach(item -> items.add(toObject(item)));
        return items;
    }
}
