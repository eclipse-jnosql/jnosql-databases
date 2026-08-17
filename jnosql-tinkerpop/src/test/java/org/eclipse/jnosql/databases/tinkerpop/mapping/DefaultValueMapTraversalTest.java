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

import jakarta.data.exceptions.NonUniqueResultException;
import org.eclipse.jnosql.databases.tinkerpop.cdi.arangodb.ArangoDBGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.neo4j.Neo4jGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.tinkergraph.TinkerGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Human;
import org.eclipse.jnosql.databases.tinkerpop.mapping.spi.TinkerpopExtension;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class})
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
abstract class DefaultValueMapTraversalTest extends AbstractTraversalTest {

    @AddPackages(ArangoDBGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class ArangoDBTest extends DefaultValueMapTraversalTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class Neo4jTest extends DefaultValueMapTraversalTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends DefaultValueMapTraversalTest {
    }

    @Test
    void shouldCount() {
        long count = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class).valueMap("name").count();
        assertThat(count).isEqualTo(3L);
    }


    @Test
    void shouldReturnMapValues() {
        List<String> names = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class).valueMap("name")
                .stream()
                .map(m -> m.getOrDefault("name", "").toString()).collect(Collectors.toList());


        assertThat(names).contains("Poliana", "Otavio", "Paulo");
    }

    @Test
    void shouldReturnStream() {
        Stream<Map<String, Object>> stream = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class).valueMap("name")
                .stream();
        assertThat(stream).isNotNull();
        assertThat(stream.count()).isEqualTo(3L);
    }


    @Test
    void shouldReturnResultAsList() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class).valueMap("name")
                .resultList();
        assertThat(maps.size()).isEqualTo(3);
    }

    @Test
    void shouldReturnErrorWhenThereAreMoreThanOneInGetSingleResult() {
        assertThatExceptionOfType(NonUniqueResultException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class).valueMap("name")
                .singleResult());
    }

    @Test
    void shouldReturnOptionalEmptyWhenThereIsNotResultInSingleResult() {
        Optional<Map<String, Object>> entity =   tinkerpopTemplate.traversalVertex()
                .hasLabel("not_found").valueMap("name").singleResult();
        assertThat(entity.isPresent()).isFalse();
    }

    @Test
    void shouldReturnSingleResult() {
        String name = "Poliana";
        Optional<Map<String, Object>> poliana = tinkerpopTemplate.traversalVertex().hasLabel("Human").
                has("name", name).valueMap("name").singleResult();
        assertThat(poliana.map(m ->  m.get("name")).orElse("")).isEqualTo(name);
    }
}
