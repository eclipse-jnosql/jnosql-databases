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
 * Adapter class that integrates both DatabaseConfiguration and GraphConfiguration.
 * This class is responsible for creating a DatabaseManagerFactory based on the provided settings.
 * It utilizes the GraphConfiguration SPI to create a Graph instance and then wraps it in a DatabaseManagerFactory.
 */
public class DatabaseConfigurationAdapter implements DatabaseConfiguration {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConfigurationAdapter.class.getName());
/**
 * The tinkerpop provider configuration key.
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
