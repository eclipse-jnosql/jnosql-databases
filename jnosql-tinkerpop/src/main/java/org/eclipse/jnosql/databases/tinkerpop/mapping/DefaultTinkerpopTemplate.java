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
package org.eclipse.jnosql.databases.tinkerpop.mapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.communication.TinkerpopGraphDatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EntityConverterFactory;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import static org.eclipse.jnosql.mapping.DatabaseType.GRAPH;

@Default
@ApplicationScoped
@Database(GRAPH)
@Typed(TinkerpopTemplate.class)
class DefaultTinkerpopTemplate extends AbstractTinkerpopTemplate {

    private EntityConverter converter;
    private TinkerpopGraphDatabaseManager manager;
    private EventPersistManager eventManager;
    private EntitiesMetadata entities;
    private Converters converters;
    private Graph graph;

    @Inject
    DefaultTinkerpopTemplate(EntityConverterFactory converter, Graph graph,
                             EventPersistManager eventManager,
                             EntitiesMetadata entities, Converters converters) {
        this.graph = graph;
        this.eventManager = eventManager;
        this.entities = entities;
        this.converters = converters;
        this.manager = TinkerpopGraphDatabaseManager.of(graph);
        this.converter = converter.create(manager);
    }

    /**
     * Constructor for CDI
     */
    @Deprecated
    DefaultTinkerpopTemplate() {}

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected TinkerpopGraphDatabaseManager manager() {
        return manager;
    }

    @Override
    protected GraphTraversalSource traversal() {
        return graph.traversal();
    }

    @Override
    protected Graph graph() {
        return graph;
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
}
