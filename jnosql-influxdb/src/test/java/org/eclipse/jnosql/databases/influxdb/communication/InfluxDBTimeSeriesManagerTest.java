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

import com.influxdb.v3.client.InfluxDBClient;
import com.influxdb.v3.client.Point;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InfluxDBTimeSeriesManagerTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-09-08T17:00:00Z");

    private InfluxDBClient client;

    private InfluxDBTimeSeriesManager manager;

    @BeforeEach
    void setUp() {
        client = mock(InfluxDBClient.class);
        manager = new InfluxDBTimeSeriesManager(client, "metrics");
    }

    @Test
    void shouldInsert() {
        CommunicationEntity entity = entity(TIMESTAMP, 21.5D);

        assertThat(manager.insert(entity)).isSameAs(entity);

        ArgumentCaptor<Point> captor = ArgumentCaptor.forClass(Point.class);
        verify(client).writePoint(captor.capture());
        assertThat(captor.getValue().getField("value")).isEqualTo(21.5D);
    }

    @Test
    void shouldInsertBatch() {
        List<CommunicationEntity> entities = List.of(
                entity(TIMESTAMP, 21.5D),
                entity(TIMESTAMP.plusSeconds(1), 22.5D));

        assertThat(manager.insert(entities)).containsExactlyElementsOf(entities);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Point>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).writePoints(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void shouldSelect() {
        Map<String, Object> row = Map.of(
                "time", TIMESTAMP,
                "location", "Lisbon",
                "value", 21.5D);
        when(client.queryRows(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap())).thenReturn(Stream.of(row));
        SelectQuery query = SelectQuery.select().from("temperature")
                .where("location").eq("Lisbon").limit(1).build();

        assertThat(manager.select(query)).singleElement()
                .satisfies(entity -> {
                    assertThat(entity.name()).isEqualTo("temperature");
                    assertThat(entity.find("_id", Instant.class)).contains(TIMESTAMP);
                    assertThat(entity.find("location", String.class)).contains("Lisbon");
                });

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(client).queryRows(captor.capture(), parameters.capture());
        assertThat(captor.getValue()).contains("LIMIT 1");
        assertThat(parameters.getValue()).containsValue("Lisbon");
    }

    @Test
    void shouldRejectDeleteByExactTemporalIdentifier() {
        DeleteQuery query = DeleteQuery.delete().from("temperature")
                .where("_id").eq(TIMESTAMP).build();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.delete(query))
                .withMessageContaining("InfluxDB 3");
    }

    @Test
    void shouldRejectUnsafeDelete() {
        DeleteQuery query = DeleteQuery.delete().from("temperature")
                .where("location").eq("Lisbon").build();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.delete(query))
                .withMessageContaining("InfluxDB 3");
    }

    @Test
    void shouldRejectUnsupportedOperations() {
        CommunicationEntity entity = entity(TIMESTAMP, 21.5D);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.insert(entity, Duration.ofMinutes(1)));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.update(entity));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.update(List.of(entity)));
    }

    private CommunicationEntity entity(Instant timestamp, double value) {
        CommunicationEntity entity = CommunicationEntity.of("temperature");
        entity.add("_id", timestamp);
        entity.add("location", "Lisbon");
        entity.add("value", value);
        return entity;
    }
}
