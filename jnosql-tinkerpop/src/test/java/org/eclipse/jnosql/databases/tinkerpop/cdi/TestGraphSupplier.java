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
package org.eclipse.jnosql.databases.tinkerpop.cdi;

import com.arangodb.tinkerpop.gremlin.structure.ArangoDBGraph;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.lang.System.currentTimeMillis;

public enum TestGraphSupplier implements Supplier<Graph> {

    NEO4J {
        private static final Logger LOGGER = Logger.getLogger(TestGraphSupplier.class.getName());

        @Override
        public Graph get() {
            String directory = new File("").getAbsolutePath() + "/target/neo4j-graph/" + currentTimeMillis();
            LOGGER.info("Starting Neo4j at directory: " + directory);
            return Neo4jGraph.open(directory);
        }
    },

    ARANGODB {
        private final GenericContainer<?> arangodb =
                new GenericContainer<>("arangodb/arangodb:latest")
                        .withExposedPorts(8529)
                        .withEnv("ARANGO_NO_AUTH", "1")
                        .waitingFor(Wait.forHttp("/")
                                .forStatusCode(200));

        {
            arangodb.start();
        }

        @Override
        public Graph get() {
            Configuration configuration = new BaseConfiguration();
            configuration.addProperty("gremlin.graph", ArangoDBGraph.class.getName());
            configuration.addProperty("gremlin.arangodb.conf.graph.enableDataDefinition", true);
            configuration.addProperty("gremlin.arangodb.conf.driver.hosts", arangodb.getHost() + ":" + arangodb.getFirstMappedPort());
            return GraphFactory.open(configuration);
        }
    },

    TINKER_GRAPH {
        @Override
        public Graph get() {
            return TinkerGraph.open();
        }
    }

}
