/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import oracle.nosql.driver.values.ArrayValue;
import oracle.nosql.driver.values.BinaryValue;
import oracle.nosql.driver.values.BooleanValue;
import oracle.nosql.driver.values.DoubleValue;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.IntegerValue;
import oracle.nosql.driver.values.LongValue;
import oracle.nosql.driver.values.MapValue;
import oracle.nosql.driver.values.NullValue;
import oracle.nosql.driver.values.NumberValue;
import oracle.nosql.driver.values.StringValue;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

enum FieldValueConverter {

    INSTANCE;

    FieldValue of(Object value) {
        if (value == null) {
            return NullValue.getInstance();
        } else if (value instanceof Character character) {
            return new StringValue(character.toString());
        } else if (value instanceof String string) {
            return new StringValue(string);
        } else if (value instanceof UUID uuid) {
            return new StringValue(uuid.toString());
        } else if (value instanceof Integer integer) {
            return new IntegerValue(integer);
        } else if (value instanceof Long longValue) {
            return new LongValue(longValue);
        } else if (value instanceof Double doubleValue) {
            return new DoubleValue(doubleValue);
        } else if (value instanceof Boolean booleanValue) {
            return Boolean.TRUE.equals(booleanValue) ? BooleanValue.trueInstance() : BooleanValue.falseInstance();
        } else if (value instanceof Number) {
            return new NumberValue(value.toString());
        } else if (value instanceof byte[]) {
            return new BinaryValue((byte[]) value);
        } else if (value instanceof Iterable<?> values) {
            return createList(values);
        } else if (value.getClass().isArray()) {
            return createArray(value);
        } else if (value instanceof Map<?, ?>) {
            return entries((Map<String, ?>) value);
        } else if (value instanceof FieldValue) {
            return (FieldValue) value;
        } else {
            throw new UnsupportedOperationException("There is not support to: " + value.getClass());
        }
    }

    Object toObject(FieldValue value) {
        return switch (value.getType()) {
            case STRING -> value.getString();
            case INTEGER -> value.getInt();
            case LONG -> value.getLong();
            case DOUBLE -> value.getDouble();
            case BOOLEAN -> value.getBoolean();
            case NUMBER -> value.getNumber();
            case BINARY -> value.getBinary();
            case TIMESTAMP -> value.getTimestamp();
            case NULL, JSON_NULL -> null;
            case ARRAY -> toList(value.asArray());
            case MAP -> toMap(value.asMap());
            case EMPTY -> throw new UnsupportedOperationException("Oracle EMPTY represents a missing value");
        };
    }

    private List<Object> toList(ArrayValue value) {
        List<Object> values = new ArrayList<>(value.size());
        for (FieldValue element : value) {
            values.add(toObject(element));
        }
        return values;
    }

    private Map<String, Object> toMap(MapValue value) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Map.Entry<String, FieldValue> entry : value) {
            if (!entry.getValue().isEMPTY()) {
                values.put(entry.getKey(), toObject(entry.getValue()));
            }
        }
        return values;
    }

    private MapValue entries(Map<String, ?> value) {
        MapValue mapValue = new MapValue();
        for (Map.Entry<String, ?> entry : value.entrySet()) {
            mapValue.put(entry.getKey(), of(entry.getValue()));
        }
        return mapValue;
    }

    private ArrayValue createArray(Object value) {
        var arrayValue = new ArrayValue();
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            arrayValue.add(of(Array.get(value, i)));
        }
        return arrayValue;
    }

    private ArrayValue createList(Iterable<?> values) {
        var arrayValue = new ArrayValue();
        for (Object value : values) {
            arrayValue.add(of(value));
        }
        return arrayValue;
    }
}
