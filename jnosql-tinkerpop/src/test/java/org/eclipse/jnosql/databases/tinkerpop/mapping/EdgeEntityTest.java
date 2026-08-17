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

import jakarta.data.exceptions.EmptyResultException;
import jakarta.inject.Inject;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.databases.tinkerpop.cdi.arangodb.ArangoDBGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.neo4j.Neo4jGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.tinkergraph.TinkerGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Human;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Magazine;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class})
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
abstract class EdgeEntityTest {

    @AddPackages(ArangoDBGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class ArangoDBTest extends EdgeEntityTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class Neo4jTest extends EdgeEntityTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends EdgeEntityTest {
    }

    @Inject
    private TinkerpopTemplate tinkerpopTemplate;


    @Test
    void shouldReturnErrorWhenInboundIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = null;
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnErrorWhenOutboundIsNull() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnErrorWhenLabelIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            tinkerpopTemplate.edge(human, (String) null, magazine);
        });
    }

    @Test
    void shouldReturnNullWhenInboundIdIsNull() {
        assertThatExceptionOfType(EmptyResultException.class).isThrownBy(() -> {
            Human human = Human.builder().withId("-5").withName("Poliana").withAge().build();
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            tinkerpopTemplate.edge(human, "reads", magazine);
        });

    }

    @Test
    void shouldReturnNullWhenOutboundIdIsNull() {
        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnEntityNotFoundWhenOutBoundDidNotFound() {
        assertThatExceptionOfType(EmptyResultException.class).isThrownBy(() -> {
            Human human = Human.builder().withId("-10").withName("Poliana").withAge().build();
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnEntityNotFoundWhenInBoundDidNotFound() {
        assertThatExceptionOfType(EmptyResultException.class).isThrownBy(() -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = Magazine.builder().withId("10").withAge(2007).withName("The Shack").build();
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldCreateAnEdge() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        assertThat(edge.label()).isEqualTo("reads");
        assertThat((Object) edge.outgoing()).isEqualTo(human);
        assertThat((Object) edge.incoming()).isEqualTo(magazine);
        assertThat(edge.isEmpty()).isTrue();
        assertThat(edge.id()).isNotNull();
    }

    @Test
    void shouldGetId() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        assertThat(edge.label()).isEqualTo("reads");
        assertThat((Object) edge.outgoing()).isEqualTo(human);
        assertThat((Object) edge.incoming()).isEqualTo(magazine);
        assertThat(edge.isEmpty()).isTrue();
        assertThat(edge.id()).isNotNull();
        final String id = edge.id(String.class);
        assertThat(id).isNotNull();

        assertThat(edge.id(String.class)).isEqualTo(id);

    }

    @Test
    void shouldCreateAnEdgeWithSupplier() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, () -> "reads", magazine);

        assertThat(edge.label()).isEqualTo("reads");
        assertThat((Object) edge.outgoing()).isEqualTo(human);
        assertThat((Object) edge.incoming()).isEqualTo(magazine);
        assertThat(edge.isEmpty()).isTrue();
        assertThat(edge.id()).isNotNull();
    }

    @Test
    void shouldUseAnEdge() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        EdgeEntity sameEdge = tinkerpopTemplate.edge(human, "reads", magazine);

        assertThat(sameEdge.id()).isEqualTo(edge.id());
        assertThat(sameEdge).isEqualTo(edge);
    }

    @Test
    void shouldUseAnEdge2() {
        Human poliana = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Human nilzete = tinkerpopTemplate.insert(Human.builder().withName("Nilzete").withAge().build());

        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(poliana, "reads", magazine);
        EdgeEntity edge1 = tinkerpopTemplate.edge(nilzete, "reads", magazine);

        EdgeEntity sameEdge = tinkerpopTemplate.edge(poliana, "reads", magazine);
        EdgeEntity sameEdge1 = tinkerpopTemplate.edge(nilzete, "reads", magazine);

        assertThat(sameEdge.id()).isEqualTo(edge.id());
        assertThat(sameEdge).isEqualTo(edge);

        assertThat(sameEdge1.id()).isEqualTo(edge1.id());
        assertThat(sameEdge1).isEqualTo(edge1);

    }

    @Test
    void shouldUseADifferentEdge() {
        Human poliana = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Human nilzete = tinkerpopTemplate.insert(Human.builder().withName("Nilzete").withAge().build());

        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(poliana, "reads", magazine);
        EdgeEntity edge1 = tinkerpopTemplate.edge(nilzete, "reads", magazine);

        EdgeEntity sameEdge = tinkerpopTemplate.edge(poliana, "reads", magazine);
        EdgeEntity sameEdge1 = tinkerpopTemplate.edge(nilzete, "reads", magazine);

        assertThat(edge1.id()).isNotEqualTo(edge.id());
        assertThat(sameEdge1.id()).isNotEqualTo(edge.id());

        assertThat(sameEdge.id()).isNotEqualTo(sameEdge1.id());
    }

    @Test
    void shouldReturnErrorWhenAddKeyIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
            edge.add(null, "Brazil");
        });
    }

    @Test
    void shouldReturnErrorWhenAddValueIsNull() {

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
            edge.add("where", null);
        });
    }

    @Test
    void shouldAddProperty() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");

        assertThat(edge.isEmpty()).isFalse();
        assertThat(edge.size()).isEqualTo(1);
        assertThat(edge.properties()).contains(Element.of("where", "Brazil"));
    }

    @Test
    void shouldAddPropertyWithValue() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.add("where", Value.of("Brazil"));

        assertThat(edge.isEmpty()).isFalse();
        assertThat(edge.size()).isEqualTo(1);
        assertThat(edge.properties()).contains(Element.of("where", "Brazil"));
    }


    @Test
    void shouldReturnErrorWhenRemoveNullKeyProperty() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
            edge.add("where", "Brazil");


            assertThat(edge.isEmpty()).isFalse();
            edge.remove(null);
        });
    }

    @Test
    void shouldRemoveProperty() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");
        assertThat(edge.size()).isEqualTo(1);
        assertThat(edge.isEmpty()).isFalse();
        edge.remove("where");
        assertThat(edge.isEmpty()).isTrue();
        assertThat(edge.size()).isEqualTo(0);
    }

    @Test
    void shouldFindProperty() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");

        Optional<Value> where = edge.get("where");
        assertThat(where.isPresent()).isTrue();
        assertThat(where.get().get()).isEqualTo("Brazil");
        assertThat(edge.get("not").isPresent()).isFalse();

    }

    @Test
    void shouldDeleteAnEdge() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.delete();

        EdgeEntity newEdge = tinkerpopTemplate.edge(human, "reads", magazine);
        assertThat(newEdge.id()).isNotEqualTo(edge.id());

        tinkerpopTemplate.deleteEdge(newEdge.id());
    }

    @Test
    void shouldReturnErrorWhenDeleteAnEdgeWithNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.delete((Iterable<Object>) null));
    }

    @Test
    void shouldDeleteAnEdge2() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());

        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        tinkerpopTemplate.deleteEdge(edge.id());

        EdgeEntity newEdge = tinkerpopTemplate.edge(human, "reads", magazine);
        assertThat(newEdge.id()).isNotEqualTo(edge.id());
    }


    @Test
    void shouldReturnErrorWhenFindEdgeWithNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.edge(null));
    }


    @Test
    void shouldFindAnEdge() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        Optional<EdgeEntity> newEdge = tinkerpopTemplate.edge(edge.id());

        assertThat(newEdge.isPresent()).isTrue();
        assertThat(newEdge.get().id()).isEqualTo(edge.id());

        tinkerpopTemplate.deleteEdge(edge.id());
    }

    @Test
    void shouldNotFindAnEdge() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.edge("-12");

        assertThat(edgeEntity.isPresent()).isFalse();
    }

}
