/*
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
 */
package org.eclipse.jnosql.databases.tinkerpop.mapping.configuration;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.eclipse.jnosql.databases.tinkerpop.communication.GraphConfiguration;
import org.eclipse.jnosql.databases.tinkerpop.communication.TinkerpopGraphDatabaseManager;

import java.util.logging.Logger;

/**
 * The {@link DatabaseConfiguration} wrapper to {@link GraphConfiguration}
 */
public class GraphDatabaseConfiguration implements DatabaseConfiguration {

    private static final Logger LOGGER = Logger.getLogger(GraphDatabaseConfiguration.class.getName());

    @Override
    public DatabaseManagerFactory apply(Settings settings) {
        LOGGER.fine(() -> "Creating graph database manager");
        var configuration = GraphConfiguration.getConfiguration();
        var graph = configuration.apply(settings);
        return new GraphDatabaseManagerFactory(graph);
    }

    static class GraphDatabaseManagerFactory implements DatabaseManagerFactory {

        private final Graph graph;

        GraphDatabaseManagerFactory(Graph graph) {
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
    }
}
