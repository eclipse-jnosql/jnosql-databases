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
import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.databases.tinkerpop.communication.TinkerpopGraphDatabaseManager;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.EntityConverterFactory;
import org.eclipse.jnosql.mapping.semistructured.EventPersistManager;

import java.util.Objects;
import java.util.function.Function;

/**
 * An {@code ApplicationScoped} producer class responsible for creating instances of {@link TinkerpopTemplate}.
 * It implements the {@link Function} interface with {@link DatabaseManager} as input and {@link TinkerpopTemplate} as output.
 */
@ApplicationScoped
public class GraphTemplateProducer implements Function<Graph, TinkerpopTemplate> {

    @Inject
    private EntityConverterFactory converter;

    @Inject
    private EventPersistManager eventManager;

    @Inject
    private EntitiesMetadata entities;

    @Inject
    private Converters converters;


    @Override
    public TinkerpopTemplate apply(Graph graph) {
        Objects.requireNonNull(graph, "graph is required");
        return new ProducerTinkerpopTemplate(converter, graph,
                eventManager, entities, converters);
    }

    @Vetoed
    static class ProducerTinkerpopTemplate extends AbstractTinkerpopTemplate {

        private final EntityConverter converter;


        private final EventPersistManager eventManager;

        private final EntitiesMetadata entities;

        private final Converters converters;

        private final Graph graph;

        private final TinkerpopGraphDatabaseManager manager;

        public ProducerTinkerpopTemplate(EntityConverterFactory converter, Graph graph,
                                         EventPersistManager eventManager,
                                         EntitiesMetadata entities, Converters converters) {
            this.graph = graph;
            this.manager = TinkerpopGraphDatabaseManager.of(graph);
            this.converter = converter.create(manager);
            this.eventManager = eventManager;
            this.entities = entities;
            this.converters = converters;
        }

        ProducerTinkerpopTemplate() {
            this(null, null, null, null, null);
        }

        @Override
        protected EntityConverter converter() {
            return converter;
        }

        @Override
        protected TinkerpopGraphDatabaseManager manager() {
            return manager;
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
        protected GraphTraversalSource traversal() {
            return graph.traversal();
        }

        @Override
        protected Graph graph() {
            return graph;
        }
    }
}
