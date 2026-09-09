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
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.eclipse.jnosql.databases.influxdb.communication.InfluxDBEntityConverter.ID_FIELD;

/**
 * Stores mapped time-series entities in one InfluxDB 3 database.
 *
 * <p>The entity name becomes the measurement, {@code _id} becomes the point timestamp, and
 * all remaining scalar values become fields. Identifiers must be {@link Instant},
 * {@link java.time.LocalDateTime}, {@link OffsetDateTime}, or {@link java.time.ZonedDateTime}.
 * A local date-time is interpreted as UTC.</p>
 *
 * <p>InfluxDB does not provide entity replacement semantics through its Java client. Consequently,
 * update operations, generated identifiers, per-point TTL, and deletion are intentionally unsupported.
 * Configure data retention at the InfluxDB database level.</p>
 */
public class InfluxDBTimeSeriesManager implements DatabaseManager {

    private final InfluxDBClient client;

    private final String database;

    InfluxDBTimeSeriesManager(InfluxDBClient client, String database) {
        this.client = client;
        this.database = database;
    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        Point point = InfluxDBEntityConverter.toPoint(entity);
        client.writePoint(point);
        return entity;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("InfluxDB does not support per-point TTL");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        List<CommunicationEntity> values = StreamSupport.stream(entities.spliterator(), false).toList();
        List<Point> points = values.stream().map(InfluxDBEntityConverter::toPoint).toList();
        client.writePoints(points);
        return values;
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        throw new UnsupportedOperationException("InfluxDB does not support per-point TTL");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        throw new UnsupportedOperationException(
                "InfluxDB writes cannot preserve JNoSQL entity update semantics");
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        throw new UnsupportedOperationException(
                "InfluxDB writes cannot preserve JNoSQL entity update semantics");
    }

    @Override
    public void update(UpdateQuery query) {
        throw new UnsupportedOperationException(
                "InfluxDB does not support JNoSQL update queries");
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");
        throw new UnsupportedOperationException(
                "InfluxDB 3 does not provide a delete operation that preserves JNoSQL entity semantics");
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        InfluxDBQueryConverter.InfluxDBQuery sql = InfluxDBQueryConverter.convert(query);
        return client.queryRows(sql.statement(), sql.parameters())
                .map(row -> InfluxDBEntityConverter.toEntity(query.name(), row));
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        InfluxDBQueryConverter.InfluxDBQuery sql = InfluxDBQueryConverter.count(query);
        try (Stream<java.util.Map<String, Object>> rows = client.queryRows(sql.statement(), sql.parameters())) {
            Object value = rows.findFirst()
                    .map(row -> row.get(InfluxDBQueryConverter.COUNT_COLUMN))
                    .orElse(0L);
            if (value instanceof Number number) {
                return number.longValue();
            }
            throw new IllegalArgumentException("InfluxDB count result is not numeric");
        }
    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        SelectQuery query = SelectQuery.select().from(documentCollection).build();
        return count(query);
    }

    @Override
    public Optional<String> defaultIdFieldName() {
        return Optional.of(ID_FIELD);
    }

    /**
     * Leaves the factory-owned InfluxDB client open.
     *
     * <p>Close the {@link InfluxDBTimeSeriesManagerFactory} to release the client.</p>
     */
    @Override
    public void close() {
    }

}
