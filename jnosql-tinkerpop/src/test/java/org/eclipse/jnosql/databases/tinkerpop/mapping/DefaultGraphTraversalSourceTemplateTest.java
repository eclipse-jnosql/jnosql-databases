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

import jakarta.inject.Inject;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.eclipse.jnosql.databases.tinkerpop.cdi.arangodb.ArangoDBGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.neo4j.Neo4jGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.tinkergraph.TinkerGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.spi.TinkerpopExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, Transactional.class})
@AddPackages({MagazineRepository.class, Reflections.class})
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
abstract class DefaultGraphTraversalSourceTemplateTest extends AbstractTinkerpopTemplateTest {

    @AddPackages(ArangoDBGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class ArangoDBTest extends DefaultGraphTraversalSourceTemplateTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class Neo4jTest extends DefaultGraphTraversalSourceTemplateTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends DefaultGraphTraversalSourceTemplateTest {
    }

    @Inject
    private TinkerpopTemplate graphTemplate;

    @Inject
    private Graph graph;

    @Override
    protected Graph getGraph() {
        return graph;
    }

    @Override
    protected TinkerpopTemplate getGraphTemplate() {
        return graphTemplate;
    }
}
