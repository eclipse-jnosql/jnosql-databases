/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.oracle.communication;

import jakarta.json.bind.Jsonb;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.ops.PrepareRequest;
import oracle.nosql.driver.ops.PrepareResult;
import oracle.nosql.driver.ops.PreparedStatement;
import oracle.nosql.driver.ops.QueryRequest;
import oracle.nosql.driver.ops.QueryResult;
import oracle.nosql.driver.values.ArrayValue;
import oracle.nosql.driver.values.EmptyValue;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.IntegerValue;
import oracle.nosql.driver.values.JsonNullValue;
import oracle.nosql.driver.values.MapValue;
import oracle.nosql.driver.values.NullValue;
import oracle.nosql.driver.values.NumberValue;
import oracle.nosql.driver.values.StringValue;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OracleNoSQLProjectionResultTest {

    @Test
    void shouldExposeProjectedFieldsAsJavaValues() {
        String sql = "select entity, id, name, age, amount, tags, profile, nullValue, jsonNull, missing from database";
        NoSQLHandle serviceHandle = mock(NoSQLHandle.class);
        Jsonb jsonB = mock(Jsonb.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        PrepareResult prepareResult = new PrepareResult().setPreparedStatement(preparedStatement);
        QueryResult queryResult = mock(QueryResult.class);
        MapValue profile = new MapValue()
                .put("city", new StringValue("Lisbon"))
                .put("coordinates", new ArrayValue().add(38.7D).add(-9.1D))
                .put("sqlNull", NullValue.getInstance())
                .put("jsonNull", JsonNullValue.getInstance())
                .put("missing", EmptyValue.getInstance());
        MapValue row = new MapValue()
                .put(DefaultOracleNoSQLDocumentManager.ENTITY, "person")
                .put(DefaultOracleNoSQLDocumentManager.ORACLE_ID, "person:42")
                .put("name", new StringValue("Ada"))
                .put("age", new IntegerValue(37))
                .put("amount", new NumberValue(new BigDecimal("19.99")))
                .put("tags", new ArrayValue().add("engineer").add("speaker"))
                .put("profile", profile)
                .put("nullValue", NullValue.getInstance())
                .put("jsonNull", JsonNullValue.getInstance())
                .put("missing", EmptyValue.getInstance());
        when(serviceHandle.prepare(any(PrepareRequest.class))).thenReturn(prepareResult);
        when(serviceHandle.query(any(QueryRequest.class))).thenReturn(queryResult);
        when(queryResult.getResults()).thenReturn(List.of(row));
        var manager = new DefaultOracleNoSQLDocumentManager("database", serviceHandle, jsonB);

        CommunicationEntity entity = manager.sql(sql).findFirst().orElseThrow();

        assertThat(entity.name()).isEqualTo("person");
        assertThat(entity.elementNames()).containsExactlyInAnyOrder("_id", "name", "age", "amount", "tags", "profile",
                "nullValue", "jsonNull");
        assertThat(entity.find("_id").orElseThrow().get()).isExactlyInstanceOf(String.class).isEqualTo("42");
        assertThat(entity.find("name").orElseThrow().get()).isExactlyInstanceOf(String.class).isEqualTo("Ada");
        assertThat(entity.find("age").orElseThrow().get()).isExactlyInstanceOf(Integer.class).isEqualTo(37);
        assertThat(entity.find("amount").orElseThrow().get())
                .isExactlyInstanceOf(BigDecimal.class).isEqualTo(new BigDecimal("19.99"));
        assertThat(entity.find("tags").orElseThrow().get())
                .isInstanceOf(List.class).isEqualTo(List.of("engineer", "speaker"));
        assertThat(entity.find("nullValue")).get().extracting(Element::get).isNull();
        assertThat(entity.find("jsonNull")).get().extracting(Element::get).isNull();
        assertThat(entity.find("missing")).isEmpty();

        Object profileValue = entity.find("profile").orElseThrow().get();
        assertThat(profileValue).isInstanceOf(List.class);
        Map<String, Object> convertedProfile = new LinkedHashMap<>();
        for (Object value : (List<?>) profileValue) {
            Element element = (Element) value;
            convertedProfile.put(element.name(), element.get());
        }
        assertThat(convertedProfile.get("city")).isExactlyInstanceOf(String.class).isEqualTo("Lisbon");
        assertThat(convertedProfile.get("coordinates"))
                .isInstanceOf(List.class).isEqualTo(List.of(38.7D, -9.1D));
        assertThat(convertedProfile).containsKeys("sqlNull", "jsonNull").doesNotContainKey("missing");
        assertThat(convertedProfile.get("sqlNull")).isNull();
        assertThat(convertedProfile.get("jsonNull")).isNull();
        assertNoFieldValue(entity.elements());
        verify(serviceHandle).prepare(any(PrepareRequest.class));
        verify(serviceHandle).query(any(QueryRequest.class));
    }

    private static void assertNoFieldValue(Object value) {
        if (value == null) {
            return;
        }
        assertThat(value).isNotInstanceOf(FieldValue.class);
        if (value instanceof Element element) {
            assertNoFieldValue(element.get());
        } else if (value instanceof Map<?, ?> map) {
            map.values().forEach(OracleNoSQLProjectionResultTest::assertNoFieldValue);
        } else if (value instanceof Iterable<?> values) {
            values.forEach(OracleNoSQLProjectionResultTest::assertNoFieldValue);
        }
    }
}
