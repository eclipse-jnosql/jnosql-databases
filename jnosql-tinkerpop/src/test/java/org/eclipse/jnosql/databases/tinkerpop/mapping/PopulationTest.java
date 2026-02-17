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
package org.eclipse.jnosql.databases.tinkerpop.mapping;


import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.databases.tinkerpop.cdi.arangodb.ArangoDBGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.neo4j.Neo4jGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.tinkergraph.TinkerGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Human;
import org.eclipse.jnosql.databases.tinkerpop.mapping.spi.TinkerpopExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.graph.GraphTemplate;
import org.eclipse.jnosql.mapping.graph.spi.GraphExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class, GraphTemplate.class})
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class, GraphExtension.class})
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
abstract class PopulationTest {

    @AddPackages(ArangoDBGraphProducer.class)
    static class ArangoDBTest extends PopulationTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    static class Neo4jTest extends PopulationTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends PopulationTest {
    }

    @Inject
    private Population population;

    @BeforeEach
    void setUp() {
        this.population.deleteAll();
    }

    @Test
    void shouldSave() {
        var human = Human.builder().withAge().withName("Otavio").build();
        Human saved = population.save(human);

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(saved).isNotNull();
            soft.assertThat(saved.getId()).isNotNull();
            soft.assertThat(saved.getName()).isEqualTo(human.getName());
            soft.assertThat(saved.getAge()).isEqualTo(human.getAge());
        });

    }

    @Test
    void shouldFindByQuery() {
        var otavio = population.save(Human.builder().withAge().withName("Otavio").build());
        var poliana = population.save(Human.builder().withAge().withName("Poliana").build());
        var ada = population.save(Human.builder().withAge().withName("Ada").build());

        List<Human> people = population.allHumans();

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(people).isNotNull();
            soft.assertThat(people).hasSize(3);
            soft.assertThat(people).containsExactly(ada, otavio, poliana);
        });
    }

    @Test
    void shouldFindByNameQuery() {
        var otavio = population.save(Human.builder().withAge().withName("Otavio").build());
        var poliana = population.save(Human.builder().withAge().withName("Poliana").build());
        var ada = population.save(Human.builder().withAge().withName("Ada").build());

        List<Human> people = population.findByName("Otavio");

        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(people).isNotNull();
            soft.assertThat(people).hasSize(1);
            soft.assertThat(people).containsExactly(otavio);
        });
    }
}
