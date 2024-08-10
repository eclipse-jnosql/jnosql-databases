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
package org.eclipse.jnosql.mapping.graph;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.communication.graph.GraphDatabaseManager;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import static org.eclipse.jnosql.mapping.DatabaseType.GRAPH;

@Default
@ApplicationScoped
@Database(GRAPH)
class DefaultGraphTemplate extends AbstractGraphTemplate {

    private final EntityConverter converter;

    private final GraphDatabaseManager manager;

    private final EventPersistManager eventManager;

    private final EntitiesMetadata entities;

    private final Converters converters;
    private final Graph graph;


    @Inject
    DefaultGraphTemplate(EntityConverter converter, Graph graph,
                         EventPersistManager eventManager,
                         EntitiesMetadata entities, Converters converters) {
        this.converter = converter;
        this.graph = graph;
        this.eventManager = eventManager;
        this.entities = entities;
        this.converters = converters;
        this.manager = GraphDatabaseManager.of(graph);
    }

    DefaultGraphTemplate() {
        this(null, null, null, null, null);
    }

    @Override
    protected EntityConverter converter() {
        return converter;
    }

    @Override
    protected GraphDatabaseManager manager() {
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
