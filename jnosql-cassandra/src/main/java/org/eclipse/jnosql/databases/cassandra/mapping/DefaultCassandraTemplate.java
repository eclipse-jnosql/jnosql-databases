/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.cassandra.mapping;


import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.cassandra.communication.CassandraColumnManager;
import org.eclipse.jnosql.databases.cassandra.communication.CassandraPreparedStatement;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Typed(CassandraTemplate.class)
@ApplicationScoped
class DefaultCassandraTemplate extends AbstractSemiStructuredTemplate implements CassandraTemplate {

    private final Instance<CassandraColumnManager> manager;

    private final CassandraColumnEntityConverter converter;

    private final EventPersistManager persistManager;

    private final EntitiesMetadata entities;

    private final Converters converters;

    @Inject
    DefaultCassandraTemplate(Instance<CassandraColumnManager> manager,
                             CassandraColumnEntityConverter converter,
                             EventPersistManager persistManager,
                             EntitiesMetadata entities,
                             Converters converters) {
        this.manager = manager;
        this.converter = converter;
        this.persistManager = persistManager;
        this.entities = entities;
        this.converters = converters;
    }

    DefaultCassandraTemplate() {
        this(null, null, null, null, null);
    }

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected DatabaseManager manager() {
        return manager.get();
    }

    @Override
    protected EventPersistManager eventManager() {
        return persistManager;
    }

    @Override
    protected EntitiesMetadata entities() {
        return entities;
    }

    @Override
    protected Converters converters() {
        return converters;
    }

    @Override
    public <T> T save(T entity, ConsistencyLevel level) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(level, "level is required");
        UnaryOperator<CommunicationEntity> save = e -> manager.get().save(e, level);
        return persist(entity, save);
    }

    @Override
    public <T> Iterable<T> save(Iterable<T> entities, Duration ttl, ConsistencyLevel level) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(ttl, "ttl is required");
        Objects.requireNonNull(level, "level is required");

        return StreamSupport.stream(entities.spliterator(), false)
                .map(converter::toCommunication)
                .map(e -> manager.get().save(e, ttl, level))
                .map(converter::toEntity)
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }

    @Override
    public <T> Iterable<T> save(Iterable<T> entities, ConsistencyLevel level) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(level, "level is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(converter::toCommunication)
                .map(e -> manager.get().save(e, level))
                .map(converter::toEntity)
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }

    @Override
    public <T> T save(T entity, Duration ttl, ConsistencyLevel level) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(ttl, "ttl is required");
        Objects.requireNonNull(level, "level is required");
        UnaryOperator<CommunicationEntity> save = e -> manager.get().save(e, ttl, level);
        return persist(entity, save);
    }

    @Override
    public void delete(DeleteQuery query, ConsistencyLevel level) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(level, "level is required");
        manager.get().delete(query, level);
    }

    @Override
    public long count(SelectQuery query, ConsistencyLevel level) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(level, "level is required");
        return manager.get().count(query, level);
    }

    @Override
    public <T> Stream<T> find(SelectQuery query, ConsistencyLevel level) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(level, "level is required");

        return manager.get().select(query, level)
                .map(converter::toEntity);
    }

    @Override
    public <T> Stream<T> cql(String query) {
        return manager.get().cql(query)
                .map(converter::toEntity);
    }

    @Override
    public <T> Stream<T> cql(String query, Map<String, Object> values) {
        return manager.get().cql(query, values)
                .map(converter::toEntity);
    }

    @Override
    public <T> Stream<T> cql(String query, Object... params) {
        Objects.requireNonNull(query, "query is required");
        CassandraPreparedStatement cassandraPrepareStatement = manager.get().nativeQueryPrepare(query);
        Stream<CommunicationEntity> entities = cassandraPrepareStatement.bind(params).executeQuery();
        return entities.map(converter::toEntity).map(e -> (T) e);
    }

    @Override
    public <T> Stream<T> execute(SimpleStatement statement) {
        return manager.get().execute(statement)
                .map(converter::toEntity);
    }

}
