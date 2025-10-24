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
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.structure.T;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, TinkerpopTemplate.class})
@AddPackages(GraphProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class, TinkerpopExtension.class})
class DefaultVertexTraversalTest extends AbstractTraversalTest {


    @Test
    void shouldReturnErrorWhenVertexIdIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex(null));
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

        assertEquals(1, people.size());
        assertThat(people).contains(otavio);
    }

    @Test
    void shouldDefineLimit2() {
        List<Human> people = tinkerpopTemplate.traversalVertex(otavio.getId(), poliana.getId(), paulo.getId()).
                <Human>next(2)
                .collect(toList());

        assertEquals(2, people.size());
        assertThat(people).contains(otavio, poliana);
    }

    @Test
    void shouldNext() {
        Optional<?> next = tinkerpopTemplate.traversalVertex().next();
        assertTrue(next.isPresent());
    }

    @Test
    void shouldEmptyNext() {
        Optional<?> next = tinkerpopTemplate.traversalVertex("-12").next();
        assertFalse(next.isPresent());
    }


    @Test
    void shouldHave() {
        Optional<Human> person = tinkerpopTemplate.traversalVertex().has("name", "Poliana").next();
        assertTrue(person.isPresent());
        assertEquals(person.get(), poliana);
    }

    @Test
    void shouldReturnErrorWhenHasNullKey() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex()
                .has((String) null, "Poliana")
                .next());
    }


    @Test
    void shouldReturnErrorWhenHasNullValue() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().has("name", null)
                .next());
    }

    @Test
    void shouldHaveId() {
        Optional<Human> person = tinkerpopTemplate.traversalVertex().has(T.id, poliana.getId()).next();
        assertTrue(person.isPresent());
        assertEquals(person.get(), poliana);
    }

    @Test
    void shouldReturnErrorWhenHasIdHasNullValue() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().has(T.id, null).next());
    }

    @Test
    void shouldReturnErrorWhenHasIdHasNullAccessor() {
        assertThrows(NullPointerException.class, () -> {
            T id = null;
            tinkerpopTemplate.traversalVertex().has(id, poliana.getId()).next();
        });
    }


    @Test
    void shouldHavePredicate() {
        List<?> result = tinkerpopTemplate.traversalVertex().has("age", P.gt(26))
                .result()
                .toList();
        assertEquals(5, result.size());
    }

    @Test
    void shouldReturnErrorWhenHasPredicateIsNull() {
        assertThrows(NullPointerException.class, () -> {
            P<Integer> gt = null;
            tinkerpopTemplate.traversalVertex().has("age", gt)
                    .result()
                    .toList();
        });
    }

    @Test
    void shouldReturnErrorWhenHasKeyIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().has((String) null,
                        P.gt(26))
                .result()
                .toList());
    }

    @Test
    void shouldHaveLabel() {
        List<Magazine> magazines = tinkerpopTemplate.traversalVertex().hasLabel("Magazine").<Magazine>result().collect(toList());
        assertEquals(3, magazines.size());
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
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().hasLabel((String) null)
                .<Magazine>result().toList());
    }

    @Test
    void shouldIn() {
        List<Magazine> magazines = tinkerpopTemplate.traversalVertex().out(READS).<Magazine>result().collect(toList());
        assertEquals(3, magazines.size());
        assertThat(magazines).contains(shack, license, effectiveJava);
    }

    @Test
    void shouldReturnErrorWhenInIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().out((String) null).<Magazine>result().toList());
    }

    @Test
    void shouldOut() {
        List<Human> people = tinkerpopTemplate.traversalVertex().in(READS).<Human>result().collect(toList());
        assertEquals(3, people.size());
        assertThat(people).contains(otavio, poliana, paulo);
    }

    @Test
    void shouldReturnErrorWhenOutIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().in((String) null).<Human>result().toList());
    }

    @Test
    void shouldBoth() {
        List<?> entities = tinkerpopTemplate.traversalVertex().both(READS)
                .<Human>result().toList();
        assertEquals(6, entities.size());
    }

    @Test
    void shouldReturnErrorWhenBothIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().both((String) null)
                .<Human>result().toList());
    }

    @Test
    void shouldNot() {
        List<?> result = tinkerpopTemplate.traversalVertex().hasNot("year").result().toList();
        assertEquals(6, result.size());
    }

    @Test
    void shouldReturnErrorWhenHasNotIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().hasNot((String) null)
                .result().toList());
    }

    @Test
    void shouldCount() {
        long count = tinkerpopTemplate.traversalVertex().both(READS).count();
        assertEquals(6L, count);
    }

    @Test
    void shouldReturnZeroWhenCountIsEmpty() {
        long count = tinkerpopTemplate.traversalVertex().both("WRITES").count();
        assertEquals(0L, count);
    }

    @Test
    void shouldDefinesLimit() {
        long count = tinkerpopTemplate.traversalVertex().limit(1L).count();
        assertEquals(1L, count);
        assertNotEquals(tinkerpopTemplate.traversalVertex().count(), count);
    }

    @Test
    void shouldDefinesRange() {
        long count = tinkerpopTemplate.traversalVertex().range(1, 3).count();
        assertEquals(2L, count);
        assertNotEquals(tinkerpopTemplate.traversalVertex().count(), count);
    }

    @Test
    void shouldMapValuesAsStream() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .valueMap("name").stream().toList();

        assertFalse(maps.isEmpty());
        assertEquals(3, maps.size());

        List<String> names = new ArrayList<>();

        maps.forEach(m -> names.add(m.get("name").toString()));

        assertThat(names).contains("Otavio", "Poliana", "Paulo");
    }

    @Test
    void shouldMapValuesAsStreamLimit() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .valueMap("name").next(2).toList();

        assertFalse(maps.isEmpty());
        assertEquals(2, maps.size());
    }


    @Test
    void shouldReturnMapValueAsEmptyStream() {
        Stream<Map<String, Object>> stream = tinkerpopTemplate.traversalVertex().hasLabel("Person")
                .valueMap("noField").stream();
        assertTrue(stream.allMatch(m -> Objects.isNull(m.get("noFoundProperty"))));
    }

    @Test
    void shouldReturnNext() {
        Map<String, Object> map = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .valueMap("name").next();

        assertNotNull(map);
        assertFalse(map.isEmpty());
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
        assertTrue(animal.isPresent());
        assertEquals(plant, animal.get());

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
        assertTrue(animal.isPresent());
        assertEquals(lion, animal.get());

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

        assertTrue(animal.isPresent());


        assertEquals(plant, animal.get());
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

        assertTrue(animal.isPresent());


        assertEquals(lion, animal.get());
    }


    @Test
    void shouldReturnErrorWhenTheOrderIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().orderBy(null));
    }

    @Test
    void shouldReturnErrorWhenThePropertyDoesNotExist() {
        assertThrows(NoSuchElementException.class, () ->
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
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().hasLabel((String) null));
    }

    @Test
    void shouldReturnErrorWhenHasLabelSupplierNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().hasLabel((Supplier<String>) null));
    }

    @Test
    void shouldReturnErrorWhenHasLabelEntityClassNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().hasLabel((Class<?>) null));
    }

    @Test
    void shouldReturnHasLabel() {
        assertTrue(tinkerpopTemplate.traversalVertex().hasLabel("Person").result().allMatch(Human.class::isInstance));
        assertTrue(tinkerpopTemplate.traversalVertex().hasLabel(() -> "Book").result().allMatch(Magazine.class::isInstance));
        assertTrue(tinkerpopTemplate.traversalVertex().hasLabel(Creature.class).result().allMatch(Creature.class::isInstance));
    }

    @Test
    void shouldReturnResultAsList() {
        List<Human> people = tinkerpopTemplate.traversalVertex().hasLabel("Human")
                .<Human>result()
                .toList();
        assertEquals(3, people.size());
    }

    @Test
    void shouldReturnErrorWhenThereAreMoreThanOneInGetSingleResult() {
        assertThrows(NonUniqueResultException.class, () -> tinkerpopTemplate.traversalVertex().hasLabel("Human").singleResult());
    }

    @Test
    void shouldReturnOptionalEmptyWhenThereIsNotResultInSingleResult() {
        Optional<Object> entity = tinkerpopTemplate.traversalVertex().hasLabel("NoEntity").singleResult();
        assertFalse(entity.isPresent());
    }

    @Test
    void shouldReturnSingleResult() {
        String name = "Poliana";
        Optional<Human> poliana = tinkerpopTemplate.traversalVertex().hasLabel("Human").
                has("name", name).singleResult();
        assertEquals(name, poliana.map(Human::getName).orElse(""));
    }

    @Test
    void shouldReturnErrorWhenPredicateIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().filter(null));
    }

    @Test
    void shouldPredicate() {
        long count = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .filter(Human::isAdult).count();
        assertEquals(3L, count);
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

        assertEquals(6, people.size());

        people = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .in("knows").dedup().<Human>result()
                .toList();

        assertEquals(3, people.size());
    }


}
