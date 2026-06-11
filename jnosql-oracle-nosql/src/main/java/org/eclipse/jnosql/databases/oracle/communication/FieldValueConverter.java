/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
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
import java.util.Map;
import java.util.UUID;

enum FieldValueConverter {

    INSTANCE;

    FieldValue of(Object value){
        if (value == null) {
            return NullValue.getInstance();
        }
        if (value instanceof String string) {
            return new StringValue(string);
        } else if(value instanceof UUID uuid) {
            return new StringValue(uuid.toString());
        }
        else if (value instanceof Integer integer) {
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
        if (value.isNull()) {
            return null;
        }
        return switch (value.getType()) {
            case STRING -> value.asString();
            case INTEGER -> value.asInteger();
            case LONG -> value.asLong();
            case DOUBLE -> value.asDouble();
            case BOOLEAN -> value.asBoolean();
            case NUMBER -> value.asNumber();
            case BINARY -> value.asBinary();
            case ARRAY -> value.asArray();
            case MAP -> value.asMap();
            default -> throw new UnsupportedOperationException("There is not support to: " + value.getType());
        };
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
        for (int i = 0; i < length; i ++) {
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
