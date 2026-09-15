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

import io.questdb.client.QuestDB;
import io.questdb.client.Sender;
import io.questdb.client.cutlass.qwp.client.QwpBindValues;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuestDBTimeSeriesManagerTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-09-15T03:00:00Z");

    private QuestDBTimeSeriesManager manager;

    private Sender sender;

    @BeforeEach
    void setUp() {
        QuestDB questDB = mock(QuestDB.class);
        sender = mock(Sender.class);
        when(questDB.borrowSender()).thenReturn(sender);
        when(sender.table(anyString())).thenReturn(sender);
        when(sender.stringColumn(anyString(), anyString())).thenReturn(sender);
        when(sender.doubleColumn(anyString(), org.mockito.ArgumentMatchers.anyDouble())).thenReturn(sender);
        when(sender.drain(60_000L)).thenReturn(true);

        manager = new QuestDBTimeSeriesManager(questDB, "qdb");
    }

    @Test
    void shouldInsertSingleAndBatchWithNativeSender() {
        CommunicationEntity first = entity(TIMESTAMP, 21.5D);
        CommunicationEntity second = entity(TIMESTAMP.plusSeconds(1), 22.5D);

        assertThat(manager.insert(first)).isSameAs(first);
        assertThat(manager.insert(List.of(first, second))).containsExactly(first, second);

        verify(sender, times(2)).flush();
        verify(sender, times(2)).drain(60_000L);
        verify(sender, times(3)).table("sensor_reading");
    }

    @Test
    void shouldRejectUnsupportedOperations() {
        CommunicationEntity entity = entity(TIMESTAMP, 21.5D);
        DeleteQuery delete = DeleteQuery.delete().from("sensor_reading")
                .where("_id").eq(TIMESTAMP).build();

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.insert(entity, Duration.ofMinutes(1)));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.delete(delete));
    }

    @Test
    void shouldBindHostileTextThroughNativeQwpParameters() {
        QwpBindValues binds = mock(QwpBindValues.class);
        String hostile = "Robert'); DROP TABLE sensor;--";

        QuestDBTimeSeriesManager.bind(binds, List.of(hostile));

        verify(binds).setVarchar(0, hostile);
    }

    private CommunicationEntity entity(Instant timestamp, double value) {
        CommunicationEntity entity = CommunicationEntity.of("sensor_reading");
        entity.add("_id", timestamp);
        entity.add("sensor", "warehouse-1");
        entity.add("temperature", value);
        return entity;
    }
}
