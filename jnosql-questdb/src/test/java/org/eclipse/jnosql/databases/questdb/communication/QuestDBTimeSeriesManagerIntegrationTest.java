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

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class QuestDBTimeSeriesManagerIntegrationTest {

    private static QuestDBTimeSeriesManager manager;

    @BeforeAll
    static void setUp() {
        manager = QuestDBDatabase.INSTANCE.manager();
    }

    @Test
    void shouldInsertBatchQueryUpdateOrderAndLimit() {
        String sensor = UUID.randomUUID().toString();
        Instant first = Instant.now().minusSeconds(2).truncatedTo(ChronoUnit.MICROS);
        Instant second = first.plusSeconds(1);
        manager.insert(List.of(entity(first, sensor, 21.5D), entity(second, sensor, 22.5D)));

        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .where("sensor").eq(sensor)
                .and("_id").gte(first)
                .orderBy("_id").desc()
                .limit(1)
                .build();
        awaitCount(query, 1L);
        assertThat(manager.select(query)).singleElement()
                .satisfies(entity -> {
                    assertThat(entity.find("_id", Instant.class)).contains(second);
                    assertThat(entity.find("temperature", Double.class)).contains(22.5D);
                });

        manager.update(entity(second, sensor, 23.5D));
        awaitTemperature(second, 23.5D);
    }

    @Test
    void shouldSelectFirstAndSecondPagesOfTenRecords() {
        String table = "pagination_" + UUID.randomUUID().toString().replace("-", "");
        String sensor = UUID.randomUUID().toString();
        Instant first = Instant.now().minusSeconds(20).truncatedTo(ChronoUnit.MICROS);
        List<CommunicationEntity> records = IntStream.range(0, 20)
                .mapToObj(index -> entity(table, first.plusSeconds(index), sensor, 20D + index))
                .toList();
        manager.insert(records);

        SelectQuery allRecords = SelectQuery.select().from(table)
                .where("sensor").eq(sensor)
                .build();
        awaitCount(allRecords, records.size());
        SelectQuery firstPage = SelectQuery.select().from(table)
                .where("sensor").eq(sensor)
                .orderBy("_id").asc()
                .limit(10)
                .build();
        SelectQuery secondPage = SelectQuery.select().from(table)
                .where("sensor").eq(sensor)
                .orderBy("_id").asc()
                .skip(10)
                .limit(10)
                .build();

        assertThat(manager.select(firstPage))
                .extracting(entity -> entity.find("_id", Instant.class).orElseThrow())
                .containsExactlyElementsOf(IntStream.range(0, 10)
                        .mapToObj(first::plusSeconds)
                        .toList());
        assertThat(manager.select(secondPage))
                .extracting(entity -> entity.find("_id", Instant.class).orElseThrow())
                .containsExactlyElementsOf(IntStream.range(10, 20)
                        .mapToObj(first::plusSeconds)
                        .toList());
    }

    @Test
    void shouldTreatSqlInjectionPayloadAsAValue() {
        String hostile = "Robert'); DROP TABLE sensor_reading;--";
        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MICROS);

        manager.insert(entity(timestamp, hostile, 19.5D));

        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .where("sensor").eq(hostile).build();
        awaitCount(query, 1L);
        assertThat(manager.select(query)).singleElement()
                .satisfies(entity -> assertThat(entity.find("sensor", String.class)).contains(hostile));

        assertThat(manager.count("sensor_reading")).isPositive();
    }

    @Test
    void shouldDelegateTableCreationAndColumnEvolutionToQuestDB() {
        String table = "auto_schema_" + UUID.randomUUID().toString().replace("-", "");
        Instant first = Instant.now().minusSeconds(1).truncatedTo(ChronoUnit.MICROS);
        Instant second = first.plusSeconds(1);
        CommunicationEntity initial = entity(table, first, "first", 18.5D);
        CommunicationEntity evolved = entity(table, second, "second", 19.5D);
        evolved.add("location", "warehouse");

        manager.insert(initial);
        manager.insert(evolved);

        SelectQuery query = SelectQuery.select().from(table).where("_id").eq(second).build();
        awaitCount(query, 1L);
        assertThat(manager.select(query)).singleElement()
                .satisfies(entity -> assertThat(entity.find("location", String.class)).contains("warehouse"));
    }

    private static void awaitCount(SelectQuery query, long expected) {
        long deadline = System.currentTimeMillis() + 10_000L;
        while (manager.count(query) < expected) {
            if (System.currentTimeMillis() >= deadline) {
                throw new AssertionError("Timed out waiting for QuestDB rows");
            }
            LockSupport.parkNanos(10_000_000L);
        }
    }

    private static void awaitTemperature(Instant timestamp, double expected) {
        SelectQuery query = SelectQuery.select().from("sensor_reading")
                .where("_id").eq(timestamp).build();
        long deadline = System.currentTimeMillis() + 10_000L;
        while (manager.select(query)
                .map(entity -> entity.find("temperature", Double.class).orElse(Double.NaN))
                .noneMatch(value -> Double.compare(value, expected) == 0)) {
            if (System.currentTimeMillis() >= deadline) {
                throw new AssertionError("Timed out waiting for QuestDB update");
            }
            LockSupport.parkNanos(10_000_000L);
        }
    }

    private static CommunicationEntity entity(Instant timestamp, String sensor, double value) {
        return entity("sensor_reading", timestamp, sensor, value);
    }

    private static CommunicationEntity entity(String table, Instant timestamp, String sensor, double value) {
        CommunicationEntity entity = CommunicationEntity.of(table);
        entity.add("_id", timestamp);
        entity.add("sensor", sensor);
        entity.add("temperature", value);
        return entity;
    }
}
