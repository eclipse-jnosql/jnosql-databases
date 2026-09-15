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

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class IoTDBTimeSeriesManagerIntegrationTest {

    private static IoTDBTimeSeriesManager manager;

    @BeforeAll
    static void setUp() {
        manager = IoTDBDatabase.INSTANCE.manager();
    }

    @Test
    void shouldAutoCreateSchemaInsertBatchQueryOrderLimitAndDelete() {
        String table = "sensor_" + UUID.randomUUID().toString().replace("-", "");
        String sensor = UUID.randomUUID().toString();
        Instant first = Instant.now().minusSeconds(2).truncatedTo(ChronoUnit.MILLIS);
        Instant second = first.plusSeconds(1);
        manager.insert(List.of(entity(table, first, sensor, 21.5D), entity(table, second, sensor, 22.5D)));

        CommunicationEntity evolved = entity(table, second.plusSeconds(1), sensor, 23.5D);
        evolved.add("active", true);
        manager.insert(evolved);

        SelectQuery query = SelectQuery.select().from(table)
                .where("sensor").eq(sensor)
                .and("_id").gte(first)
                .orderBy("_id").desc()
                .limit(2)
                .build();
        assertThat(manager.select(query)).hasSize(2)
                .first()
                .satisfies(result -> assertThat(result.find("_id", Instant.class))
                        .contains(second.plusSeconds(1)));
        assertThat(manager.count(SelectQuery.select().from(table).where("sensor").eq(sensor).build()))
                .isEqualTo(3L);
        assertThat(manager.select(SelectQuery.select().from(table)
                .where("sensor").eq(sensor)
                .orderBy("_id").asc()
                .skip(1)
                .limit(1)
                .build())).singleElement()
                .satisfies(result -> assertThat(result.find("_id", Instant.class)).contains(second));

        manager.delete(DeleteQuery.delete().from(table).where("_id").eq(second).build());
        assertThat(manager.select(SelectQuery.select().from(table).where("_id").eq(second).build())).isEmpty();
    }

    @Test
    void shouldTreatSqlInjectionPayloadAsData() {
        String table = "injection_" + UUID.randomUUID().toString().replace("-", "");
        List<String> hostileValues = List.of(
                "Robert'); DROP TABLE sensor;--",
                "x' OR '1'='1",
                "x\\'); DROP TABLE sensor;--",
                "x'; SELECT /*",
                "line1\n' OR TRUE --",
                "comment/**/' OR 1=1 --");
        Instant first = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        List<CommunicationEntity> entities = new java.util.ArrayList<>();
        for (int index = 0; index < hostileValues.size(); index++) {
            entities.add(entity(table, first.plusMillis(index), hostileValues.get(index), 19.5D + index));
        }
        manager.insert(entities);

        for (String hostile : hostileValues) {
            SelectQuery query = SelectQuery.select().from(table).where("sensor").eq(hostile).build();
            assertThat(manager.select(query)).singleElement()
                    .satisfies(result -> assertThat(result.find("sensor", String.class)).contains(hostile));
        }
        assertThat(manager.count(table)).isEqualTo(hostileValues.size());
    }

    private static CommunicationEntity entity(String table, Instant timestamp, String sensor, double value) {
        CommunicationEntity entity = CommunicationEntity.of(table);
        entity.add("_id", timestamp);
        entity.add("sensor", sensor);
        entity.add("temperature", value);
        return entity;
    }
}
