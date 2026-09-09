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
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

final class InfluxDBEntityConverter {

    static final String ID_FIELD = "_id";

    private InfluxDBEntityConverter() {
    }

    static Point toPoint(CommunicationEntity entity) {
        Element identifier = entity.find(ID_FIELD)
                .orElseThrow(() -> new IllegalArgumentException(
                        "InfluxDB entities require a temporal '" + ID_FIELD + "' identifier"));
        Point point = Point.measurement(entity.name())
                .setTimestamp(toInstant(identifier.get()));

        entity.elements().stream()
                .filter(element -> !ID_FIELD.equals(element.name()))
                .forEach(element -> addField(point, element));

        if (!point.hasFields()) {
            throw new IllegalArgumentException("InfluxDB entities require at least one field");
        }
        return point;
    }

    static CommunicationEntity toEntity(String measurement, Map<String, Object> row) {
        Object timeValue = row.get("time");
        Instant time = queryTimestamp(timeValue);
        CommunicationEntity entity = CommunicationEntity.of(measurement);
        entity.add(ID_FIELD, time);
        row.forEach((name, value) -> {
            if (!"time".equals(name) && value != null) {
                entity.add(name, value);
            }
        });
        return entity;
    }

    private static Instant queryTimestamp(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof BigInteger) {
            Number number = (Number) value;
            BigInteger nanos = number instanceof BigInteger bigInteger
                    ? bigInteger : BigInteger.valueOf(number.longValue());
            try {
                BigInteger[] secondsAndNanos = nanos.divideAndRemainder(BigInteger.valueOf(1_000_000_000L));
                long seconds = secondsAndNanos[0].longValueExact();
                long nanoAdjustment = secondsAndNanos[1].longValue();
                if (nanoAdjustment < 0) {
                    seconds--;
                    nanoAdjustment += 1_000_000_000L;
                }
                return Instant.ofEpochSecond(seconds, nanoAdjustment);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("InfluxDB result timestamp is outside the supported range", exception);
            }
        }
        throw new IllegalArgumentException("InfluxDB result does not contain a timestamp");
    }

    static Instant toInstant(Object value) {
        Object converted = value instanceof org.eclipse.jnosql.communication.Value jnosqlValue
                ? ValueUtil.convert(jnosqlValue) : value;
        if (converted == null) {
            throw new IllegalArgumentException("InfluxDB identifier cannot be null");
        }
        if (converted instanceof Instant) {
            return (Instant) converted;
        }
        if (converted instanceof LocalDateTime) {
            return ((LocalDateTime) converted).toInstant(ZoneOffset.UTC);
        }
        if (converted instanceof OffsetDateTime) {
            return ((OffsetDateTime) converted).toInstant();
        }
        if (converted instanceof ZonedDateTime) {
            return ((ZonedDateTime) converted).toInstant();
        }
        throw new IllegalArgumentException(
                "InfluxDB identifier must be Instant, LocalDateTime, OffsetDateTime, or ZonedDateTime");
    }

    private static void addField(Point point, Element element) {
        Object value = ValueUtil.convert(element.value());
        if (value == null) {
            throw new IllegalArgumentException("InfluxDB field '" + element.name() + "' cannot be null");
        }
        if (value instanceof Boolean) {
            point.setField(element.name(), (Boolean) value);
        } else if (value instanceof Number) {
            point.setField(element.name(), (Number) value);
        } else if (value instanceof Character) {
            point.setField(element.name(), value.toString());
        } else if (value instanceof CharSequence) {
            point.setField(element.name(), value.toString());
        } else {
            throw new IllegalArgumentException(
                    "InfluxDB field '" + element.name() + "' must be a string, number, boolean, or character");
        }
    }
}
