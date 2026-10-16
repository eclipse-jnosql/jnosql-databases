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

import io.weaviate.client6.v1.api.collections.DataType;
import org.eclipse.jnosql.communication.Value;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converts Java values into the scalar types Weaviate stores, and the stored values back.
 * <ul>
 *     <li>{@code text}: {@link CharSequence}, {@link Character}, {@link Enum} and {@link UUID};</li>
 *     <li>{@code int}: {@link Byte}, {@link Short}, {@link Integer} and {@link Long};</li>
 *     <li>{@code number}: {@link Float} and {@link Double};</li>
 *     <li>{@code boolean}: {@link Boolean};</li>
 *     <li>arrays of the types above, from an {@link Iterable} or an array.</li>
 * </ul>
 * Any other value is rejected instead of being turned into text.
 */
final class WeaviateValueConverter {

    private WeaviateValueConverter() {
    }

    static Object convert(Value value) {
        return toStorable(WeaviateValueWriteDecorator.convert(value));
    }

    static List<Object> convertToList(Value value) {
        return WeaviateValueWriteDecorator.convertToList(value).stream()
                .map(WeaviateValueConverter::toStorable)
                .toList();
    }

    static Object toStorable(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Value wrapped) {
            return convert(wrapped);
        }
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return ((Number) value).longValue();
        }
        if (value instanceof Float || value instanceof Double) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof CharSequence || value instanceof Character || value instanceof UUID) {
            return value.toString();
        }
        if (value instanceof Enum<?> enumeration) {
            return enumeration.name();
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> items = new ArrayList<>();
            iterable.forEach(items::add);
            return toArray(items);
        }
        if (value.getClass().isArray() && !(value instanceof byte[])) {
            List<Object> items = new ArrayList<>();
            for (int index = 0; index < Array.getLength(value); index++) {
                items.add(Array.get(value, index));
            }
            return toArray(items);
        }
        throw new UnsupportedOperationException("Weaviate cannot store a value of type "
                + value.getClass().getName());
    }

    private static List<Object> toArray(List<Object> items) {
        List<Object> converted = new ArrayList<>();
        for (Object item : items) {
            Object storable = toStorable(item);
            if (storable == null || storable instanceof List<?>) {
                throw new UnsupportedOperationException("Weaviate arrays accept only non-null scalar values");
            }
            converted.add(storable);
        }
        boolean hasDouble = converted.stream().anyMatch(Double.class::isInstance);
        if (hasDouble && converted.stream().allMatch(Number.class::isInstance)) {
            return converted.stream().map(item -> (Object) ((Number) item).doubleValue()).toList();
        }
        if (!converted.isEmpty() && converted.stream().map(Object::getClass).distinct().count() > 1) {
            throw new IllegalArgumentException("A Weaviate array must have values of a single type");
        }
        return converted;
    }

    /**
     * The Weaviate data type of a converted value, or {@code null} when it cannot be inferred from an
     * empty array.
     */
    static String dataType(Object storable) {
        if (storable instanceof String) {
            return DataType.TEXT;
        }
        if (storable instanceof Long) {
            return DataType.INT;
        }
        if (storable instanceof Double) {
            return DataType.NUMBER;
        }
        if (storable instanceof Boolean) {
            return DataType.BOOL;
        }
        if (storable instanceof List<?> list) {
            if (list.isEmpty()) {
                return null;
            }
            String element = dataType(list.get(0));
            return element + "[]";
        }
        throw new UnsupportedOperationException("Weaviate cannot store a value of type "
                + storable.getClass().getName());
    }

    static boolean isCompatible(String propertyType, Object storable) {
        String valueType = dataType(storable);
        if (valueType == null) {
            return propertyType.endsWith("[]");
        }
        if (propertyType.equals(valueType)) {
            return true;
        }
        return DataType.NUMBER.equals(propertyType) && DataType.INT.equals(valueType)
                || DataType.NUMBER_ARRAY.equals(propertyType) && DataType.INT_ARRAY.equals(valueType);
    }

    static Object fromStored(Object value, String propertyType) {
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            String element = propertyType != null && propertyType.endsWith("[]")
                    ? propertyType.substring(0, propertyType.length() - 2) : null;
            return list.stream().map(item -> fromStored(item, element)).toList();
        }
        if (value instanceof Number number && propertyType != null) {
            if (DataType.INT.equals(propertyType)) {
                return number.longValue();
            }
            if (DataType.NUMBER.equals(propertyType)) {
                return number.doubleValue();
            }
        }
        return value;
    }
}
