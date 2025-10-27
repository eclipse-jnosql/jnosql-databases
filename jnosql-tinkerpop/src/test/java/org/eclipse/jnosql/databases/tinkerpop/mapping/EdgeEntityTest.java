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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class})
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
abstract class EdgeEntityTest {

    @AddPackages(ArangoDBGraphProducer.class)
    static class ArangoDBTest extends EdgeEntityTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    static class Neo4jTest extends EdgeEntityTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends EdgeEntityTest {
    }

    @Inject
    private TinkerpopTemplate tinkerpopTemplate;


    @Test
    void shouldReturnErrorWhenInboundIsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = null;
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnErrorWhenOutboundIsNull() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnErrorWhenLabelIsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            tinkerpopTemplate.edge(human, (String) null, magazine);
        });
    }

    @Test
    void shouldReturnNullWhenInboundIdIsNull() {
        Assertions.assertThrows(EmptyResultException.class, () -> {
            Human human = Human.builder().withId("-5").withName("Poliana").withAge().build();
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            tinkerpopTemplate.edge(human, "reads", magazine);
        });

    }

    @Test
    void shouldReturnNullWhenOutboundIdIsNull() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnEntityNotFoundWhenOutBoundDidNotFound() {
        Assertions.assertThrows( EmptyResultException.class, () -> {
            Human human = Human.builder().withId("-10").withName("Poliana").withAge().build();
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            tinkerpopTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnEntityNotFoundWhenInBoundDidNotFound() {
        Assertions.assertThrows( EmptyResultException.class, () -> {
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

        assertEquals("reads", edge.label());
        assertEquals(human, edge.outgoing());
        assertEquals(magazine, edge.incoming());
        assertTrue(edge.isEmpty());
        assertNotNull(edge.id());
    }

    @Test
    void shouldGetId() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        assertEquals("reads", edge.label());
        assertEquals(human, edge.outgoing());
        assertEquals(magazine, edge.incoming());
        assertTrue(edge.isEmpty());
        assertNotNull(edge.id());
        final String id = edge.id(String.class);
        assertNotNull(id);

        assertEquals(id, edge.id(String.class));

    }

    @Test
    void shouldCreateAnEdgeWithSupplier() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, () -> "reads", magazine);

        assertEquals("reads", edge.label());
        assertEquals(human, edge.outgoing());
        assertEquals(magazine, edge.incoming());
        assertTrue(edge.isEmpty());
        assertNotNull(edge.id());
    }

    @Test
    void shouldUseAnEdge() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        EdgeEntity sameEdge = tinkerpopTemplate.edge(human, "reads", magazine);

        assertEquals(edge.id(), sameEdge.id());
        assertEquals(edge, sameEdge);
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

        assertEquals(edge.id(), sameEdge.id());
        assertEquals(edge, sameEdge);

        assertEquals(edge1.id(), sameEdge1.id());
        assertEquals(edge1, sameEdge1);

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

        assertNotEquals(edge.id(), edge1.id());
        assertNotEquals(edge.id(), sameEdge1.id());

        assertNotEquals(sameEdge1.id(), sameEdge.id());
    }

    @Test
    void shouldReturnErrorWhenAddKeyIsNull() {
        assertThrows(NullPointerException.class, () -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
            edge.add(null, "Brazil");
        });
    }

    @Test
    void shouldReturnErrorWhenAddValueIsNull() {

        assertThrows(NullPointerException.class, () -> {
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

        assertFalse(edge.isEmpty());
        assertEquals(1, edge.size());
        assertThat(edge.properties()).contains(Element.of("where", "Brazil"));
    }

    @Test
    void shouldAddPropertyWithValue() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.add("where", Value.of("Brazil"));

        assertFalse(edge.isEmpty());
        assertEquals(1, edge.size());
        assertThat(edge.properties()).contains(Element.of("where", "Brazil"));
    }


    @Test
    void shouldReturnErrorWhenRemoveNullKeyProperty() {
        assertThrows(NullPointerException.class, () -> {
            Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
            edge.add("where", "Brazil");


            assertFalse(edge.isEmpty());
            edge.remove(null);
        });
    }

    @Test
    void shouldRemoveProperty() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");
        assertEquals(1, edge.size());
        assertFalse(edge.isEmpty());
        edge.remove("where");
        assertTrue(edge.isEmpty());
        assertEquals(0, edge.size());
    }

    @Test
    void shouldFindProperty() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");

        Optional<Value> where = edge.get("where");
        assertTrue(where.isPresent());
        assertEquals("Brazil", where.get().get());
        assertFalse(edge.get("not").isPresent());

    }

    @Test
    void shouldDeleteAnEdge() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);
        edge.delete();

        EdgeEntity newEdge = tinkerpopTemplate.edge(human, "reads", magazine);
        assertNotEquals(edge.id(), newEdge.id());

        tinkerpopTemplate.deleteEdge(newEdge.id());
    }

    @Test
    void shouldReturnErrorWhenDeleteAnEdgeWithNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.delete((Iterable<Object>) null));
    }

    @Test
    void shouldDeleteAnEdge2() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());

        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        tinkerpopTemplate.deleteEdge(edge.id());

        EdgeEntity newEdge = tinkerpopTemplate.edge(human, "reads", magazine);
        assertNotEquals(edge.id(), newEdge.id());
    }


    @Test
    void shouldReturnErrorWhenFindEdgeWithNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.edge(null));
    }


    @Test
    void shouldFindAnEdge() {
        Human human = tinkerpopTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = tinkerpopTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = tinkerpopTemplate.edge(human, "reads", magazine);

        Optional<EdgeEntity> newEdge = tinkerpopTemplate.edge(edge.id());

        assertTrue(newEdge.isPresent());
        assertEquals(edge.id(), newEdge.get().id());

        tinkerpopTemplate.deleteEdge(edge.id());
    }

    @Test
    void shouldNotFindAnEdge() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.edge("-12");

        assertFalse(edgeEntity.isPresent());
    }

}
