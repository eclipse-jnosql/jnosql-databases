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
package org.eclipse.jnosql.databases.arangodb.mapping;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBDocumentManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.graph.Edge;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.AbstractSemiStructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * The Default implementation of {@link ArangoDBTemplate}
 */
@Typed(ArangoDBTemplate.class)
@ApplicationScoped
class DefaultArangoDBTemplate extends AbstractSemiStructuredTemplate implements ArangoDBTemplate {

    private final Instance<ArangoDBDocumentManager> manager;

    private final  EntityConverter converter;

    private final  EventPersistManager eventManager;

    private final  EntitiesMetadata entities;

    private final  Converters converters;

    @Inject
    DefaultArangoDBTemplate(Instance<ArangoDBDocumentManager> manager,
                            EntityConverter converter,
                            EventPersistManager eventManager,
                            EntitiesMetadata entities,
                            Converters converters) {
        this.manager = manager;
        this.converter = converter;
        this.eventManager = eventManager;
        this.entities = entities;
        this.converters = converters;
    }

    DefaultArangoDBTemplate() {
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
        return eventManager;
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
    public <T> Stream<T> aql(String query, Map<String, Object> params) {
        requireNonNull(query, "query is required");
        requireNonNull(params, "values is required");
        return manager.get().aql(query, params).map(converter::toEntity).map(d -> (T) d);
    }

    @Override
    public <T> Stream<T> aql(String query, Map<String, Object> params, Class<T> type) {
        return manager.get().aql(query, params, type);
    }

    @Override
    public <T> Stream<T> aql(String query, Class<T> type) {
        return manager.get().aql(query, type);
    }

    @Override
    public <T, E> Edge<T, E> edge(T source, String label, E target, Map<String, Object> properties) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public <T, E> Edge<T, E> edge(T source, Supplier<String> label, E target, Map<String, Object> properties) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public <T, E> Edge<T, E> edge(Edge<T, E> edge) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public <T, E> void delete(Edge<T, E> edge) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public <K> void deleteEdge(K id) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public <K, T, E> Optional<Edge<T, E>> findEdgeById(K id) {
        throw new UnsupportedOperationException("TODO");
    }
}
