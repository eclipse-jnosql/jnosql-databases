/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import oracle.nosql.driver.values.ArrayValue;
import oracle.nosql.driver.values.BinaryValue;
import oracle.nosql.driver.values.BooleanValue;
import oracle.nosql.driver.values.DoubleValue;
import oracle.nosql.driver.values.EmptyValue;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.IntegerValue;
import oracle.nosql.driver.values.JsonNullValue;
import oracle.nosql.driver.values.LongValue;
import oracle.nosql.driver.values.MapValue;
import oracle.nosql.driver.values.NullValue;
import oracle.nosql.driver.values.NumberValue;
import oracle.nosql.driver.values.StringValue;
import oracle.nosql.driver.values.TimestampValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleNoSQLProjectionFieldValueConverterTest {

    private final FieldValueConverter converter = FieldValueConverter.INSTANCE;

    @Test
    @SuppressWarnings("unchecked")
    void shouldRecursivelyConvertFieldValuesToJavaValues() {
        byte[] binary = {1, 2, 3};
        BigDecimal number = new BigDecimal("1234567890.0123456789");
        Timestamp timestamp = Timestamp.valueOf("2026-08-02 12:34:56.123456789");
        MapValue nestedMap = new MapValue()
                .put("nestedString", new StringValue("nested"))
                .put("nestedLong", new LongValue(99L));
        ArrayValue array = new ArrayValue()
                .add(new StringValue("first"))
                .add(new IntegerValue(7))
                .add(nestedMap);
        MapValue source = new MapValue()
                .put("string", new StringValue("value"))
                .put("integer", new IntegerValue(42))
                .put("long", new LongValue(43L))
                .put("double", new DoubleValue(44.5D))
                .put("boolean", BooleanValue.trueInstance())
                .put("number", new NumberValue(number))
                .put("binary", new BinaryValue(binary))
                .put("timestamp", new TimestampValue(timestamp))
                .put("null", NullValue.getInstance())
                .put("jsonNull", JsonNullValue.getInstance())
                .put("array", array)
                .put("map", nestedMap);

        Object converted = converter.toObject(source);

        assertThat(converted).isInstanceOf(Map.class);
        Map<String, Object> result = (Map<String, Object>) converted;
        assertThat(result.get("string")).isExactlyInstanceOf(String.class).isEqualTo("value");
        assertThat(result.get("integer")).isExactlyInstanceOf(Integer.class).isEqualTo(42);
        assertThat(result.get("long")).isExactlyInstanceOf(Long.class).isEqualTo(43L);
        assertThat(result.get("double")).isExactlyInstanceOf(Double.class).isEqualTo(44.5D);
        assertThat(result.get("boolean")).isExactlyInstanceOf(Boolean.class).isEqualTo(true);
        assertThat(result.get("number")).isExactlyInstanceOf(BigDecimal.class).isEqualTo(number);
        assertThat(result.get("binary")).isExactlyInstanceOf(byte[].class);
        assertThat((byte[]) result.get("binary")).containsExactly(binary);
        assertThat(result.get("timestamp")).isExactlyInstanceOf(Timestamp.class).isEqualTo(timestamp);
        assertThat(result).containsKeys("null", "jsonNull");
        assertThat(result.get("null")).isNull();
        assertThat(result.get("jsonNull")).isNull();

        assertThat(result.get("array")).isInstanceOf(List.class);
        List<Object> convertedArray = (List<Object>) result.get("array");
        assertThat(convertedArray.get(0)).isExactlyInstanceOf(String.class).isEqualTo("first");
        assertThat(convertedArray.get(1)).isExactlyInstanceOf(Integer.class).isEqualTo(7);
        assertThat(convertedArray.get(2)).isInstanceOf(Map.class);
        Map<String, Object> arrayMap = (Map<String, Object>) convertedArray.get(2);
        assertThat(arrayMap.get("nestedString")).isExactlyInstanceOf(String.class).isEqualTo("nested");
        assertThat(arrayMap.get("nestedLong")).isExactlyInstanceOf(Long.class).isEqualTo(99L);

        assertThat(result.get("map")).isInstanceOf(Map.class);
        Map<String, Object> convertedMap = (Map<String, Object>) result.get("map");
        assertThat(convertedMap.get("nestedString")).isExactlyInstanceOf(String.class).isEqualTo("nested");
        assertThat(convertedMap.get("nestedLong")).isExactlyInstanceOf(Long.class).isEqualTo(99L);
        assertNoFieldValue(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldKeepEmptyDistinctFromNull() {
        MapValue source = new MapValue()
                .put("missing", EmptyValue.getInstance())
                .put("null", NullValue.getInstance());

        Map<String, Object> result = (Map<String, Object>) converter.toObject(source);

        assertThat(result).doesNotContainKey("missing");
        assertThat(result).containsKey("null");
        assertThat(result.get("null")).isNull();
        assertThatThrownBy(() -> converter.toObject(EmptyValue.getInstance()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("missing value");
        assertThatThrownBy(() -> converter.toObject(new ArrayValue().add(EmptyValue.getInstance())))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("missing value");
    }

    private static void assertNoFieldValue(Object value) {
        if (value == null) {
            return;
        }
        assertThat(value).isNotInstanceOf(FieldValue.class);
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(OracleNoSQLProjectionFieldValueConverterTest::assertNoFieldValue);
        } else if (value instanceof Iterable<?> values) {
            values.forEach(OracleNoSQLProjectionFieldValueConverterTest::assertNoFieldValue);
        }
    }
}
