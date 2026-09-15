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

import org.apache.iotdb.isession.ITableSession;
import org.apache.iotdb.isession.pool.ITableSessionPool;
import org.apache.tsfile.write.record.Tablet;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IoTDBTimeSeriesManagerTest {

    private ITableSession session;
    private IoTDBTimeSeriesManager manager;

    @BeforeEach
    void setUp() throws Exception {
        ITableSessionPool pool = mock(ITableSessionPool.class);
        session = mock(ITableSession.class);
        when(pool.getSession()).thenReturn(session);
        manager = new IoTDBTimeSeriesManager(pool, "metrics");
    }

    @Test
    void shouldUseOneNativeTabletForCompatibleBatch() throws Exception {
        Instant first = Instant.parse("2026-09-15T03:00:00Z");
        CommunicationEntity one = entity(first, 21.5D);
        CommunicationEntity two = entity(first.plusSeconds(1), 22.5D);

        assertThat(manager.insert(List.of(one, two))).containsExactly(one, two);

        ArgumentCaptor<Tablet> captor = ArgumentCaptor.forClass(Tablet.class);
        verify(session).insert(captor.capture());
        assertThat(captor.getValue().getRowSize()).isEqualTo(2);
    }

    @Test
    void shouldRejectUnsupportedUpdateAndTtl() {
        CommunicationEntity entity = entity(Instant.now(), 21.5D);
        UpdateQuery query = org.mockito.Mockito.mock(UpdateQuery.class);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.update(entity));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.update(query));
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> manager.insert(entity, Duration.ofMinutes(1)));
    }

    private CommunicationEntity entity(Instant timestamp, double value) {
        CommunicationEntity entity = CommunicationEntity.of("sensor_reading");
        entity.add("_id", timestamp);
        entity.add("sensor", "warehouse-1");
        entity.add("temperature", value);
        return entity;
    }
}
