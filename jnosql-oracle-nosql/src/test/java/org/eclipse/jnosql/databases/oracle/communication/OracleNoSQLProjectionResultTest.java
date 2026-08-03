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
import oracle.nosql.driver.values.StringValue;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OracleNoSQLProjectionResultTest {

    @Test
    void shouldExposeSqlProjectionValuesWithoutOracleWrappers() {
        MapValue nested = new MapValue(true, 6)
                .put("label", new StringValue("nested"))
                .put("values", new ArrayValue()
                        .add(new IntegerValue(1))
                        .add(JsonNullValue.getInstance()))
                .put("sqlNull", NullValue.getInstance())
                .put("jsonNull", JsonNullValue.getInstance())
                .put("missing", EmptyValue.getInstance());
        MapValue row = new MapValue(true, 8)
                .put(DefaultOracleNoSQLDocumentManager.ORACLE_ID, new StringValue("person:42"))
                .put(DefaultOracleNoSQLDocumentManager.ENTITY, new StringValue("person"))
                .put("name", new StringValue("Ada"))
                .put("age", new IntegerValue(37))
                .put("profile", nested)
                .put("nullable", JsonNullValue.getInstance())
                .put("missing", EmptyValue.getInstance());

        NoSQLHandle handle = mock(NoSQLHandle.class);
        Jsonb jsonb = mock(Jsonb.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        PrepareResult prepareResult = new PrepareResult().setPreparedStatement(statement);
        QueryResult queryResult = new QueryResult(new QueryRequest()).setResults(List.of(row));
        when(handle.prepare(any(PrepareRequest.class))).thenReturn(prepareResult);
        when(handle.query(any(QueryRequest.class))).thenReturn(queryResult);
        var manager = new DefaultOracleNoSQLDocumentManager("database", handle, jsonb);

        CommunicationEntity entity = manager.sql("select projections from database")
                .findFirst().orElseThrow();

        assertThat(entity.name()).isEqualTo("person");
        assertThat(entity.find(DefaultOracleNoSQLDocumentManager.ID).orElseThrow().get())
                .isExactlyInstanceOf(String.class).isEqualTo("42");
        assertThat(entity.find("name").orElseThrow().get())
                .isExactlyInstanceOf(String.class).isEqualTo("Ada");
        assertThat(entity.find("age").orElseThrow().get())
                .isExactlyInstanceOf(Integer.class).isEqualTo(37);
        assertThat(entity.find("nullable")).isPresent();
        assertThat(entity.find("nullable").orElseThrow().get()).isNull();
        assertThat(entity.find("missing")).isEmpty();

        Map<String, Object> profile = new LinkedHashMap<>();
        for (Object value : (List<?>) entity.find("profile").orElseThrow().get()) {
            Element element = (Element) value;
            profile.put(element.name(), element.get());
        }
        assertThat(profile).containsKeys("sqlNull", "jsonNull").doesNotContainKey("missing");
        assertThat(profile.get("sqlNull")).isNull();
        assertThat(profile.get("jsonNull")).isNull();
        assertNoFieldValues(entity.toMap());
    }

    private static void assertNoFieldValues(Object value) {
        if (value == null) {
            return;
        }
        assertThat(value).isNotInstanceOf(FieldValue.class);
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, item) -> assertNoFieldValues(item));
        } else if (value instanceof Iterable<?> iterable) {
            iterable.forEach(OracleNoSQLProjectionResultTest::assertNoFieldValues);
        } else if (value instanceof Element element) {
            assertNoFieldValues(element.get());
        }
    }
}
