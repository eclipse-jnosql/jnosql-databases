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
package org.eclipse.jnosql.databases.arangodb.integration;

import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBConfiguration;
import org.eclipse.jnosql.databases.arangodb.communication.ArangoDBConfigurations;
import org.eclipse.jnosql.databases.arangodb.communication.DocumentDatabase;
import org.eclipse.jnosql.databases.arangodb.mapping.ArangoDBTemplate;
import org.eclipse.jnosql.mapping.Database;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.config.MappingConfigurations;
import org.eclipse.jnosql.mapping.graph.Edge;
import org.eclipse.jnosql.mapping.graph.GraphTemplate;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Database.class, EntityConverter.class, ArangoDBTemplate.class})
@AddPackages(Magazine.class)
@AddPackages(GraphTemplate.class)
@AddPackages(Reflections.class)
@AddPackages(Converters.class)
@AddExtensions({ReflectionEntityMetadataExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class GraphTemplateIntegrationTest {

    static {
        System.setProperty(ArangoDBConfigurations.HOST.get() + ".1", DocumentDatabase.INSTANCE.host());
        System.setProperty(MappingConfigurations.DOCUMENT_DATABASE.get(), "library");
        System.setProperty(MappingConfigurations.GRAPH_PROVIDER.get(), ArangoDBConfiguration.class.getName());
        System.setProperty(MappingConfigurations.GRAPH_DATABASE.get(), "arangodb");
    }

    @Inject
    private GraphTemplate template;

    @BeforeEach
    void setUp() {
        template.delete(Magazine.class).execute();
    }

    @Test
    void shouldCreateEdge() {
        Magazine firstEdition = template.insert(new Magazine(null, "Effective Java", 1));
        Magazine secondEdition = template.insert(new Magazine(null, "Effective Java", 2));
        Edge<Magazine, Magazine> edge = Edge.source(firstEdition).label("NEXT").target(secondEdition).property("year", 2025).build();
        Edge<Magazine, Magazine> magazineEdge = template.edge(edge);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(magazineEdge.source()).isEqualTo(firstEdition);
            soft.assertThat(magazineEdge.target()).isEqualTo(secondEdition);
            soft.assertThat(magazineEdge.label()).isEqualTo("NEXT");
            soft.assertThat(magazineEdge.property("year", Integer.class)).contains(2025);
            soft.assertThat(magazineEdge.id()).isPresent();
        });
    }

    @Test
    void shouldCreateEdgeFromNullId() {
        Magazine firstEdition = new Magazine(null, "Effective Java", 1);
        Magazine secondEdition = new Magazine(null, "Effective Java", 2);
        Edge<Magazine, Magazine> edge = Edge.source(firstEdition).label("NEXT").target(secondEdition).property("year", 2025).build();
        Edge<Magazine, Magazine> magazineEdge = template.edge(edge);

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(magazineEdge.source()).isNotNull();
            soft.assertThat(magazineEdge.source().id()).isNotNull();
            soft.assertThat(magazineEdge.target()).isNotNull();
            soft.assertThat(magazineEdge.target().id()).isNotNull();
            soft.assertThat(magazineEdge.label()).isEqualTo("NEXT");
            soft.assertThat(magazineEdge.property("year", Integer.class)).contains(2025);
            soft.assertThat(magazineEdge.id()).isPresent();
        });
    }

}
