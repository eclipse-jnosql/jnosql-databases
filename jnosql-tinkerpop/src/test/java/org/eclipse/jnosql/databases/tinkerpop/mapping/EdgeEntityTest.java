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
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Magazine;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Human;
import org.eclipse.jnosql.databases.tinkerpop.mapping.spi.GraphExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
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
@AddPackages(value = {Converters.class, EntityConverter.class, GraphTemplate.class})
@AddPackages(GraphProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class, GraphExtension.class})
class EdgeEntityTest {


    @Inject
    private GraphTemplate graphTemplate;


    @Test
    void shouldReturnErrorWhenInboundIsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = null;
            graphTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnErrorWhenOutboundIsNull() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            graphTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnErrorWhenLabelIsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            Human human = Human.builder().withName("Poliana").withAge().build();
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            graphTemplate.edge(human, (String) null, magazine);
        });
    }

    @Test
    void shouldReturnNullWhenInboundIdIsNull() {
        Assertions.assertThrows(EmptyResultException.class, () -> {
            Human human = Human.builder().withId(-5).withName("Poliana").withAge().build();
            Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            graphTemplate.edge(human, "reads", magazine);
        });

    }

    @Test
    void shouldReturnNullWhenOutboundIdIsNull() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = Magazine.builder().withAge(2007).withName("The Shack").build();
            graphTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnEntityNotFoundWhenOutBoundDidNotFound() {
        Assertions.assertThrows( EmptyResultException.class, () -> {
            Human human = Human.builder().withId(-10L).withName("Poliana").withAge().build();
            Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            graphTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldReturnEntityNotFoundWhenInBoundDidNotFound() {
        Assertions.assertThrows( EmptyResultException.class, () -> {
            Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = Magazine.builder().withId(10L).withAge(2007).withName("The Shack").build();
            graphTemplate.edge(human, "reads", magazine);
        });
    }

    @Test
    void shouldCreateAnEdge() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);

        assertEquals("reads", edge.label());
        assertEquals(human, edge.outgoing());
        assertEquals(magazine, edge.incoming());
        assertTrue(edge.isEmpty());
        assertNotNull(edge.id());
    }

    @Test
    void shouldGetId() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);

        assertEquals("reads", edge.label());
        assertEquals(human, edge.outgoing());
        assertEquals(magazine, edge.incoming());
        assertTrue(edge.isEmpty());
        assertNotNull(edge.id());
        final Long id = edge.id(Long.class);
        assertNotNull(id);

        assertEquals(id, edge.id(Integer.class).longValue());

    }

    @Test
    void shouldCreateAnEdgeWithSupplier() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, () -> "reads", magazine);

        assertEquals("reads", edge.label());
        assertEquals(human, edge.outgoing());
        assertEquals(magazine, edge.incoming());
        assertTrue(edge.isEmpty());
        assertNotNull(edge.id());
    }

    @Test
    void shouldUseAnEdge() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);

        EdgeEntity sameEdge = graphTemplate.edge(human, "reads", magazine);

        assertEquals(edge.id(), sameEdge.id());
        assertEquals(edge, sameEdge);
    }

    @Test
    void shouldUseAnEdge2() {
        Human poliana = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Human nilzete = graphTemplate.insert(Human.builder().withName("Nilzete").withAge().build());

        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(poliana, "reads", magazine);
        EdgeEntity edge1 = graphTemplate.edge(nilzete, "reads", magazine);

        EdgeEntity sameEdge = graphTemplate.edge(poliana, "reads", magazine);
        EdgeEntity sameEdge1 = graphTemplate.edge(nilzete, "reads", magazine);

        assertEquals(edge.id(), sameEdge.id());
        assertEquals(edge, sameEdge);

        assertEquals(edge1.id(), sameEdge1.id());
        assertEquals(edge1, sameEdge1);

    }

    @Test
    void shouldUseADifferentEdge() {
        Human poliana = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Human nilzete = graphTemplate.insert(Human.builder().withName("Nilzete").withAge().build());

        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(poliana, "reads", magazine);
        EdgeEntity edge1 = graphTemplate.edge(nilzete, "reads", magazine);

        EdgeEntity sameEdge = graphTemplate.edge(poliana, "reads", magazine);
        EdgeEntity sameEdge1 = graphTemplate.edge(nilzete, "reads", magazine);

        assertNotEquals(edge.id(), edge1.id());
        assertNotEquals(edge.id(), sameEdge1.id());

        assertNotEquals(sameEdge1.id(), sameEdge.id());
    }

    @Test
    void shouldReturnErrorWhenAddKeyIsNull() {
        assertThrows(NullPointerException.class, () -> {
            Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
            edge.add(null, "Brazil");
        });
    }

    @Test
    void shouldReturnErrorWhenAddValueIsNull() {

        assertThrows(NullPointerException.class, () -> {
            Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
            edge.add("where", null);
        });
    }

    @Test
    void shouldAddProperty() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");

        assertFalse(edge.isEmpty());
        assertEquals(1, edge.size());
        assertThat(edge.properties()).contains(Element.of("where", "Brazil"));
    }

    @Test
    void shouldAddPropertyWithValue() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
        edge.add("where", Value.of("Brazil"));

        assertFalse(edge.isEmpty());
        assertEquals(1, edge.size());
        assertThat(edge.properties()).contains(Element.of("where", "Brazil"));
    }


    @Test
    void shouldReturnErrorWhenRemoveNullKeyProperty() {
        assertThrows(NullPointerException.class, () -> {
            Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
            Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
            EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
            edge.add("where", "Brazil");


            assertFalse(edge.isEmpty());
            edge.remove(null);
        });
    }

    @Test
    void shouldRemoveProperty() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");
        assertEquals(1, edge.size());
        assertFalse(edge.isEmpty());
        edge.remove("where");
        assertTrue(edge.isEmpty());
        assertEquals(0, edge.size());
    }

    @Test
    void shouldFindProperty() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
        edge.add("where", "Brazil");

        Optional<Value> where = edge.get("where");
        assertTrue(where.isPresent());
        assertEquals("Brazil", where.get().get());
        assertFalse(edge.get("not").isPresent());

    }

    @Test
    void shouldDeleteAnEdge() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);
        edge.delete();

        EdgeEntity newEdge = graphTemplate.edge(human, "reads", magazine);
        assertNotEquals(edge.id(), newEdge.id());

        graphTemplate.deleteEdge(newEdge.id());
    }

    @Test
    void shouldReturnErrorWhenDeleteAnEdgeWithNull() {
        assertThrows(NullPointerException.class, () -> graphTemplate.delete((Iterable<Object>) null));
    }

    @Test
    void shouldDeleteAnEdge2() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());

        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);

        graphTemplate.deleteEdge(edge.id());

        EdgeEntity newEdge = graphTemplate.edge(human, "reads", magazine);
        assertNotEquals(edge.id(), newEdge.id());
    }


    @Test
    void shouldReturnErrorWhenFindEdgeWithNull() {
        assertThrows(NullPointerException.class, () -> graphTemplate.edge(null));
    }


    @Test
    void shouldFindAnEdge() {
        Human human = graphTemplate.insert(Human.builder().withName("Poliana").withAge().build());
        Magazine magazine = graphTemplate.insert(Magazine.builder().withAge(2007).withName("The Shack").build());
        EdgeEntity edge = graphTemplate.edge(human, "reads", magazine);

        Optional<EdgeEntity> newEdge = graphTemplate.edge(edge.id());

        assertTrue(newEdge.isPresent());
        assertEquals(edge.id(), newEdge.get().id());

        graphTemplate.deleteEdge(edge.id());
    }

    @Test
    void shouldNotFindAnEdge() {
        Optional<EdgeEntity> edgeEntity = graphTemplate.edge(-12L);

        assertFalse(edgeEntity.isPresent());
    }

}
