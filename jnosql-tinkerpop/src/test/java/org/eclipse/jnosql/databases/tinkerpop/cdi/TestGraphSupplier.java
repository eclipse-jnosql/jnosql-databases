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
import com.arangodb.tinkerpop.gremlin.structure.ArangoDBGraphConfig;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerTransactionGraph;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.util.List;
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
        @Override
        public Graph get() {
            GenericContainer<?> container = ArangoDeployment.INSTANCE.getContainer();
            Configuration configuration = new BaseConfiguration();
            configuration.addProperty("gremlin.graph", ArangoDBGraph.class.getName());
            configuration.addProperty("gremlin.arangodb.conf.graph.enableDataDefinition", true);
            configuration.addProperty("gremlin.arangodb.conf.graph.type", ArangoDBGraphConfig.GraphType.COMPLEX.name());
            configuration.addProperty("gremlin.arangodb.conf.graph.edgeDefinitions", List.of(
                    "reads:[Human]->[Magazine]",
                    "knows:[Person]->[Person]",
                    "eats:[Creature]->[Creature]",
                    "loves:[Human]->[Human]",
                    "likes:[Human]->[Creature]",
                    "friend:[Person]->[Person]"
            ));
            configuration.addProperty("gremlin.arangodb.conf.driver.hosts", container.getHost() + ":" + container.getFirstMappedPort());
            return GraphFactory.open(configuration);
        }
    },

    TINKER_GRAPH {
        @Override
        public Graph get() {
            Configuration configuration = new BaseConfiguration();
            configuration.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_VERTEX_ID_MANAGER, TinkerGraph.DefaultIdManager.STRING.name());
            configuration.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_EDGE_ID_MANAGER, TinkerGraph.DefaultIdManager.STRING.name());
            configuration.setProperty(TinkerGraph.GREMLIN_TINKERGRAPH_VERTEX_PROPERTY_ID_MANAGER, TinkerGraph.DefaultIdManager.STRING.name());
            return TinkerTransactionGraph.open(configuration);
        }
    }

}
