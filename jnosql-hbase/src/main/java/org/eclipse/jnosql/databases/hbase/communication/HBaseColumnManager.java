/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.hbase.communication;


import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.ValueWriter;
import org.eclipse.jnosql.communication.ValueWriterDecorator;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jnosql.communication.Condition.EQUALS;
import static org.eclipse.jnosql.communication.Condition.IN;
import static org.eclipse.jnosql.communication.Condition.OR;

/**
 * The Hbase implementation to {@link DatabaseManager}.
 * It does not support TTL methods
 * {@link HBaseColumnManager#insert(org.eclipse.jnosql.communication.semistructured.CommunicationEntity, Duration)}
 */
public class HBaseColumnManager implements DatabaseManager {

    private static final String KEY_REQUIRED_ERROR = "\"To save an entity is necessary to have an row, a Column that has a blank name. Documents.of(\\\"\\\", keyValue);\"";

    private final Connection connection;
    private final Table table;
    private final ValueWriter writerField = ValueWriterDecorator.getInstance();

    private final String database;


    HBaseColumnManager(Connection connection, Table table, String database) {
        this.connection = connection;
        this.table = table;
        this.database = database;
    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        String family = entity.name();
        List<Element> columns = entity.elements();
        if (columns.isEmpty()) {
            return entity;
        }
        Element columnID = entity.find(HBaseUtils.KEY_COLUMN).orElseThrow(() -> new HBaseException(KEY_REQUIRED_ERROR));

        Put put = new Put(Bytes.toBytes(valueToString(columnID.value())));
        columns.stream().filter(Predicate.isEqual(columnID).negate()).forEach(column ->
                put.addColumn(Bytes.toBytes(family),
                        Bytes.toBytes(column.name()),
                        Bytes.toBytes(valueToString(column.value()))));
        try {
            table.put(put);
        } catch (IOException e) {
            throw new HBaseException("An error happened when try to save an entity", e);
        }
        return entity;
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) throws NullPointerException {
        return insert(entity);
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update)
                .collect(Collectors.toList());
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) throws NullPointerException {
        throw new UnsupportedOperationException("There is not support to save async");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> insert(e, ttl))
                .collect(Collectors.toList());
    }


    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");
        CriteriaCondition condition = query.condition()
                .orElseThrow(() -> new IllegalArgumentException("Condition is required"));
        checkedCondition(condition);
        List<String> values = new ArrayList<>();

        convert(condition, values);
        List<Delete> deletes = values
                .stream()
                .map(String::getBytes)
                .map(Delete::new)
                .collect(toList());
        try {
            table.delete(deletes);
        } catch (IOException e) {
            throw new HBaseException("An error when try to delete columns", e);
        }

    }


    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        var condition = query.condition()
                .orElseThrow(() -> new IllegalArgumentException("Condition is required"));
        checkedCondition(condition);
        return Stream.of(findById(condition))
                .map(EntityUnit::new).filter(EntityUnit::isNotEmpty)
                .map(EntityUnit::toEntity);
    }

    @Override
    public long count(String columnFamily) {
        throw new UnsupportedOperationException("Hbase does not have support to count method");
    }


    @Override
    public void close() {
        try {
            connection.close();
            table.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String valueToString(Value value) {
        Object object = value.get();
        if (writerField.test(object.getClass())) {
            return writerField.write(object).toString();
        } else {
            return object.toString();
        }
    }

    private Result[] findById(CriteriaCondition condition) {
        List<String> values = new ArrayList<>();
        convert(condition, values);

        List<Get> gets = values.stream()
                .map(String::getBytes)
                .map(Get::new).collect(toList());
        try {
            return table.get(gets);
        } catch (IOException e) {
            throw new HBaseException("An error when try to find by id", e);
        }
    }


    private void convert(CriteriaCondition columnCondition, List<String> values) {
        Condition condition = columnCondition.condition();

        if (OR.equals(condition)) {
            columnCondition.element().get(new TypeReference<List<CriteriaCondition>>() {
            }).forEach(c -> convert(c, values));
        } else if (IN.equals(condition)) {
            values.addAll(columnCondition.element().get(new TypeReference<List<String>>() {
            }));
        } else if (EQUALS.equals(condition)) {
            values.add(valueToString(columnCondition.element().value()));
        }


    }

    private void checkedCondition(CriteriaCondition columnCondition) {

        Condition condition = columnCondition.condition();
        if (OR.equals(condition)) {
            List<CriteriaCondition> columnConditions = columnCondition.element().get(new TypeReference<>() {
            });
            for (CriteriaCondition cc : columnConditions) {
                checkedCondition(cc);
            }
            return;

        }
        if (!EQUALS.equals(condition) && !IN.equals(condition)) {
            throw new UnsupportedOperationException("Hbase does not support the following condition: %s just AND, EQUAL and IN ");
        }
    }


    @Override
    public String toString() {
        return "HBaseColumnManager{" +
                "table=" + table +
                ", database='" + database + '\'' +
                '}';
    }
}
