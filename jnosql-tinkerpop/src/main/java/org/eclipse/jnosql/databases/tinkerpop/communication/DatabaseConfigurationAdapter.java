/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.eclipse.jnosql.mapping.reflection.Reflections;

import java.util.logging.Logger;

/**
 * The {@link DatabaseConfiguration} for TinkerPop graph databases. It creates the {@link Graph} through the
 * {@link GraphConfiguration} SPI and wraps it in a {@link DatabaseManagerFactory}.
 *
 * <p>The {@link GraphConfiguration} is the class set in {@link #TINKERPOP_PROVIDER}; if the setting is absent,
 * the first one registered in {@link java.util.ServiceLoader} is used. The factory ignores the name passed to
 * {@code apply(String)}, because a TinkerPop {@link Graph} has no database concept, and closing it has no
 * effect.</p>
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put(DatabaseConfigurationAdapter.TINKERPOP_PROVIDER, MyGraphConfiguration.class)
 *         .build();
 * DatabaseManagerFactory factory = new DatabaseConfigurationAdapter().apply(settings);
 * DatabaseManager manager = factory.apply("any-name");
 * }
 */
public class DatabaseConfigurationAdapter implements DatabaseConfiguration {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfigurationAdapter.class.getName());

/**
 * The setting key, {@code jnosql.graph.tinkerpop.provider}, that holds the {@link GraphConfiguration}
 * implementation class to use.
 */
    public static final String TINKERPOP_PROVIDER = "jnosql.graph.tinkerpop.provider";

    @SuppressWarnings("unchecked")
    @Override
    public DatabaseManagerFactory apply(Settings settings) {
        LOGGER.fine(() -> "Creating graph database manager based on settings and GraphConfiguration SPI");
        GraphConfiguration configuration = settings.get(TINKERPOP_PROVIDER, Class.class)
                .filter(GraphConfiguration.class::isAssignableFrom)
                .map(c -> (GraphConfiguration) Reflections.newInstance(c))
                .orElseGet(GraphConfiguration::getConfiguration);
        var graph = configuration.apply(settings);
        return GraphDatabaseManagerFactory.of(graph);
    }

    static class GraphDatabaseManagerFactory implements DatabaseManagerFactory {

        private final Graph graph;

        private GraphDatabaseManagerFactory(Graph graph) {
            this.graph = graph;
        }

        @Override
        public void close() {

        }

        @Override
        public DatabaseManager apply(String database) {
            LOGGER.fine(() -> "Creating graph database manager where we will ignore the database name: " + database);
            return TinkerpopGraphDatabaseManager.of(graph);
        }

        public static GraphDatabaseManagerFactory of(Graph graph) {
            return new GraphDatabaseManagerFactory(graph);
        }
    }
}
