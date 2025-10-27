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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.tinkerpop.cdi.arangodb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.cdi.TestGraphSupplier;

import java.util.function.Supplier;
import java.util.logging.Logger;

@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
public class ArangoDBGraphProducer implements Supplier<Graph> {

    private static final Logger LOGGER = Logger.getLogger(ArangoDBGraphProducer.class.getName());

    private Graph graph;

    @PostConstruct
    public void init() {
        graph = TestGraphSupplier.ARANGODB.get();
        LOGGER.info("Graph database created");
    }

    @Produces
    @ApplicationScoped
    @Override
    public Graph get() {
        return graph;
    }

    public void dispose(@Disposes Graph graph) throws Exception {
        LOGGER.info("Graph database closing");
        graph.close();
        LOGGER.info("Graph Database closed");
    }

}
