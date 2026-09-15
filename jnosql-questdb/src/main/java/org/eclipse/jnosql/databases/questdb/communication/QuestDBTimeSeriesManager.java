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

import io.questdb.client.Completion;
import io.questdb.client.Query;
import io.questdb.client.QuestDB;
import io.questdb.client.Sender;
import io.questdb.client.cutlass.qwp.client.QwpBindValues;
import io.questdb.client.cutlass.qwp.client.QwpColumnBatch;
import io.questdb.client.cutlass.qwp.client.QwpColumnBatchHandler;
import io.questdb.client.cutlass.qwp.client.QwpServerInfo;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueUtil;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.eclipse.jnosql.databases.questdb.communication.QuestDBEntityConverter.ID_FIELD;

/**
 * Stores mapped time-series entities through QuestDB's native QWP client.
 *
 * <p>The entity name becomes a table, {@code _id} becomes QuestDB's designated
 * timestamp, and scalar columns become QuestDB columns. Table and column
 * creation are delegated to QuestDB's server-side automatic schema policy.</p>
 */
public class QuestDBTimeSeriesManager implements DatabaseManager {

    private static final long TIMEOUT_MILLIS = 60_000L;

    private final QuestDB questDB;

    private final String database;

    QuestDBTimeSeriesManager(QuestDB questDB, String database) {
        this.questDB = questDB;
        this.database = database;
    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        write(List.of(QuestDBEntityConverter.toRow(entity)));
        return entity;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException(
                "QuestDB supports partition retention, not per-entity TTL");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        List<CommunicationEntity> values = StreamSupport.stream(entities.spliterator(), false).toList();
        write(values.stream().map(QuestDBEntityConverter::toRow).toList());
        return values;
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        throw new UnsupportedOperationException(
                "QuestDB supports partition retention, not per-entity TTL");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        executeUpdate(QuestDBQueryConverter.update(QuestDBEntityConverter.toUpdateRow(entity)));
        return entity;
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        List<CommunicationEntity> values = StreamSupport.stream(entities.spliterator(), false).toList();
        values.forEach(this::update);
        return values;
    }

    @Override
    public void update(UpdateQuery query) {
        Objects.requireNonNull(query, "query is required");
        executeUpdate(QuestDBQueryConverter.update(query));
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");
        throw new UnsupportedOperationException(
                "QuestDB does not support row-level DELETE statements");
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        EntityResultHandler handler = new EntityResultHandler(query.name());
        execute(QuestDBQueryConverter.convert(query), handler);
        return handler.results().stream();
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        CountResultHandler handler = new CountResultHandler();
        execute(QuestDBQueryConverter.count(query), handler);
        return handler.count();
    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        return count(SelectQuery.select().from(documentCollection).build());
    }

    @Override
    public Optional<String> defaultIdFieldName() {
        return Optional.of(ID_FIELD);
    }

    @Override
    public void close() {
    }

    private void write(List<QuestDBEntityConverter.QuestDBRow> rows) {
        if (rows.isEmpty()) {
            return;
        }
        try (Sender sender = questDB.borrowSender()) {
            rows.forEach(row -> QuestDBEntityConverter.append(sender, row));
            sender.flush();
            if (!sender.drain(TIMEOUT_MILLIS)) {
                throw new IllegalStateException("Timed out waiting for QuestDB to acknowledge the write");
            }
        }
    }

    private void executeUpdate(QuestDBQueryConverter.QuestDBQuery query) {
        execute(query, new UpdateResultHandler());
    }

    private void execute(QuestDBQueryConverter.QuestDBQuery statement, QwpColumnBatchHandler handler) {
        try (Query query = questDB.borrowQuery()) {
            Completion completion = query.sql(statement.statement())
                    .binds(values -> bind(values, statement.parameters()))
                    .handler(handler)
                    .submit();
            if (!completion.await(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                completion.cancel();
                throw new IllegalStateException("Timed out waiting for QuestDB query completion");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for QuestDB query completion", exception);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not execute QuestDB query", exception);
        }
    }

    static void bind(QwpBindValues binds, List<Object> parameters) {
        for (int index = 0; index < parameters.size(); index++) {
            bind(binds, index, parameters.get(index));
        }
    }

    private static void bind(QwpBindValues binds, int index, Object value) {
        Object converted = value instanceof Value jnosqlValue ? ValueUtil.convert(jnosqlValue) : value;
        switch (converted) {
            case Boolean booleanValue -> binds.setBoolean(index, booleanValue);
            case Byte byteValue -> binds.setByte(index, byteValue);
            case Short shortValue -> binds.setShort(index, shortValue);
            case Character character -> binds.setChar(index, character);
            case Integer integer -> binds.setInt(index, integer);
            case Long longValue -> binds.setLong(index, longValue);
case BigInteger bigInteger when bigInteger.bitLength() < Long.SIZE ->
        binds.setLong(index, bigInteger.longValue());
            case Float floatValue -> binds.setFloat(index, floatValue);
            case Double doubleValue -> binds.setDouble(index, doubleValue);
            case CharSequence text -> binds.setVarchar(index, text);
            case Instant instant -> binds.setTimestampMicros(index, QuestDBEntityConverter.toEpochMicros(instant));
            case LocalDateTime dateTime -> binds.setTimestampMicros(index,
                    QuestDBEntityConverter.toEpochMicros(dateTime.toInstant(ZoneOffset.UTC)));
            case OffsetDateTime dateTime -> binds.setTimestampMicros(index,
                    QuestDBEntityConverter.toEpochMicros(dateTime.toInstant()));
            case ZonedDateTime dateTime -> binds.setTimestampMicros(index,
                    QuestDBEntityConverter.toEpochMicros(dateTime.toInstant()));
            case UUID uuid -> binds.setUuid(index, uuid);
            case null -> throw new UnsupportedOperationException(
                    "QuestDB native binds require a concrete type; null values are not supported");
            default -> throw new IllegalArgumentException(
                    "Unsupported QuestDB query parameter type: " + converted.getClass().getName());
        }
    }

    private static class ResultHandler implements QwpColumnBatchHandler {

        @Override
        public void onBatch(QwpColumnBatch batch) {
        }

        @Override
        public void onEnd(long totalRows) {
        }

        @Override
        public void onError(byte status, String message) {
        }

        @Override
        public void onFailoverReset(QwpServerInfo newNode) {
        }
    }

    private static final class EntityResultHandler extends ResultHandler {

        private final String table;

        private final List<CommunicationEntity> results = new ArrayList<>();

        private EntityResultHandler(String table) {
            this.table = table;
        }

        @Override
        public void onBatch(QwpColumnBatch batch) {
            for (int row = 0; row < batch.getRowCount(); row++) {
                results.add(QuestDBEntityConverter.toEntity(table, batch, row));
            }
        }

        @Override
        public void onFailoverReset(QwpServerInfo newNode) {
            results.clear();
        }

        private List<CommunicationEntity> results() {
            return List.copyOf(results);
        }
    }

    private static final class CountResultHandler extends ResultHandler {

        private long count;

        @Override
        public void onBatch(QwpColumnBatch batch) {
            if (batch.getRowCount() > 0) {
                count = batch.getLongValue(0, 0);
            }
        }

        @Override
        public void onFailoverReset(QwpServerInfo newNode) {
            count = 0L;
        }

        private long count() {
            return count;
        }
    }

    private static final class UpdateResultHandler extends ResultHandler {
    }
}
