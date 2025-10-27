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
package org.eclipse.jnosql.databases.tinkerpop.communication;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.function.Supplier;
import java.util.logging.Logger;

public enum GraphSupplier implements Supplier<Graph> {
    INSTANCE;

    private final GenericContainer<?> arangodb =
            new GenericContainer<>("arangodb/arangodb:latest")
                    .withExposedPorts(8529)
                    .withEnv("ARANGO_NO_AUTH", "1")
                    .waitingFor(Wait.forHttp("/")
                            .forStatusCode(200));

    {
        arangodb.start();
    }

    private static final Logger LOGGER = Logger.getLogger(GraphSupplier.class.getName());

    @Override
    public Graph get() {
        LOGGER.info("Starting Graph database");
        Configuration configuration = new BaseConfiguration();
        configuration.addProperty("gremlin.graph", "com.arangodb.tinkerpop.gremlin.structure.ArangoDBGraph");
        configuration.addProperty("gremlin.arangodb.conf.graph.enableDataDefinition", true);
        configuration.addProperty("gremlin.arangodb.conf.driver.hosts", arangodb.getHost() + ":" + arangodb.getFirstMappedPort());
        return GraphFactory.open(configuration);
    }
}
