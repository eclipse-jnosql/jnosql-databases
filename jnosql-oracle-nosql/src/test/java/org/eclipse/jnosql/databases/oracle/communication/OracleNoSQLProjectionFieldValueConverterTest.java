/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OracleNoSQLProjectionFieldValueConverterTest {

    @Test
    void shouldConvertScalarFieldValuesToJavaValues() {
        BigDecimal number = new BigDecimal("1234567890.0123456789");
        byte[] binary = {1, 2, 3};
        Timestamp timestamp = Timestamp.valueOf("2026-08-02 19:30:45.123456789");

        assertThat(FieldValueConverter.toObject(new StringValue("Ada")))
                .isExactlyInstanceOf(String.class).isEqualTo("Ada");
        assertThat(FieldValueConverter.toObject(new IntegerValue(42)))
                .isExactlyInstanceOf(Integer.class).isEqualTo(42);
        assertThat(FieldValueConverter.toObject(new LongValue(42L)))
                .isExactlyInstanceOf(Long.class).isEqualTo(42L);
        assertThat(FieldValueConverter.toObject(new DoubleValue(42.5)))
                .isExactlyInstanceOf(Double.class).isEqualTo(42.5);
        assertThat(FieldValueConverter.toObject(BooleanValue.trueInstance()))
                .isExactlyInstanceOf(Boolean.class).isEqualTo(true);
        assertThat(FieldValueConverter.toObject(new NumberValue(number)))
                .isExactlyInstanceOf(BigDecimal.class).isEqualTo(number);
        assertThat((byte[]) FieldValueConverter.toObject(new BinaryValue(binary)))
                .containsExactly(binary);
        assertThat(FieldValueConverter.toObject(new TimestampValue(timestamp)))
                .isExactlyInstanceOf(Timestamp.class).isEqualTo(timestamp);
        assertThat(FieldValueConverter.toObject(NullValue.getInstance())).isNull();
        assertThat(FieldValueConverter.toObject(JsonNullValue.getInstance())).isNull();
    }

    @Test
    void shouldRecursivelyConvertNestedFieldValues() {
        MapValue nested = new MapValue(true, 4)
                .put("enabled", BooleanValue.trueInstance())
                .put("nullValue", NullValue.getInstance())
                .put("jsonNullValue", JsonNullValue.getInstance())
                .put("missing", EmptyValue.getInstance());
        ArrayValue array = new ArrayValue()
                .add(new StringValue("first"))
                .add(new IntegerValue(2))
                .add(JsonNullValue.getInstance())
                .add(nested);
        MapValue value = new MapValue(true, 3)
                .put("name", new StringValue("projection"))
                .put("items", array)
                .put("details", nested);

        Object converted = FieldValueConverter.toObject(value);

        assertThat(converted).isInstanceOf(Map.class);
        Map<?, ?> result = (Map<?, ?>) converted;
        assertThat(result.get("name")).isExactlyInstanceOf(String.class).isEqualTo("projection");
        assertThat(result.get("items")).isInstanceOf(List.class);
        assertThat(result.get("items")).isEqualTo(Arrays.asList("first", 2, null, expectedNestedMap()));
        assertThat(result.get("details")).isEqualTo(expectedNestedMap());
        assertNoFieldValues(converted);
    }

    @Test
    void shouldTreatEmptyAsMissingWithoutChangingArrayPositions() {
        MapValue map = new MapValue(true, 2)
                .put("present", new StringValue("value"))
                .put("missing", EmptyValue.getInstance());

        assertThat(FieldValueConverter.toObject(map)).isEqualTo(Map.of("present", "value"));
        assertThatThrownBy(() -> FieldValueConverter.toObject(EmptyValue.getInstance()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("missing field");
        assertThatThrownBy(() -> FieldValueConverter.toObject(
                new ArrayValue().add(EmptyValue.getInstance())))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("missing field");
    }

    private static void assertNoFieldValues(Object value) {
        if (value == null) {
            return;
        }
        assertThat(value).isNotInstanceOf(FieldValue.class);
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, item) -> assertNoFieldValues(item));
        } else if (value instanceof Iterable<?> iterable) {
            iterable.forEach(OracleNoSQLProjectionFieldValueConverterTest::assertNoFieldValues);
        }
    }

    private static Map<String, Object> expectedNestedMap() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("enabled", true);
        expected.put("nullValue", null);
        expected.put("jsonNullValue", null);
        return expected;
    }
}
