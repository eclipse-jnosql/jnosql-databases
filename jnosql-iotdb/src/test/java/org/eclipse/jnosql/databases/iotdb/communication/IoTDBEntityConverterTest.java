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

import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.write.record.Tablet;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class IoTDBEntityConverterTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-09-15T03:00:00.123456Z");

    @Test
    void shouldConvertTemporalIdentifiersToMilliseconds() {
        long expected = TIMESTAMP.toEpochMilli();

        assertThat(IoTDBEntityConverter.toEpochMillis(TIMESTAMP)).isEqualTo(expected);
        assertThat(IoTDBEntityConverter.toEpochMillis(
                LocalDateTime.ofInstant(TIMESTAMP, ZoneOffset.UTC))).isEqualTo(expected);
        assertThat(IoTDBEntityConverter.toEpochMillis(
                OffsetDateTime.ofInstant(TIMESTAMP, ZoneOffset.ofHours(-3)))).isEqualTo(expected);
        assertThat(IoTDBEntityConverter.toEpochMillis(
                ZonedDateTime.ofInstant(TIMESTAMP, ZoneOffset.ofHours(2)))).isEqualTo(expected);
    }

    @Test
    void shouldBuildNativeTabletWithFieldColumns() {
        CommunicationEntity entity = entity(TIMESTAMP);

        Tablet tablet = IoTDBEntityConverter.tablet(List.of(IoTDBEntityConverter.toRow(entity)));

        assertThat(tablet.getTableName()).isEqualTo("sensor_reading");
        assertThat(tablet.getRowSize()).isOne();
        assertThat(tablet.getTimestamp(0)).isEqualTo(TIMESTAMP.toEpochMilli());
        assertThat(tablet.getSchemas()).extracting(schema -> schema.getType())
                .containsExactlyInAnyOrder(TSDataType.STRING, TSDataType.DOUBLE);
        assertThat(tablet.getColumnTypes()).containsOnly(org.apache.tsfile.enums.ColumnCategory.FIELD);
    }

    @Test
    void shouldConvertSupportedColumnTypes() {
        CommunicationEntity entity = CommunicationEntity.of("types");
        entity.add("_id", TIMESTAMP);
        entity.add("boolean_value", true);
        entity.add("integer_value", 10);
        entity.add("long_value", 20L);
        entity.add("float_value", 1.5F);
        entity.add("double_value", 2.5D);
        entity.add("text_value", UUID.randomUUID());
        entity.add("timestamp_value", TIMESTAMP);
        entity.add("date_value", LocalDate.of(2026, 9, 15));

        Tablet tablet = IoTDBEntityConverter.tablet(List.of(IoTDBEntityConverter.toRow(entity)));
        Map<String, TSDataType> types = tablet.getSchemas().stream()
                .collect(toMap(schema -> schema.getMeasurementName(), schema -> schema.getType()));

        assertThat(types).containsEntry("boolean_value", TSDataType.BOOLEAN)
                .containsEntry("integer_value", TSDataType.INT32)
                .containsEntry("long_value", TSDataType.INT64)
                .containsEntry("float_value", TSDataType.FLOAT)
                .containsEntry("double_value", TSDataType.DOUBLE)
                .containsEntry("text_value", TSDataType.STRING)
                .containsEntry("timestamp_value", TSDataType.TIMESTAMP)
                .containsEntry("date_value", TSDataType.DATE);
    }

    @Test
    void shouldConvertEnumToItsName() {
        CommunicationEntity entity = CommunicationEntity.of("transaction");
        entity.add("_id", TIMESTAMP);
        entity.add("type", TransactionType.OPEN);

        IoTDBEntityConverter.IoTDBRow row = IoTDBEntityConverter.toRow(entity);

        assertThat(row.columns()).singleElement()
                .satisfies(column -> {
                    assertThat(column.type()).isEqualTo(TSDataType.STRING);
                    assertThat(column.value()).isEqualTo("OPEN");
                });
    }

    @Test
    void shouldRejectInvalidIdentifiersAndIds() {
        CommunicationEntity missingId = CommunicationEntity.of("sensor_reading");
        missingId.add("temperature", 20D);
        CommunicationEntity invalidId = CommunicationEntity.of("sensor_reading");
        invalidId.add("_id", 10L);
        invalidId.add("temperature", 20D);
        CommunicationEntity invalidTable = entity(TIMESTAMP);
        invalidTable = CommunicationEntity.of("sensor; DROP TABLE sensor");
        invalidTable.add("_id", TIMESTAMP);
        invalidTable.add("temperature", 20D);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> IoTDBEntityConverter.toRow(missingId));
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> IoTDBEntityConverter.toRow(invalidId));
        CommunicationEntity hostile = invalidTable;
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> IoTDBEntityConverter.toRow(hostile));
    }

    private CommunicationEntity entity(Instant timestamp) {
        CommunicationEntity entity = CommunicationEntity.of("sensor_reading");
        entity.add("_id", timestamp);
        entity.add("sensor", "warehouse-1");
        entity.add("temperature", 21.5D);
        return entity;
    }

    private enum TransactionType {
        OPEN;

        @Override
        public String toString() {
            return "open transaction";
        }
    }
}
