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
import jakarta.data.exceptions.NonUniqueResultException;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Creature;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Human;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Magazine;
import org.eclipse.jnosql.mapping.PreparedStatement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractTinkerpopTemplateTest {

    protected abstract Graph getGraph();

    protected abstract TinkerpopTemplate getGraphTemplate();

    @AfterEach
    void after() {
        getGraph().traversal().V().toList().forEach(Vertex::remove);
        getGraph().traversal().E().toList().forEach(Edge::remove);
    }

    @Test
    void shouldReturnErrorWhenEntityIsNull() {
        assertThrows(NullPointerException.class, () -> getGraphTemplate().insert(null));
    }

    @Test
    void shouldInsertAnEntity() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Human updated = getGraphTemplate().insert(human);

        getGraphTemplate().delete(updated.getId());
    }

    @Test
    void shouldReturnErrorWhenInsertWithTTL() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> getGraphTemplate().insert(human, Duration.ZERO));
    }

    @Test
    void shouldReturnErrorWhenInsertIterableWithTTL() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> getGraphTemplate().insert(Collections.singleton(human), Duration.ZERO));
    }

    @Test
    void shouldInsertEntities() {
        Human otavio = Human.builder().withAge()
                .withName("Otavio").build();

        Human poliana = Human.builder().withAge()
                .withName("Poliana").build();

        final Iterable<Human> people = getGraphTemplate()
                .insert(Arrays.asList(otavio, poliana));

        final boolean allHasId = StreamSupport.stream(people.spliterator(), false)
                .map(Human::getId)
                .allMatch(Objects::nonNull);
        assertTrue(allHasId);
    }

    @Test
    void shouldMergeOnInsert() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Human updated = getGraphTemplate().insert(human);
        assertSame(human, updated);
    }

    @Test
    void shouldGetErrorWhenIdIsNullWhenUpdate() {
        assertThrows(EmptyResultException.class, () -> {
            Human human = Human.builder().withAge()
                    .withName("Otavio").build();
            getGraphTemplate().update(human);
        });
    }

    @Test
    void shouldGetErrorWhenEntityIsNotSavedYet() {
        assertThrows(EmptyResultException.class, () -> {
            Human human = Human.builder().withAge()
                    .withId(10L)
                    .withName("Otavio").build();

            getGraphTemplate().update(human);
        });
    }

    @Test
    void shouldUpdate() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Human updated = getGraphTemplate().insert(human);
        Human newHuman = Human.builder()
                .withAge()
                .withId(updated.getId())
                .withName("Otavio Updated").build();

        Human update = getGraphTemplate().update(newHuman);

        assertEquals(newHuman, update);

        getGraphTemplate().delete(update.getId());
    }

    @Test
    void shouldUpdateEntities() {
        Human otavio = Human.builder().withAge()
                .withName("Otavio").build();

        Human poliana = Human.builder().withAge()
                .withName("Poliana").build();

        final Iterable<Human> insertPeople = getGraphTemplate().insert(Arrays.asList(otavio, poliana));

        final List<Human> newPeople = StreamSupport.stream(insertPeople.spliterator(), false)
                .map(p -> Human.builder().withAge().withId(p.getId()).withName(p.getName() + " updated").build())
                .collect(toList());

        final Iterable<Human> update = getGraphTemplate().update(newPeople);

        final boolean allUpdated = StreamSupport.stream(update.spliterator(), false)
                .map(Human::getName).allMatch(name -> name.contains(" updated"));

        assertTrue(allUpdated);
    }


    @Test
    void shouldMergeOnUpdate() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Human updated = getGraphTemplate().insert(human);
        Human newHuman = Human.builder()
                .withAge()
                .withId(updated.getId())
                .withName("Otavio Updated").build();

        Human update = getGraphTemplate().update(newHuman);

        assertSame(update, newHuman);
    }

    @Test
    void shouldReturnErrorInFindWhenIdIsNull() {
        assertThrows(NullPointerException.class, () -> getGraphTemplate().find(null));
    }

    @Test
    void shouldFindAnEntity() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Human updated = getGraphTemplate().insert(human);
        Optional<Human> personFound = getGraphTemplate().find(updated.getId());

        assertTrue(personFound.isPresent());
        assertEquals(updated, personFound.get());

        getGraphTemplate().delete(updated.getId());
    }

    @Test
    void shouldNotFindAnEntity() {
        Optional<Human> personFound = getGraphTemplate().find(0L);
        assertFalse(personFound.isPresent());
    }

    @Test
    void shouldDeleteById() {

        Human human = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertTrue(getGraphTemplate().find(human.getId()).isPresent());
        getGraphTemplate().delete(human.getId());
        assertFalse(getGraphTemplate().find(human.getId()).isPresent());
    }


    @Test
    void shouldDeleteAnEntityFromTemplate() {

        Human human = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertTrue(getGraphTemplate().find(human.getId()).isPresent());
        getGraphTemplate().delete(Human.class, human.getId());
        assertFalse(getGraphTemplate().find(human.getId()).isPresent());
    }

    @Test
    void shouldNotDeleteAnEntityFromTemplate() {

        Human human = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertTrue(getGraphTemplate().find(human.getId()).isPresent());
        getGraphTemplate().delete(Magazine.class, human.getId());
        assertTrue(getGraphTemplate().find(human.getId()).isPresent());
        getGraphTemplate().delete(Human.class, human.getId());
        assertFalse(getGraphTemplate().find(human.getId()).isPresent());
    }


    @Test
    void shouldDeleteEntitiesById() {

        Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        Human poliana = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Poliana").build());

        assertTrue(getGraphTemplate().find(otavio.getId()).isPresent());
        getGraphTemplate().delete(Arrays.asList(otavio.getId(), poliana.getId()));
        assertFalse(getGraphTemplate().find(otavio.getId()).isPresent());
        assertFalse(getGraphTemplate().find(poliana.getId()).isPresent());
    }

    @Test
    void shouldReturnErrorWhenGetEdgesIdHasNullId() {
        assertThrows(NullPointerException.class, () -> getGraphTemplate().edgesById(null, Direction.BOTH));
    }

    @Test
    void shouldReturnErrorWhenGetEdgesIdHasNullDirection() {
        assertThrows(NullPointerException.class, () -> getGraphTemplate().edgesById(10, null));
    }

    @Test
    void shouldReturnEmptyWhenVertexDoesNotExist() {
        Collection<EdgeEntity> edges = getGraphTemplate().edgesById(10, Direction.BOTH);
        assertTrue(edges.isEmpty());
    }

    @Test
    void shouldReturnEdgesById() {
        Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        Creature dog = getGraphTemplate().insert(new Creature("dog"));
        Magazine cleanCode = getGraphTemplate().insert(Magazine.builder().withName("Clean code").build());

        EdgeEntity likes = getGraphTemplate().edge(otavio, "likes", dog);
        EdgeEntity reads = getGraphTemplate().edge(otavio, "reads", cleanCode);

        Collection<EdgeEntity> edgesById = getGraphTemplate().edgesById(otavio.getId(), Direction.BOTH);
        Collection<EdgeEntity> edgesById1 = getGraphTemplate().edgesById(otavio.getId(), Direction.BOTH, "reads");
        Collection<EdgeEntity> edgesById2 = getGraphTemplate().edgesById(otavio.getId(), Direction.BOTH, () -> "likes");
        Collection<EdgeEntity> edgesById3 = getGraphTemplate().edgesById(otavio.getId(), Direction.OUT);
        Collection<EdgeEntity> edgesById4 = getGraphTemplate().edgesById(cleanCode.getId(), Direction.IN);

        assertEquals(edgesById, edgesById3);
        assertThat(edgesById).contains(likes, reads);
        assertThat(edgesById1).contains(reads);
        assertThat(edgesById2).contains(likes);
        assertThat(edgesById4).contains(reads);

    }

    @Test
    void shouldDeleteEdge() {
        Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());
        Creature dog = getGraphTemplate().insert(new Creature("Ada"));

        EdgeEntity likes = getGraphTemplate().edge(otavio, "likes", dog);

        final Optional<EdgeEntity> edge = getGraphTemplate().edge(likes.id());
        Assertions.assertTrue(edge.isPresent());

        getGraphTemplate().deleteEdge(likes.id());
        assertFalse(getGraphTemplate().edge(likes.id()).isPresent());
    }

    @Test
    void shouldDeleteEdges() {
        Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());
        Creature dog = getGraphTemplate().insert(new Creature("Ada"));
        Magazine cleanCode = getGraphTemplate().insert(Magazine.builder().withName("Clean code").build());

        EdgeEntity likes = getGraphTemplate().edge(otavio, "likes", dog);
        EdgeEntity reads = getGraphTemplate().edge(otavio, "reads", cleanCode);

        final Optional<EdgeEntity> edge = getGraphTemplate().edge(likes.id());
        Assertions.assertTrue(edge.isPresent());

        getGraphTemplate().deleteEdge(Arrays.asList(likes.id(), reads.id()));
        assertFalse(getGraphTemplate().edge(likes.id()).isPresent());
        assertFalse(getGraphTemplate().edge(reads.id()).isPresent());
    }

    @Test
    void shouldReturnErrorWhenGetEdgesHasNullId() {
        assertThrows(NullPointerException.class, () -> getGraphTemplate().edges(null, Direction.BOTH));
    }

    @Test
    void shouldReturnErrorWhenGetEdgesHasNullId2() {
            Human otavio = Human.builder().withId(0L).withAge().withName("Otavio").build();
        Collection<EdgeEntity> edges = getGraphTemplate().edges(otavio, Direction.BOTH);
        assertThat(edges).isEmpty();
    }

    @Test
    void shouldReturnErrorWhenGetEdgesHasNullDirection() {
        assertThrows(NullPointerException.class, () -> {
            Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                    .withName("Otavio").build());
            getGraphTemplate().edges(otavio, null);
        });
    }

    @Test
    void shouldReturnEmptyWhenEntityDoesNotExist() {
        Human otavio = Human.builder().withAge().withName("Otavio").withId(10L).build();
        Collection<EdgeEntity> edges = getGraphTemplate().edges(otavio, Direction.BOTH);
        assertTrue(edges.isEmpty());
    }


    @Test
    void shouldReturnEdges() {
        Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        Creature dog = getGraphTemplate().insert(new Creature("dog"));
        Magazine cleanCode = getGraphTemplate().insert(Magazine.builder().withName("Clean code").build());

        EdgeEntity likes = getGraphTemplate().edge(otavio, "likes", dog);
        EdgeEntity reads = getGraphTemplate().edge(otavio, "reads", cleanCode);

        Collection<EdgeEntity> edgesById = getGraphTemplate().edges(otavio, Direction.BOTH);
        Collection<EdgeEntity> edgesById1 = getGraphTemplate().edges(otavio, Direction.BOTH, "reads");
        Collection<EdgeEntity> edgesById2 = getGraphTemplate().edges(otavio, Direction.BOTH, () -> "likes");
        Collection<EdgeEntity> edgesById3 = getGraphTemplate().edges(otavio, Direction.OUT);
        Collection<EdgeEntity> edgesById4 = getGraphTemplate().edges(cleanCode, Direction.IN);

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(edgesById).contains(likes, reads);
            soft.assertThat(edgesById1).contains(reads);
            soft.assertThat(edgesById2).contains(likes);
            soft.assertThat(edgesById3).contains(likes, reads);
            soft.assertThat(edgesById4).contains(reads);
        });
    }

    @Test
    void shouldGetTransaction() {
        Transaction transaction = getGraphTemplate().transaction();
        assertNotNull(transaction);
    }

    @Test
    void shouldExecuteQuery() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        getGraphTemplate().insert(human);
        List<Human> people = getGraphTemplate()
                .<Human>gremlin("g.V().hasLabel('Human')")
                .toList();
        assertThat(people.stream().map(Human::getName).collect(toList())).contains("Otavio");
    }


    @Test
    void shouldExecuteQueryWithParameter() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Map<String, Object> parameters = Collections.singletonMap("name", "Otavio");
        getGraphTemplate().insert(human);
        List<Human> people = getGraphTemplate()
                .<Human>gremlin("g.V().hasLabel('Human').has('name', @name)", parameters)
                .toList();

        assertThat(people.stream().map(Human::getName).collect(toList())).contains("Otavio");
    }

    @Test
    void shouldReturnEmpty() {
        Optional<Human> person = getGraphTemplate().gremlinSingleResult("g.V().hasLabel('Person')");
        assertFalse(person.isPresent());
    }

    @Test
    void shouldReturnOneElement() {
        Human otavio = Human.builder().withAge()
                .withName("Otavio").build();
        getGraphTemplate().insert(otavio);
        Optional<Human> person = getGraphTemplate().gremlinSingleResult("g.V().hasLabel('Human')");
        assertTrue(person.isPresent());
    }

    @Test
    void shouldReturnErrorWhenHasNoneThanOneElement() {

        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertThrows(NonUniqueResultException.class, () -> getGraphTemplate().gremlinSingleResult("g.V().hasLabel('Human')"));
    }

    @Test
    void shouldExecutePrepareStatement() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        PreparedStatement prepare = getGraphTemplate().gremlinPrepare("g.V().hasLabel(@param)");
        prepare.bind("param", "Human");
        List<Human> people = prepare.<Human>result().toList();
        assertThat(people.stream().map(Human::getName).collect(toList())).contains("Otavio");
    }

    @Test
    void shouldExecutePrepareStatementSingleton() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        PreparedStatement prepare = getGraphTemplate().gremlinPrepare("g.V().hasLabel(@param)");
        prepare.bind("param", "Human");
        Optional<Human> otavio = prepare.singleResult();
        assertTrue(otavio.isPresent());
    }

    @Test
    void shouldExecutePrepareStatementSingletonEmpty() {
        PreparedStatement prepare = getGraphTemplate().gremlinPrepare("g.V().hasLabel(@param)");
        prepare.bind("param", "Person");
        Optional<Human> otavio = prepare.singleResult();
        assertFalse(otavio.isPresent());
    }

    @Test
    void shouldExecutePrepareStatementWithErrorWhenThereIsMoreThanOneResult() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        PreparedStatement prepare = getGraphTemplate().gremlinPrepare("g.V().hasLabel(@param)");
        prepare.bind("param", "Human");
        assertThrows(NonUniqueResultException.class, prepare::singleResult);
    }

    @Test
    void shouldCount() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertEquals(2L, getGraphTemplate().count("Human"));
    }

    @Test
    void shouldCountFromEntity() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertEquals(2L, getGraphTemplate().count(Human.class));
    }

    @Test
    void shouldCountFromSelectQuery() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertEquals(2L, getGraphTemplate().count(SelectQuery.builder().select().from("Human").build()));
    }


    @Test
    void shouldFindById() {
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        final Optional<Human> person = getGraphTemplate().find(Human.class, otavio.getId());
        assertNotNull(person);
        assertTrue(person.isPresent());
        assertEquals(otavio.getName(), person.map(Human::getName).get());
    }

    @Test
    void shouldFindAll() {
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());
        List<Human> people = getGraphTemplate().findAll(Human.class).toList();

        assertThat(people).hasSize(1)
                .map(Human::getName)
                .contains("Otavio");
    }

    @Test
    void shouldDeleteAll() {
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());
        List<Human> people = getGraphTemplate().findAll(Human.class).toList();

        assertThat(people).hasSize(1)
                .map(Human::getName)
                .contains("Otavio");

        getGraphTemplate().deleteAll(Human.class);
        people = getGraphTemplate().findAll(Human.class).toList();

        assertThat(people).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenFindByIdNotFound() {

        final Optional<Human> person = getGraphTemplate().find(Human.class, -2L);
        assertNotNull(person);
        assertFalse(person.isPresent());
    }

    @Test
    void shouldUpdateNullValues(){
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertEquals("Otavio", otavio.getName());
        otavio.setName(null);
        final Human human = getGraphTemplate().update(otavio);
        assertNull(human.getName());

    }

    @Test
    void shouldCreateEdgeByGraphAPI() {
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        final Human poliana = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Poliana").build());

        var edge = org.eclipse.jnosql.mapping.graph.Edge.source(otavio).label("LOVES").target(poliana).build();
        var edgeEntity = getGraphTemplate().edge(edge);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(edgeEntity).isNotNull();
            softly.assertThat(edgeEntity.label()).isEqualTo("LOVES");
            softly.assertThat(edgeEntity.source()).isEqualTo(otavio);
            softly.assertThat(edgeEntity.target()).isEqualTo(poliana);
        });
    }

    @Test
    void shouldCreateEdgeByGraphAPIWithProperties() {
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        final Human poliana = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Poliana").build());

        var edge = org.eclipse.jnosql.mapping.graph.Edge.source(otavio)
                .label("LOVES")
                .target(poliana)
                .property("when", "2017")
                .property("where", "Brazil")
                .build();
        var edgeEntity = getGraphTemplate().edge(edge);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(edgeEntity).isNotNull();
            softly.assertThat(edgeEntity.label()).isEqualTo("LOVES");
            softly.assertThat(edgeEntity.source()).isEqualTo(otavio);
            softly.assertThat(edgeEntity.target()).isEqualTo(poliana);
            softly.assertThat(edgeEntity.properties()).hasSize(2);
            softly.assertThat(edgeEntity.property("when", String.class)).contains("2017");
            softly.assertThat(edgeEntity.property("where", String.class)).contains("Brazil");
        });
    }
}
