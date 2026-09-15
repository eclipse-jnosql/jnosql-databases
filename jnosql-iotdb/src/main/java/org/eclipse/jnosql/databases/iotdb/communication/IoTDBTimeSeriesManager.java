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
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.isession.pool.ITableSessionPool;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.read.common.Field;
import org.apache.tsfile.read.common.RowRecord;
import org.apache.tsfile.write.record.Tablet;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Stores mapped time-series entities through Apache IoTDB's native Table Model client.
 *
 * <p>The entity name becomes a table, {@code _id} becomes the TIME column, and all
 * remaining scalar values become FIELD columns. IoTDB creates and evolves table schemas
 * from native tablet writes when server-side automatic schema creation is enabled.</p>
 */
public class IoTDBTimeSeriesManager implements DatabaseManager {

    private final ITableSessionPool pool;
    private final String database;

    IoTDBTimeSeriesManager(ITableSessionPool pool, String database) {
        this.pool = pool;
        this.database = database;
    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        write(List.of(IoTDBEntityConverter.toRow(entity)));
        return entity;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException(
                "IoTDB supports table/database TTL, not per-entity TTL");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        List<CommunicationEntity> values = StreamSupport.stream(entities.spliterator(), false).toList();
        write(values.stream().map(IoTDBEntityConverter::toRow).toList());
        return values;
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        throw new UnsupportedOperationException(
                "IoTDB supports table/database TTL, not per-entity TTL");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        throw new UnsupportedOperationException(
                "IoTDB Table Model updates apply only to TAG and ATTRIBUTE columns; JNoSQL columns map to FIELD");
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        throw new UnsupportedOperationException(
                "IoTDB Table Model updates apply only to TAG and ATTRIBUTE columns; JNoSQL columns map to FIELD");
    }

    @Override
    public Iterable<CommunicationEntity> update(UpdateQuery query) {
        throw new UnsupportedOperationException(
                "IoTDB Table Model updates apply only to TAG and ATTRIBUTE columns; JNoSQL columns map to FIELD");
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");
        executeNonQuery(IoTDBQueryConverter.delete(query));
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        String sql = IoTDBQueryConverter.convert(query);
        return executeQuery(sql, (dataSet, names, types) -> {
            List<CommunicationEntity> entities = new ArrayList<>();
            while (dataSet.hasNext()) {
                entities.add(IoTDBEntityConverter.toEntity(query.name(), names, types, dataSet.next()));
            }
            return entities.stream();
        });
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        return executeQuery(IoTDBQueryConverter.count(query), (dataSet, names, types) -> {
            if (!dataSet.hasNext()) {
                return 0L;
            }
            RowRecord row = dataSet.next();
            Field field = row.getFields().get(0);
            return field.getLongV();
        });
    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        return count(SelectQuery.select().from(documentCollection).build());
    }

    @Override
    public void close() {
    }

    private void write(List<IoTDBEntityConverter.IoTDBRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        Map<String, List<IoTDBEntityConverter.IoTDBRow>> groups = new LinkedHashMap<>();
        rows.forEach(row -> groups.computeIfAbsent(row.schemaKey(), ignored -> new ArrayList<>()).add(row));
        try (ITableSession session = pool.getSession()) {
            for (List<IoTDBEntityConverter.IoTDBRow> group : groups.values()) {
                Tablet tablet = IoTDBEntityConverter.tablet(group);
                session.insert(tablet);
            }
        } catch (IoTDBConnectionException | StatementExecutionException exception) {
            throw new IllegalStateException(
                    "Could not write to IoTDB database '" + database
                            + "'. Ensure the database exists and AUTO schema creation is enabled.",
                    exception);
        }
    }

    private void executeNonQuery(String sql) {
        try (ITableSession session = pool.getSession()) {
            session.executeNonQueryStatement(sql);
        } catch (IoTDBConnectionException | StatementExecutionException exception) {
            throw new IllegalStateException("Could not execute IoTDB statement", exception);
        }
    }

    private <T> T executeQuery(String sql, ResultMapper<T> mapper) {
        try (ITableSession session = pool.getSession();
             SessionDataSet dataSet = session.executeQueryStatement(sql)) {
            List<TSDataType> types = dataSet.getColumnTypes().stream()
                    .map(TSDataType::valueOf)
                    .toList();
            return mapper.map(dataSet, dataSet.getColumnNames(), types);
        } catch (IoTDBConnectionException | StatementExecutionException exception) {
            throw new IllegalStateException("Could not execute IoTDB query", exception);
        }
    }

    @FunctionalInterface
    private interface ResultMapper<T> {

        T map(SessionDataSet dataSet, List<String> names, List<TSDataType> types)
                throws IoTDBConnectionException, StatementExecutionException;
    }
}
