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
import java.util.List;
import java.util.Map;
import java.util.UUID;

class FieldValueConverter {
    private static final List<FieldValueMapper> MAPPERS = List.of(
            new FieldValuePassthroughMapper(),
            new StringValueMapper(),
            new UUIDValueMapper(),
            new IntegerValueMapper(),
            new LongValueMapper(),
            new DoubleValueMapper(),
            new BooleanValueMapper(),
            new NumberValueMapper(),
            new ByteArrayValueMapper(),
            new EnumValueMapper(),
            new IterableValueMapper(),
            new ArrayValueMapper(),
            new MapValueMapper()
    );

    private FieldValueConverter() {
        throw new AssertionError("Utility class");
    }

    static FieldValue of(Object value) {
        if (value == null) {
            return NullValue.getInstance();
        }

        for (FieldValueMapper mapper : MAPPERS) {
            if (mapper.supports(value)) {
                return mapper.toFieldValue(value);
            }
        }

        throw new UnsupportedOperationException("Unsupported value type: " + value.getClass());
    }

    public static Object toJavaObject(FieldValue value) {
        if (value == null || value.isNull()) {
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
            default -> throw new UnsupportedOperationException("Unsupported FieldValue type: " + value.getType());
        };
    }

    private interface FieldValueMapper {
        boolean supports(Object value);
        FieldValue toFieldValue(Object value);
    }

    private static final class FieldValuePassthroughMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof FieldValue;
        }

        public FieldValue toFieldValue(Object value) {
            return (FieldValue) value;
        }
    }

    private static final class StringValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof String;
        }

        public FieldValue toFieldValue(Object value) {
            return new StringValue((String) value);
        }
    }

    private static final class UUIDValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof UUID;
        }

        public FieldValue toFieldValue(Object value) {
            var uuid = (UUID) value;
            return new StringValue(uuid.toString());
        }
    }

    private static final class IntegerValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Integer;
        }

        public FieldValue toFieldValue(Object value) {
            return new IntegerValue((Integer) value);
        }
    }

    private static final class LongValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Long;
        }

        public FieldValue toFieldValue(Object value) {
            return new LongValue((Long) value);
        }
    }

    private static final class DoubleValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Double;
        }

        public FieldValue toFieldValue(Object value) {
            return new DoubleValue((Double) value);
        }
    }

    private static final class BooleanValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Boolean;
        }

        public FieldValue toFieldValue(Object value) {
            return Boolean.TRUE.equals(value)
                    ? BooleanValue.trueInstance()
                    : BooleanValue.falseInstance();
        }
    }

    private static final class NumberValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Number &&
                    !(value instanceof Integer || value instanceof Long || value instanceof Double);
        }

        public FieldValue toFieldValue(Object value) {
            return new NumberValue(value.toString());
        }
    }

    private static final class ByteArrayValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof byte[];
        }

        public FieldValue toFieldValue(Object value) {
            return new BinaryValue((byte[]) value);
        }
    }

    private static final class EnumValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Enum<?>;
        }

        public FieldValue toFieldValue(Object value) {
            return new StringValue(((Enum<?>) value).name());
        }
    }

    private static final class IterableValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Iterable<?>;
        }

        public FieldValue toFieldValue(Object value) {
            ArrayValue array = new ArrayValue();
            for (Object item : (Iterable<?>) value) {
                array.add(FieldValueConverter.of(item));
            }
            return array;
        }
    }

    private static final class ArrayValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value != null && value.getClass().isArray();
        }

        public FieldValue toFieldValue(Object value) {
            int length = Array.getLength(value);
            ArrayValue array = new ArrayValue();
            for (int i = 0; i < length; i++) {
                array.add(FieldValueConverter.of(Array.get(value, i)));
            }
            return array;
        }
    }

    private static final class MapValueMapper implements FieldValueMapper {
        public boolean supports(Object value) {
            return value instanceof Map<?, ?>;
        }

        public FieldValue toFieldValue(Object value) {
            Map<?, ?> map = (Map<?, ?>) value;
            MapValue mapValue = new MapValue();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String keyStr)) {
                    throw new IllegalArgumentException("Map keys must be strings. Found: " + key);
                }
                mapValue.put(keyStr, FieldValueConverter.of(entry.getValue()));
            }
            return mapValue;
        }
    }
}
