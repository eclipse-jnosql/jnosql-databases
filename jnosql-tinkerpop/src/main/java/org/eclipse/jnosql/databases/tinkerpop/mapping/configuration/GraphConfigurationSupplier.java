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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.communication.graph.GraphDatabaseManager;
import org.eclipse.jnosql.databases.tinkerpop.communication.TinkerpopGraphDatabaseManager;

import java.util.function.Supplier;
import java.util.logging.Logger;

@ApplicationScoped
class GraphConfigurationSupplier implements Supplier<GraphDatabaseManager> {

    private static final Logger LOGGER = Logger.getLogger(GraphSupplier.class.getName());

    @Inject
    private GraphSupplier supplier;

    @Override
    @Produces
    @ApplicationScoped
    public GraphDatabaseManager get() {
        LOGGER.fine(() -> "Loading the Graph configuration");
        Graph graph = supplier.get();
        return TinkerpopGraphDatabaseManager.of(graph);
    }
}
