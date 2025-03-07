/*
 *
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 *
 */
package org.eclipse.jnosql.databases.neo4j.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.graph.GraphDatabaseManager;
import org.eclipse.jnosql.databases.neo4j.communication.Neo4JDatabaseManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.graph.AbstractGraphTemplate;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ApplicationScoped
@Typed(Neo4JTemplate.class)
class DefaultNeo4JTemplate extends AbstractGraphTemplate implements Neo4JTemplate {

    private Instance<Neo4JDatabaseManager> manager;

    private EntityConverter converter;

    private EntitiesMetadata entities;

    private Converters converters;

    private EventPersistManager persistManager;


    @Inject
    DefaultNeo4JTemplate(Instance<Neo4JDatabaseManager> manager,
                           EntityConverter converter,
                           EntitiesMetadata entities,
                           Converters converters,
                           EventPersistManager persistManager) {
        this.manager = manager;
        this.converter = converter;
        this.entities = entities;
        this.converters = converters;
        this.persistManager = persistManager;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Stream<T> cypher(String cypher, Map<String, Object> parameters) {
        Objects.requireNonNull(cypher, "cypher is required");
        Objects.requireNonNull(parameters, "parameters is required");
        return manager.get().executeQuery(cypher, parameters)
                .map(e -> (T) converter.toEntity(e));
    }

    @Override
    public <T> Stream<T> traverse(String startNodeId, Supplier<String> relationship, int depth) {
        Objects.requireNonNull(startNodeId, "startNodeId is required");
        Objects.requireNonNull(relationship, "relationship is required");
        return traverse(startNodeId, relationship.get(), depth);
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> Stream<T> traverse(String startNodeId, String relationship, int depth) {
        Objects.requireNonNull(startNodeId, "startNodeId is required");
        Objects.requireNonNull(relationship, "relationship is required");
        return manager.get().traverse(startNodeId, relationship, depth)
                .map(e -> (T) converter.toEntity(e));
    }

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected GraphDatabaseManager manager() {
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
}
