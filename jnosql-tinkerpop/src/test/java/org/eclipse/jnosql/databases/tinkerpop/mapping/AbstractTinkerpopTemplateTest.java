/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
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
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> getGraphTemplate().insert(null));
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
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> getGraphTemplate().insert(human, Duration.ZERO));
    }

    @Test
    void shouldReturnErrorWhenInsertIterableWithTTL() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> getGraphTemplate().insert(Collections.singleton(human), Duration.ZERO));
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
        assertThat(allHasId).isTrue();
    }

    @Test
    void shouldMergeOnInsert() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Human updated = getGraphTemplate().insert(human);
        assertThat(updated).isSameAs(human);
    }

    @Test
    void shouldGetErrorWhenIdIsNullWhenUpdate() {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
            Human human = Human.builder().withAge()
                    .withName("Otavio").build();
            getGraphTemplate().update(human);
        });
    }

    @Test
    void shouldGetErrorWhenEntityIsNotSavedYet() {
        assertThatExceptionOfType(EmptyResultException.class).isThrownBy(() -> {
            Human human = Human.builder().withAge()
                    .withId("10")
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

        assertThat(update).isEqualTo(newHuman);

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

        assertThat(allUpdated).isTrue();
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

        assertThat(newHuman).isSameAs(update);
    }

    @Test
    void shouldReturnErrorInFindWhenIdIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> getGraphTemplate().find(null));
    }

    @Test
    void shouldFindAnEntity() {
        Human human = Human.builder().withAge()
                .withName("Otavio").build();
        Human updated = getGraphTemplate().insert(human);
        Optional<Human> personFound = getGraphTemplate().find(updated.getId());

        assertThat(personFound.isPresent()).isTrue();
        assertThat(personFound.get()).isEqualTo(updated);

        getGraphTemplate().delete(updated.getId());
    }

    @Test
    void shouldNotFindAnEntity() {
        Optional<Human> personFound = getGraphTemplate().find("0");
        assertThat(personFound.isPresent()).isFalse();
    }

    @Test
    void shouldDeleteById() {

        Human human = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertThat(getGraphTemplate().find(human.getId()).isPresent()).isTrue();
        getGraphTemplate().delete(human.getId());
        assertThat(getGraphTemplate().find(human.getId()).isPresent()).isFalse();
    }


    @Test
    void shouldDeleteAnEntityFromTemplate() {

        Human human = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertThat(getGraphTemplate().find(human.getId()).isPresent()).isTrue();
        getGraphTemplate().delete(Human.class, human.getId());
        assertThat(getGraphTemplate().find(human.getId()).isPresent()).isFalse();
    }

    @Test
    void shouldNotDeleteAnEntityFromTemplate() {

        Human human = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertThat(getGraphTemplate().find(human.getId()).isPresent()).isTrue();
        getGraphTemplate().delete(Magazine.class, human.getId());
        assertThat(getGraphTemplate().find(human.getId()).isPresent()).isTrue();
        getGraphTemplate().delete(Human.class, human.getId());
        assertThat(getGraphTemplate().find(human.getId()).isPresent()).isFalse();
    }


    @Test
    void shouldDeleteEntitiesById() {

        Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        Human poliana = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Poliana").build());

        assertThat(getGraphTemplate().find(otavio.getId()).isPresent()).isTrue();
        getGraphTemplate().delete(Arrays.asList(otavio.getId(), poliana.getId()));
        assertThat(getGraphTemplate().find(otavio.getId()).isPresent()).isFalse();
        assertThat(getGraphTemplate().find(poliana.getId()).isPresent()).isFalse();
    }

    @Test
    void shouldReturnErrorWhenGetEdgesIdHasNullId() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> getGraphTemplate().edgesById(null, Direction.BOTH));
    }

    @Test
    void shouldReturnErrorWhenGetEdgesIdHasNullDirection() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> getGraphTemplate().edgesById("10", null));
    }

    @Test
    void shouldReturnEmptyWhenVertexDoesNotExist() {
        Collection<EdgeEntity> edges = getGraphTemplate().edgesById("10", Direction.BOTH);
        assertThat(edges.isEmpty()).isTrue();
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

        assertThat(edgesById3).isEqualTo(edgesById);
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
        assertThat(edge.isPresent()).isTrue();

        getGraphTemplate().deleteEdge(likes.id());
        assertThat(getGraphTemplate().edge(likes.id()).isPresent()).isFalse();
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
        assertThat(edge.isPresent()).isTrue();

        getGraphTemplate().deleteEdge(Arrays.asList(likes.id(), reads.id()));
        assertThat(getGraphTemplate().edge(likes.id()).isPresent()).isFalse();
        assertThat(getGraphTemplate().edge(reads.id()).isPresent()).isFalse();
    }

    @Test
    void shouldReturnErrorWhenGetEdgesHasNullId() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> getGraphTemplate().edges(null, Direction.BOTH));
    }

    @Test
    void shouldReturnErrorWhenGetEdgesHasNullId2() {
        Human otavio = Human.builder().withId("0").withAge().withName("Otavio").build();
        Collection<EdgeEntity> edges = getGraphTemplate().edges(otavio, Direction.BOTH);
        assertThat(edges).isEmpty();
    }

    @Test
    void shouldReturnErrorWhenGetEdgesHasNullDirection() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                    .withName("Otavio").build());
            getGraphTemplate().edges(otavio, null);
        });
    }

    @Test
    void shouldReturnEmptyWhenEntityDoesNotExist() {
        Human otavio = Human.builder().withAge().withName("Otavio").withId("10").build();
        Collection<EdgeEntity> edges = getGraphTemplate().edges(otavio, Direction.BOTH);
        assertThat(edges.isEmpty()).isTrue();
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
        assumeTrue(getGraph().features().graph().supportsTransactions(), "transactions not supported");
        Transaction transaction = getGraphTemplate().transaction();
        assertThat(transaction).isNotNull();
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
        Optional<Human> person = getGraphTemplate().gremlinSingleResult("g.V().hasLabel('person')");
        assertThat(person.isPresent()).isFalse();
    }

    @Test
    void shouldReturnOneElement() {
        Human otavio = Human.builder().withAge()
                .withName("Otavio").build();
        getGraphTemplate().insert(otavio);
        Optional<Human> person = getGraphTemplate().gremlinSingleResult("g.V().hasLabel('Human')");
        assertThat(person.isPresent()).isTrue();
    }

    @Test
    void shouldReturnErrorWhenHasNoneThanOneElement() {

        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertThatExceptionOfType(NonUniqueResultException.class).isThrownBy(() -> getGraphTemplate().gremlinSingleResult("g.V().hasLabel('Human')"));
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
        assertThat(otavio.isPresent()).isTrue();
    }

    @Test
    void shouldExecutePrepareStatementSingletonEmpty() {
        PreparedStatement prepare = getGraphTemplate().gremlinPrepare("g.V().hasLabel(@param)");
        prepare.bind("param", "Person");
        Optional<Human> otavio = prepare.singleResult();
        assertThat(otavio.isPresent()).isFalse();
    }

    @Test
    void shouldExecutePrepareStatementWithErrorWhenThereIsMoreThanOneResult() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        PreparedStatement prepare = getGraphTemplate().gremlinPrepare("g.V().hasLabel(@param)");
        prepare.bind("param", "Human");
        assertThatExceptionOfType(NonUniqueResultException.class).isThrownBy(prepare::singleResult);
    }

    @Test
    void shouldCount() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertThat(getGraphTemplate().count("Human")).isEqualTo(2L);
    }

    @Test
    void shouldCountFromEntity() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertThat(getGraphTemplate().count(Human.class)).isEqualTo(2L);
    }

    @Test
    void shouldCountFromSelectQuery() {
        getGraphTemplate().insert(Human.builder().withAge().withName("Otavio").build());
        getGraphTemplate().insert(Human.builder().withAge().withName("Poliana").build());
        assertThat(getGraphTemplate().count(SelectQuery.builder().select().from("Human").build())).isEqualTo(2L);
    }


    @Test
    void shouldFindById() {
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        final Optional<Human> person = getGraphTemplate().find(Human.class, otavio.getId());
        assertThat(person).isNotNull();
        assertThat(person.isPresent()).isTrue();
        assertThat(person.map(Human::getName).get()).isEqualTo(otavio.getName());
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
        final Optional<Human> person = getGraphTemplate().find(Human.class, "-2");
        assertThat(person).isNotNull();
        assertThat(person.isPresent()).isFalse();
    }

    @Test
    void shouldUpdateNullValues(){
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        assertThat(otavio.getName()).isEqualTo("Otavio");
        otavio.setName(null);
        final Human human = getGraphTemplate().update(otavio);
        assertThat(human.getName()).isNull();

    }

    @Test
    void shouldCreateEdgeByGraphAPI() {
        final Human otavio = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Otavio").build());

        final Human poliana = getGraphTemplate().insert(Human.builder().withAge()
                .withName("Poliana").build());

        var edge = org.eclipse.jnosql.mapping.graph.Edge.source(otavio).label("loves").target(poliana).build();
        var edgeEntity = getGraphTemplate().edge(edge);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(edgeEntity).isNotNull();
            softly.assertThat(edgeEntity.label()).isEqualTo("loves");
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
                .label("loves")
                .target(poliana)
                .property("when", "2017")
                .property("where", "Brazil")
                .build();
        var edgeEntity = getGraphTemplate().edge(edge);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(edgeEntity).isNotNull();
            softly.assertThat(edgeEntity.label()).isEqualTo("loves");
            softly.assertThat(edgeEntity.source()).isEqualTo(otavio);
            softly.assertThat(edgeEntity.target()).isEqualTo(poliana);
            softly.assertThat(edgeEntity.properties()).hasSize(2);
            softly.assertThat(edgeEntity.property("when", String.class)).contains("2017");
            softly.assertThat(edgeEntity.property("where", String.class)).contains("Brazil");
        });
    }
}
