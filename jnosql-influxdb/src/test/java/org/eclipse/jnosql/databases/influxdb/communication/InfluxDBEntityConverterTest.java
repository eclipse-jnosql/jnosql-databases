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

import com.influxdb.v3.client.Point;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InfluxDBEntityConverterTest {

    private static final Instant TIMESTAMP = Instant.parse("2026-09-08T17:00:00.123456789Z");

    @ParameterizedTest
    @MethodSource("temporalIdentifiers")
    void shouldConvertSupportedTemporalIdentifiers(Object identifier) {
        CommunicationEntity entity = entity(identifier);

        Point point = InfluxDBEntityConverter.toPoint(entity);

        long epochNanos = TIMESTAMP.getEpochSecond() * 1_000_000_000L + TIMESTAMP.getNano();
        assertThat(point.getTimestamp().longValue()).isEqualTo(epochNanos);
        assertThat(point.getField("location")).isEqualTo("Lisbon");
        assertThat(point.getField("value")).isEqualTo(21.5D);
        assertThat(point.getField(InfluxDBEntityConverter.ID_FIELD)).isNull();
    }

    @Test
    void shouldRejectMissingIdentifier() {
        CommunicationEntity entity = CommunicationEntity.of("temperature");
        entity.add("value", 21.5D);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> InfluxDBEntityConverter.toPoint(entity))
                .withMessageContaining("_id");
    }

    @Test
    void shouldRejectNonTemporalIdentifier() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> InfluxDBEntityConverter.toPoint(entity("not-a-time")))
                .withMessageContaining("Instant");
    }

    @Test
    void shouldRejectUnsupportedField() {
        CommunicationEntity entity = entity(TIMESTAMP);
        entity.add("metadata", java.util.List.of("one", "two"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> InfluxDBEntityConverter.toPoint(entity))
                .withMessageContaining("metadata");
    }

    @Test
    void shouldConvertQueryRowToEntity() {
        long epochNanos = TIMESTAMP.getEpochSecond() * 1_000_000_000L + TIMESTAMP.getNano();
        Map<String, Object> row = Map.of(
                "time", epochNanos,
                "location", "Lisbon",
                "value", 21.5D);

        CommunicationEntity entity = InfluxDBEntityConverter.toEntity("temperature", row);

        assertThat(entity.find("_id", Instant.class)).contains(TIMESTAMP);
        assertThat(entity.find("location", String.class)).contains("Lisbon");
        assertThat(entity.find("value", Double.class)).contains(21.5D);
    }

    @Test
    void shouldConvertInstantQueryTimestamp() {
        CommunicationEntity entity = InfluxDBEntityConverter.toEntity(
                "temperature", Map.of("time", TIMESTAMP, "value", 21.5D));

        assertThat(entity.find("_id", Instant.class)).contains(TIMESTAMP);
    }

    @Test
    void shouldConvertBigIntegerQueryTimestampBeforeEpoch() {
        Instant timestamp = Instant.parse("1969-12-31T23:59:59.123456789Z");
        BigInteger epochNanos = BigInteger.valueOf(timestamp.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(timestamp.getNano()));

        CommunicationEntity entity = InfluxDBEntityConverter.toEntity(
                "temperature", Map.of("time", epochNanos, "value", 21.5D));

        assertThat(entity.find("_id", Instant.class)).contains(timestamp);
    }

    @Test
    void shouldRejectNonIntegralQueryTimestamp() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> InfluxDBEntityConverter.toEntity(
                        "temperature", Map.of("time", 1.5D, "value", 21.5D)))
                .withMessageContaining("timestamp");
    }

    private static Stream<Arguments> temporalIdentifiers() {
        return Stream.of(
                Arguments.of(TIMESTAMP),
                Arguments.of(LocalDateTime.ofInstant(TIMESTAMP, ZoneOffset.UTC)),
                Arguments.of(OffsetDateTime.ofInstant(TIMESTAMP, ZoneOffset.ofHours(1))),
                Arguments.of(ZonedDateTime.ofInstant(TIMESTAMP, ZoneId.of("Europe/Lisbon"))));
    }

    private static CommunicationEntity entity(Object identifier) {
        CommunicationEntity entity = CommunicationEntity.of("temperature");
        entity.add("_id", identifier);
        entity.add("location", "Lisbon");
        entity.add("value", 21.5D);
        return entity;
    }
}
