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

import jakarta.data.exceptions.NonUniqueResultException;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.T;
import org.eclipse.jnosql.databases.tinkerpop.cdi.arangodb.ArangoDBGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.neo4j.Neo4jGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.cdi.tinkergraph.TinkerGraphProducer;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Creature;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class})
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
abstract class DefaultVertexTraversalTest extends AbstractTraversalTest {

    @AddPackages(ArangoDBGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class ArangoDBTest extends DefaultVertexTraversalTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class Neo4jTest extends DefaultVertexTraversalTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends DefaultVertexTraversalTest {
    }

    @Test
    void shouldReturnErrorWhenVertexIdIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex(null));
    }

    @Test
    void shouldGetVertexFromId() {
        List<Human> people = tinkerpopTemplate.traversalVertex(otavio.getId(), poliana.getId()).<Human>result()
                .collect(toList());

        assertThat(people).contains(otavio, poliana);
    }

    @Test
    void shouldDefineLimit() {
        List<Human> people = tinkerpopTemplate.traversalVertex(otavio.getId(), poliana.getId(),
                        paulo.getId()).limit(1)
                .<Human>result()
                .collect(toList());

        assertThat(people.size()).isEqualTo(1);
        assertThat(people).contains(otavio);
    }

    @Test
    void shouldDefineLimit2() {
        List<Human> people = tinkerpopTemplate.traversalVertex(otavio.getId(), poliana.getId(), paulo.getId()).
                <Human>next(2)
                .collect(toList());

        assertThat(people.size()).isEqualTo(2);
        assertThat(people).contains(otavio, poliana);
    }

    @Test
    void shouldNext() {
        Optional<?> next = tinkerpopTemplate.traversalVertex().next();
        assertThat(next.isPresent()).isTrue();
    }

    @Test
    void shouldEmptyNext() {
        Optional<?> next = tinkerpopTemplate.traversalVertex("-12").next();
        assertThat(next.isPresent()).isFalse();
    }


    @Test
    void shouldHave() {
        Optional<Human> person = tinkerpopTemplate.traversalVertex().has("name", "Poliana").next();
        assertThat(person.isPresent()).isTrue();
        assertThat(poliana).isEqualTo(person.get());
    }

    @Test
    void shouldReturnErrorWhenHasNullKey() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex()
                .has((String) null, "Poliana")
                .next());
    }


    @Test
    void shouldReturnErrorWhenHasNullValue() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().has("name", null)
                .next());
    }

    @Test
    void shouldHaveId() {
        Optional<Human> person = tinkerpopTemplate.traversalVertex().has(T.id, poliana.getId()).next();
        assertThat(person.isPresent()).isTrue();
        assertThat(poliana).isEqualTo(person.get());
    }

    @Test
    void shouldReturnErrorWhenHasIdHasNullValue() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().has(T.id, null).next());
    }

    @Test
    void shouldReturnErrorWhenHasIdHasNullAccessor() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            T id = null;
            tinkerpopTemplate.traversalVertex().has(id, poliana.getId()).next();
        });
    }


    @Test
    void shouldHavePredicate() {
        List<?> result = tinkerpopTemplate.traversalVertex().has("age", P.gt(26))
                .result()
                .toList();
        assertThat(result.size()).isEqualTo(5);
    }

    @Test
    void shouldReturnErrorWhenHasPredicateIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> {
            P<Integer> gt = null;
            tinkerpopTemplate.traversalVertex().has("age", gt)
                    .result()
                    .toList();
        });
    }

    @Test
    void shouldReturnErrorWhenHasKeyIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().has((String) null,
                        P.gt(26))
                .result()
                .toList());
    }

    @Test
    void shouldHaveLabel() {
        List<Magazine> magazines = tinkerpopTemplate.traversalVertex().hasLabel("Magazine").<Magazine>result().collect(toList());
        assertThat(magazines.size()).isEqualTo(3);
        assertThat(magazines).contains(shack, license, effectiveJava);
    }

    @Test
    void shouldHaveLabel2() {

        List<Object> entities = tinkerpopTemplate.traversalVertex()
                .hasLabel(P.eq("Magazine").or(P.eq("Human")))
                .result().collect(toList());
        assertThat(entities).hasSize(6).contains(shack, license, effectiveJava, otavio, poliana, paulo);
    }

    @Test
    void shouldReturnErrorWhenHasLabelHasNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().hasLabel((String) null)
                .<Magazine>result().toList());
    }

    @Test
    void shouldIn() {
        List<Magazine> magazines = tinkerpopTemplate.traversalVertex().out(READS).<Magazine>result().collect(toList());
        assertThat(magazines.size()).isEqualTo(3);
        assertThat(magazines).contains(shack, license, effectiveJava);
    }

    @Test
    void shouldReturnErrorWhenInIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().out((String) null).<Magazine>result().toList());
    }

    @Test
    void shouldOut() {
        List<Human> people = tinkerpopTemplate.traversalVertex().in(READS).<Human>result().collect(toList());
        assertThat(people.size()).isEqualTo(3);
        assertThat(people).contains(otavio, poliana, paulo);
    }

    @Test
    void shouldReturnErrorWhenOutIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().in((String) null).<Human>result().toList());
    }

    @Test
    void shouldBoth() {
        List<?> entities = tinkerpopTemplate.traversalVertex().both(READS)
                .<Human>result().toList();
        assertThat(entities.size()).isEqualTo(6);
    }

    @Test
    void shouldReturnErrorWhenBothIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().both((String) null)
                .<Human>result().toList());
    }

    @Test
    void shouldNot() {
        List<?> result = tinkerpopTemplate.traversalVertex().hasNot("year").result().toList();
        assertThat(result.size()).isEqualTo(6);
    }

    @Test
    void shouldReturnErrorWhenHasNotIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().hasNot((String) null)
                .result().toList());
    }

    @Test
    void shouldCount() {
        long count = tinkerpopTemplate.traversalVertex().both(READS).count();
        assertThat(count).isEqualTo(6L);
    }

    @Test
    void shouldReturnZeroWhenCountIsEmpty() {
        long count = tinkerpopTemplate.traversalVertex().both("WRITES").count();
        assertThat(count).isEqualTo(0L);
    }

    @Test
    void shouldDefinesLimit() {
        long count = tinkerpopTemplate.traversalVertex().limit(1L).count();
        assertThat(count).isEqualTo(1L);
        assertThat(count).isNotEqualTo(tinkerpopTemplate.traversalVertex().count());
    }

    @Test
    void shouldDefinesRange() {
        long count = tinkerpopTemplate.traversalVertex().range(1, 3).count();
        assertThat(count).isEqualTo(2L);
        assertThat(count).isNotEqualTo(tinkerpopTemplate.traversalVertex().count());
    }

    @Test
    void shouldMapValuesAsStream() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .valueMap("name").stream().toList();

        assertThat(maps.isEmpty()).isFalse();
        assertThat(maps.size()).isEqualTo(3);

        List<String> names = new ArrayList<>();

        maps.forEach(m -> names.add(m.get("name").toString()));

        assertThat(names).contains("Otavio", "Poliana", "Paulo");
    }

    @Test
    void shouldMapValuesAsStreamLimit() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .valueMap("name").next(2).toList();

        assertThat(maps.isEmpty()).isFalse();
        assertThat(maps.size()).isEqualTo(2);
    }


    @Test
    void shouldReturnMapValueAsEmptyStream() {
        Stream<Map<String, Object>> stream = tinkerpopTemplate.traversalVertex().hasLabel("Person")
                .valueMap("noField").stream();
        assertThat(stream.allMatch(m -> Objects.isNull(m.get("noFoundProperty")))).isTrue();
    }

    @Test
    void shouldReturnNext() {
        Map<String, Object> map = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .valueMap("name").next();

        assertThat(map).isNotNull();
        assertThat(map.isEmpty()).isFalse();
    }


    @Test
    void shouldRepeatTimesTraversal() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake).add("when", "night");
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);
        Optional<Creature> animal = tinkerpopTemplate.traversalVertex().repeat().out("eats").times(3).next();
        assertThat(animal.isPresent()).isTrue();
        assertThat(animal.get()).isEqualTo(plant);

    }

    @Test
    void shouldRepeatTimesTraversal2() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake).add("when", "night");
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);
        Optional<Creature> animal = tinkerpopTemplate.traversalVertex().repeat().in("eats").times(3).next();
        assertThat(animal.isPresent()).isTrue();
        assertThat(animal.get()).isEqualTo(lion);

    }

    @Test
    void shouldRepeatUntilTraversal() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake);
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);

        Optional<Creature> animal = tinkerpopTemplate.traversalVertex()
                .repeat().out("eats")
                .until().has("name", "plant").next();

        assertThat(animal.isPresent()).isTrue();


        assertThat(animal.get()).isEqualTo(plant);
    }

    @Test
    void shouldRepeatUntilTraversal2() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake);
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);

        Optional<Creature> animal = tinkerpopTemplate.traversalVertex()
                .repeat().in("eats")
                .until().has("name", "lion").next();

        assertThat(animal.isPresent()).isTrue();


        assertThat(animal.get()).isEqualTo(lion);
    }


    @Test
    void shouldReturnErrorWhenTheOrderIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().orderBy(null));
    }

    @Test
    void shouldReturnErrorWhenThePropertyDoesNotExist() {
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
                tinkerpopTemplate.traversalVertex().orderBy("wrong property").asc().next().get());
    }

    @Test
    void shouldOrderAsc() {
        String property = "name";

        List<String> properties = tinkerpopTemplate.traversalVertex()
                .hasLabel("Magazine")
                .has(property)
                .orderBy(property)
                .asc().<Magazine>result()
                .map(Magazine::getName)
                .collect(toList());

        assertThat(properties).contains("Effective Java", "Software License", "The Shack");
    }

    @Test
    void shouldOrderDesc() {
        String property = "name";

        List<String> properties = tinkerpopTemplate.traversalVertex()
                .hasLabel("Magazine")
                .has(property)
                .orderBy(property)
                .desc().<Magazine>result()
                .map(Magazine::getName)
                .collect(toList());

        assertThat(properties).contains("The Shack", "Software License", "Effective Java");
    }

    @Test
    void shouldReturnErrorWhenHasLabelStringNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().hasLabel((String) null));
    }

    @Test
    void shouldReturnErrorWhenHasLabelSupplierNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().hasLabel((Supplier<String>) null));
    }

    @Test
    void shouldReturnErrorWhenHasLabelEntityClassNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().hasLabel((Class<?>) null));
    }

    @Test
    void shouldReturnHasLabel() {
        assertThat(tinkerpopTemplate.traversalVertex().hasLabel("Person").result().allMatch(Human.class::isInstance)).isTrue();
        assertThat(tinkerpopTemplate.traversalVertex().hasLabel(() -> "Book").result().allMatch(Magazine.class::isInstance)).isTrue();
        assertThat(tinkerpopTemplate.traversalVertex().hasLabel(Creature.class).result().allMatch(Creature.class::isInstance)).isTrue();
    }

    @Test
    void shouldReturnResultAsList() {
        List<Human> people = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .<Human>result()
                .toList();
        assertThat(people.size()).isEqualTo(3);
    }

    @Test
    void shouldReturnErrorWhenThereAreMoreThanOneInGetSingleResult() {
        assertThatExceptionOfType(NonUniqueResultException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().hasLabel("Human").singleResult());
    }

    @Test
    void shouldReturnOptionalEmptyWhenThereIsNotResultInSingleResult() {
        Optional<Object> entity = tinkerpopTemplate.traversalVertex().hasLabel("NoEntity").singleResult();
        assertThat(entity.isPresent()).isFalse();
    }

    @Test
    void shouldReturnSingleResult() {
        String name = "Poliana";
        Optional<Human> poliana = tinkerpopTemplate.traversalVertex().hasLabel("Human").
                has("name", name).singleResult();
        assertThat(poliana.map(Human::getName).orElse("")).isEqualTo(name);
    }

    @Test
    void shouldReturnErrorWhenPredicateIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().filter(null));
    }

    @Test
    void shouldPredicate() {
        long count = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .filter(Human::isAdult).count();
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void shouldDedup() {

        tinkerpopTemplate.edge(otavio, "knows", paulo);
        tinkerpopTemplate.edge(paulo, "knows", otavio);
        tinkerpopTemplate.edge(otavio, "knows", poliana);
        tinkerpopTemplate.edge(poliana, "knows", otavio);
        tinkerpopTemplate.edge(poliana, "knows", paulo);
        tinkerpopTemplate.edge(paulo, "knows", poliana);

        List<Human> people = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .in("knows").<Human>result()
                .collect(Collectors.toList());

        assertThat(people.size()).isEqualTo(6);

        people = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .in("knows").dedup().<Human>result()
                .toList();

        assertThat(people.size()).isEqualTo(3);
    }

}
