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

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class InfluxDBTimeSeriesManagerIntegrationTest {

    private static InfluxDBTimeSeriesManager manager;

    @BeforeAll
    static void setUp() {
        manager = InfluxDBDatabase.INSTANCE.manager();
    }

    @Test
    void shouldInsertBatchQueryOrderAndLimit() {
        Instant first = Instant.now().minusSeconds(2);
        Instant second = first.plusSeconds(1);
        List<CommunicationEntity> entities = List.of(entity(first, 21.5D), entity(second, 22.5D));

        manager.insert(entities);

        SelectQuery query = SelectQuery.select().from("temperature")
                .where("location").eq("Lisbon")
                .orderBy("_id").desc()
                .limit(1)
                .build();
        assertThat(manager.select(query)).singleElement()
                .satisfies(entity -> {
                    assertThat(entity.find("_id", Instant.class)).contains(second);
                    assertThat(entity.find("value", Double.class)).contains(22.5D);
                });

    }

    private static CommunicationEntity entity(Instant timestamp, double value) {
        CommunicationEntity entity = CommunicationEntity.of("temperature");
        entity.add("_id", timestamp);
        entity.add("location", "Lisbon");
        entity.add("value", value);
        return entity;
    }
}
