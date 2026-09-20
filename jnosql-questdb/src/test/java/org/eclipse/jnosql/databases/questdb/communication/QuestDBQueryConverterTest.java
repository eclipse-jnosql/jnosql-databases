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
package org.eclipse.jnosql.databases.questdb.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuestDBQueryConverterTest {

    @Test
    void shouldConvertPredicatesProjectionLimitAndOrder() {
        SelectQuery query = SelectQuery.select("sensor", "temperature")
                .from("sensor_reading")
                .where("sensor").eq("warehouse-1")
                .and("temperature").gte(20D)
                .orderBy("_id").desc()
                .limit(10)
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo(
                "SELECT \"timestamp\", \"sensor\", \"temperature\" FROM \"sensor_reading\" "
                        + "WHERE (\"sensor\" = $1 AND \"temperature\" >= $2) "
                        + "ORDER BY \"timestamp\" DESC LIMIT $3");
        assertThat(sql.parameters()).containsExactly("warehouse-1", 20D, 10L);
    }

    @Test
    void shouldConvertOffsetAndLimitToQuestDBRange() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .skip(10)
                .limit(20)
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo("SELECT * FROM \"sensor_reading\" LIMIT $1, $2");
        assertThat(sql.parameters()).containsExactly(10L, 30L);
    }

    @Test
    void shouldConvertSecondPageOfTenRecords() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .skip(10)
                .limit(10)
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo("SELECT * FROM \"sensor_reading\" LIMIT $1, $2");
        assertThat(sql.parameters()).containsExactly(10L, 20L);
    }

    @Test
    void shouldTreatZeroOffsetAsLimitOnly() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .skip(0)
                .limit(10)
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo("SELECT * FROM \"sensor_reading\" LIMIT $1");
        assertThat(sql.parameters()).containsExactly(10L);
    }

    @Test
    void shouldConvertOffsetOneAndLimitTen() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .skip(1)
                .limit(10)
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo("SELECT * FROM \"sensor_reading\" LIMIT $1, $2");
        assertThat(sql.parameters()).containsExactly(1L, 11L);
    }

    @Test
    void shouldConvertLargeOffsetAndLimit() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .skip(1_000_000)
                .limit(250_000)
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo("SELECT * FROM \"sensor_reading\" LIMIT $1, $2");
        assertThat(sql.parameters()).containsExactly(1_000_000L, 1_250_000L);
    }

    @Test
    void shouldPreserveWhereOrderAndBindingWithOffsetPagination() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .where("sensor").eq("warehouse-1")
                .orderBy("_id").desc()
                .skip(10)
                .limit(20)
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo(
                "SELECT * FROM \"sensor_reading\" WHERE \"sensor\" = $1 "
                        + "ORDER BY \"timestamp\" DESC LIMIT $2, $3");
        assertThat(sql.parameters()).containsExactly("warehouse-1", 10L, 30L);
    }

    @Test
    void shouldLeaveQueryWithoutPaginationUnchanged() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .where("sensor").eq("warehouse-1")
                .orderBy("_id").asc()
                .build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo(
                "SELECT * FROM \"sensor_reading\" WHERE \"sensor\" = $1 ORDER BY \"timestamp\" ASC");
        assertThat(sql.parameters()).containsExactly("warehouse-1");
    }

    @Test
    void shouldConvertTemporalIdentifierAndInPredicate() {
        Instant timestamp = Instant.parse("2026-09-15T03:00:00Z");
        CriteriaCondition condition = CriteriaCondition.eq("_id", timestamp)
                .and(CriteriaCondition.in("sensor", List.of("one", "two")));

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(
                SelectQuery.builder().select().from("sensor_reading").where(condition).build());

        assertThat(sql.statement()).isEqualTo(
                "SELECT * FROM \"sensor_reading\" WHERE "
                        + "(\"timestamp\" = $1 AND \"sensor\" IN ($2, $3))");
        assertThat(sql.parameters()).containsExactly(timestamp, "one", "two");
    }

    @Test
    void shouldConvertUpdateAndRejectTimestampMutation() {
        UpdateQuery query = mock(UpdateQuery.class);
        when(query.name()).thenReturn("sensor_reading");
        when(query.set()).thenReturn(List.of(Element.of("temperature", 22.5D)));
        when(query.condition()).thenReturn(Optional.of(CriteriaCondition.eq("sensor", "warehouse-1")));

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.update(query);

        assertThat(sql.statement()).isEqualTo(
                "UPDATE \"sensor_reading\" SET \"temperature\" = $1 WHERE \"sensor\" = $2");
        assertThat(sql.parameters()).containsExactly(22.5D, "warehouse-1");

        UpdateQuery timestampUpdate = mock(UpdateQuery.class);
        when(timestampUpdate.name()).thenReturn("sensor_reading");
        when(timestampUpdate.set()).thenReturn(List.of(Element.of("_id", Instant.now())));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> QuestDBQueryConverter.update(timestampUpdate));

        UpdateQuery nullUpdate = mock(UpdateQuery.class);
        when(nullUpdate.name()).thenReturn("sensor_reading");
        when(nullUpdate.set()).thenReturn(List.of(Element.of("sensor", null)));
        when(nullUpdate.condition()).thenReturn(Optional.empty());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> QuestDBQueryConverter.update(nullUpdate))
                .withMessageContaining("concrete type");
    }

    @Test
    void shouldRejectUnsupportedConditionAndOffsetWithoutLimit() {
        SelectQuery unsupported = SelectQuery.builder().select().from("sensor_reading")
                .where(CriteriaCondition.contains(Element.of("sensor", "house")))
                .build();
        SelectQuery offset = SelectQuery.select().from("sensor_reading").skip(2).build();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> QuestDBQueryConverter.convert(unsupported));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> QuestDBQueryConverter.convert(offset));
    }

    @Test
    void shouldRejectPaginationRangeOverflow() {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .skip(Long.MAX_VALUE)
                .limit(1)
                .build();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBQueryConverter.convert(query))
                .withMessageContaining("range exceeds");
    }

    @Test
    void shouldBindHostileValuesAndRejectHostileIdentifiers() {
        String hostile = "Robert'); DROP TABLE sensor;--";
        SelectQuery valueQuery = SelectQuery.select().from("sensor_reading")
                .where("sensor").eq(hostile).build();

        QuestDBQueryConverter.QuestDBQuery sql = QuestDBQueryConverter.convert(valueQuery);

        assertThat(sql.statement())
                .isEqualTo("SELECT * FROM \"sensor_reading\" WHERE \"sensor\" = $1")
                .doesNotContain(hostile);
        assertThat(sql.parameters()).containsExactly(hostile);

        SelectQuery tableInjection = SelectQuery.select().from("sensor; DROP TABLE sensor").build();
        SelectQuery columnInjection = SelectQuery.select().from("sensor_reading")
                .where("sensor\" OR true --").eq("value").build();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBQueryConverter.convert(tableInjection));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBQueryConverter.convert(columnInjection));
    }
}
