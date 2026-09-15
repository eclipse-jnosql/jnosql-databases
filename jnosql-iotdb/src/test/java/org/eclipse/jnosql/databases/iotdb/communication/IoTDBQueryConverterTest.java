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
package org.eclipse.jnosql.databases.iotdb.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class IoTDBQueryConverterTest {

    @Test
    void shouldConvertPredicatesProjectionOrderingAndLimit() {
        SelectQuery query = SelectQuery.select("sensor", "temperature")
                .from("sensor_reading")
                .where("sensor").eq("warehouse-1")
                .and("temperature").gte(20D)
                .orderBy("_id").desc()
                .limit(10)
                .build();

        assertThat(IoTDBQueryConverter.convert(query)).isEqualTo(
                "SELECT \"time\", \"sensor\", \"temperature\" FROM \"sensor_reading\" "
                        + "WHERE (\"sensor\" = 'warehouse-1' AND \"temperature\" >= 20.0) "
                        + "ORDER BY \"time\" DESC LIMIT 10");
    }

    @Test
    void shouldEscapeHostileValuesAndRejectHostileIdentifiers() {
        List<String> hostileValues = List.of(
                "Robert'); DROP TABLE sensor;--",
                "x' OR '1'='1",
                "x\\'); DROP TABLE sensor;--",
                "x'; SELECT /*",
                "line1\n' OR TRUE --",
                "comment/**/' OR 1=1 --");

        for (String hostile : hostileValues) {
            String sql = IoTDBQueryConverter.convert(SelectQuery.select().from("sensor_reading")
                    .where("sensor").eq(hostile).build());

            assertThat(sql)
                    .startsWith("SELECT * FROM \"sensor_reading\" WHERE \"sensor\" = '")
                    .endsWith("'")
                    .contains(hostile.replace("'", "''"));
        }

        List<String> hostileIdentifiers = List.of(
                "sensor; DROP TABLE sensor",
                "sensor\" OR true --",
                "sensor/*comment*/",
                "sensor--comment",
                "sensor name");
        for (String hostile : hostileIdentifiers) {
            SelectQuery tableInjection = SelectQuery.select().from(hostile).build();
            SelectQuery columnInjection = SelectQuery.select().from("sensor_reading")
                    .where(hostile).eq("value").build();
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> IoTDBQueryConverter.convert(tableInjection));
            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> IoTDBQueryConverter.convert(columnInjection));
        }
    }

    @Test
    void shouldRejectNonFiniteNumericLiterals() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> IoTDBQueryConverter.literal("temperature", Double.NaN));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> IoTDBQueryConverter.literal("temperature", Double.POSITIVE_INFINITY));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> IoTDBQueryConverter.literal("temperature", Float.NEGATIVE_INFINITY));
    }

    @Test
    void shouldConvertTemporalDeleteAndRejectFieldDelete() {
        Instant first = Instant.parse("2026-09-15T03:00:00Z");
        Instant second = first.plusSeconds(10);
        DeleteQuery temporal = DeleteQuery.delete().from("sensor_reading")
                .where("_id").gte(first).and("_id").lte(second).build();

        assertThat(IoTDBQueryConverter.delete(temporal)).isEqualTo(
                "DELETE FROM \"sensor_reading\" WHERE "
                        + "(\"time\" >= " + first.toEpochMilli()
                        + " AND \"time\" <= " + second.toEpochMilli() + ")");

        DeleteQuery field = DeleteQuery.delete().from("sensor_reading")
                .where("sensor").eq("warehouse-1").build();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> IoTDBQueryConverter.delete(field))
                .withMessageContaining("temporal identifier");
    }

    @Test
    void shouldRejectUnsupportedCondition() {
        SelectQuery unsupported = SelectQuery.builder().select().from("sensor_reading")
                .where(CriteriaCondition.contains(
                        org.eclipse.jnosql.communication.semistructured.Element.of("sensor", "house")))
                .build();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> IoTDBQueryConverter.convert(unsupported));
    }
}
