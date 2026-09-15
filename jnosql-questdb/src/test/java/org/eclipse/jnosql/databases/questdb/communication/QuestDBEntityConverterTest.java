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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class QuestDBEntityConverterTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-09-15T03:00:00.123456Z");

    @ParameterizedTest
    @MethodSource("temporalIdentifiers")
    void shouldConvertSupportedTemporalIdentifiers(Object identifier) {
        QuestDBEntityConverter.QuestDBRow row = QuestDBEntityConverter.toRow(entity(identifier));

        assertThat(row.timestamp()).isEqualTo(TIMESTAMP);
        assertThat(row.columns()).extracting(QuestDBEntityConverter.QuestDBColumn::name)
                .containsExactlyInAnyOrder("sensor", "temperature");
    }

    @Test
    void shouldRejectMissingIdentifier() {
        CommunicationEntity entity = CommunicationEntity.of("sensor_reading");
        entity.add("temperature", 21.5D);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBEntityConverter.toRow(entity))
                .withMessageContaining("_id");
    }

    @Test
    void shouldRejectNonTemporalIdentifier() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBEntityConverter.toRow(entity("invalid")))
                .withMessageContaining("Instant");
    }

    @Test
    void shouldRejectUnsupportedColumn() {
        CommunicationEntity entity = entity(TIMESTAMP);
        entity.add("metadata", List.of("one", "two"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBEntityConverter.toRow(entity))
                .withMessageContaining("metadata");
    }

    @Test
    void shouldOmitNullColumns() {
        CommunicationEntity entity = entity(TIMESTAMP);
        entity.add("description", null);

        QuestDBEntityConverter.QuestDBRow row = QuestDBEntityConverter.toRow(entity);

        assertThat(row.columns()).extracting(QuestDBEntityConverter.QuestDBColumn::name)
                .doesNotContain("description");
    }

    @Test
    void shouldRejectNullColumnsForEntityUpdates() {
        CommunicationEntity entity = entity(TIMESTAMP);
        entity.add("description", null);

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> QuestDBEntityConverter.toUpdateRow(entity))
                .withMessageContaining("description");
    }

    @Test
    void shouldRejectReservedTimestampColumn() {
        CommunicationEntity entity = entity(TIMESTAMP);
        entity.add("timestamp", TIMESTAMP);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBEntityConverter.toRow(entity))
                .withMessageContaining("reserved");
    }

    @Test
    void shouldRejectUnsafeTableAndColumnIdentifiers() {
        CommunicationEntity unsafeTable = CommunicationEntity.of("sensor; DROP TABLE readings");
        unsafeTable.add("_id", TIMESTAMP);
        unsafeTable.add("temperature", 21.5D);

        CommunicationEntity unsafeColumn = entity(TIMESTAMP);
        unsafeColumn.add("sensor\" OR 1=1 --", "hostile");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBEntityConverter.toRow(unsafeTable))
                .withMessageContaining("Invalid QuestDB identifier");
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuestDBEntityConverter.toRow(unsafeColumn))
                .withMessageContaining("Invalid QuestDB identifier");
    }

    private static Stream<Arguments> temporalIdentifiers() {
        return Stream.of(
                Arguments.of(TIMESTAMP),
                Arguments.of(LocalDateTime.ofInstant(TIMESTAMP, ZoneOffset.UTC)),
                Arguments.of(OffsetDateTime.ofInstant(TIMESTAMP, ZoneOffset.ofHours(1))),
                Arguments.of(ZonedDateTime.ofInstant(TIMESTAMP, ZoneId.of("Europe/Lisbon"))));
    }

    private static CommunicationEntity entity(Object identifier) {
        CommunicationEntity entity = CommunicationEntity.of("sensor_reading");
        entity.add("_id", identifier);
        entity.add("sensor", "warehouse-1");
        entity.add("temperature", 21.5D);
        return entity;
    }
}
