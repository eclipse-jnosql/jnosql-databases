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
package org.eclipse.jnosql.databases.influxdb.communication;

import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InfluxDBQueryConverterTest {

    @Test
    void shouldConvertPredicatesProjectionLimitAndOrder() {
        SelectQuery query = SelectQuery.select("location", "value")
                .from("temperature")
                .where("location").eq("Lisbon")
                .and("value").gte(20D)
                .orderBy("_id").desc()
                .limit(10)
                .skip(2)
                .build();

        InfluxDBQueryConverter.InfluxDBQuery sql = InfluxDBQueryConverter.convert(query);

        assertThat(sql.statement()).isEqualTo(
                "SELECT \"time\", \"location\", \"value\" FROM \"temperature\" "
                        + "WHERE (\"location\" = $p0 AND \"value\" >= $p1) "
                        + "ORDER BY \"time\" DESC LIMIT 10 OFFSET 2");
        assertThat(sql.parameters()).containsEntry("p0", "Lisbon").containsEntry("p1", 20D);
    }

    @Test
    void shouldConvertTemporalIdentifierAndInPredicate() {
        Instant timestamp = Instant.parse("2026-09-08T17:00:00Z");
        CriteriaCondition condition = CriteriaCondition.eq("_id", timestamp)
                .and(CriteriaCondition.in("location", List.of("Lisbon", "Porto")));

        InfluxDBQueryConverter.InfluxDBQuery sql = InfluxDBQueryConverter.convert(
                SelectQuery.builder().select().from("temperature").where(condition).build());

        assertThat(sql.statement()).isEqualTo(
                "SELECT * FROM \"temperature\" WHERE "
                        + "(\"time\" = $p0 AND \"location\" IN ($p1, $p2))");
        assertThat(sql.parameters())
                .containsEntry("p0", timestamp.toString())
                .containsEntry("p1", "Lisbon")
                .containsEntry("p2", "Porto");
    }

    @Test
    void shouldQuoteIdentifiersAndKeepValuesAsParameters() {
        SelectQuery query = SelectQuery.select().from("temperature\"archive")
                .where("location").eq("a\"b\\c")
                .build();

        InfluxDBQueryConverter.InfluxDBQuery sql = InfluxDBQueryConverter.convert(query);

        assertThat(sql.statement()).contains("FROM \"temperature\"\"archive\"")
                .doesNotContain("a\"b\\c");
        assertThat(sql.parameters()).containsEntry("p0", "a\"b\\c");
    }

    @Test
    void shouldRejectUnsupportedCondition() {
        SelectQuery query = SelectQuery.builder().select().from("temperature")
                .where(CriteriaCondition.contains(
                        org.eclipse.jnosql.communication.semistructured.Element.of("location", "is")))
                .build();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> InfluxDBQueryConverter.convert(query));
    }

    @Test
    void shouldRejectSkipWithoutLimit() {
        SelectQuery query = SelectQuery.select().from("temperature").skip(2).build();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> InfluxDBQueryConverter.convert(query));
    }
}
