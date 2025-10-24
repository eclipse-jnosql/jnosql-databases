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
import org.assertj.core.api.SoftAssertions;
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
class DefaultEdgeTraversalTest extends AbstractTraversalTest {

    @Test
    void shouldReturnErrorWhenEdgeIdIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalEdge(null));
    }

    @Test
    void shouldReturnEdgeId() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalEdge(reads.id())
                .next();

        assertTrue(edgeEntity.isPresent());
        assertEquals(reads.id(), edgeEntity.get().id());
    }

    @Test
    void shouldReturnOutE() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().outE(READS)
                .stream()
                .collect(toList());

        assertEquals(3, edges.size());
        assertThat(edges).contains(reads, reads2, reads3);
    }

    @Test
    void shouldReturnOutEWithSupplier() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().outE(() -> READS)
                .stream()
                .collect(toList());

        assertEquals(3, edges.size());
        assertThat(edges).contains(reads, reads2, reads3);
    }

    @Test
    void shouldReturnErrorOutEWhenIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().outE((String) null)
                .stream()
                .toList());
    }

    @Test
    void shouldReturnInE() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().inE(READS)
                .stream()
                .collect(toList());

        assertEquals(3, edges.size());
        assertThat(edges).contains(reads, reads2, reads3);
    }

    @Test
    void shouldReturnInEWitSupplier() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().inE(() -> READS)
                .stream()
                .collect(toList());

        assertEquals(3, edges.size());
        assertThat(edges).contains(reads, reads2, reads3);
    }


    @Test
    void shouldReturnErrorWhenInEIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().inE((String) null)
                .stream()
                .toList());

    }

    @Test
    void shouldReturnBothE() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().bothE(READS)
                .stream()
                .toList();

        assertEquals(6, edges.size());
    }

    @Test
    void shouldReturnBothEWithSupplier() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().bothE(() -> READS)
                .stream()
                .toList();

        assertEquals(6, edges.size());
    }

    @Test
    void shouldReturnErrorWhenBothEIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().bothE((String) null)
                .stream()
                .toList());
    }


    @Test
    void shouldReturnOut() {
        List<Human> people = tinkerpopTemplate.traversalVertex().outE(READS).outV().<Human>result().collect(toList());
        assertEquals(3, people.size());
        assertThat(people).contains(poliana, otavio, paulo);
    }

    @Test
    void shouldReturnIn() {
        List<Magazine> magazines = tinkerpopTemplate.traversalVertex().outE(READS).inV().<Magazine>result().collect(toList());
        assertEquals(3, magazines.size());
        assertThat(magazines).contains(shack, effectiveJava, license);
    }


    @Test
    void shouldReturnBoth() {
        List<Object> entities = tinkerpopTemplate.traversalVertex().outE(READS).bothV().result().collect(toList());
        assertEquals(6, entities.size());
        assertThat(entities).contains(shack, effectiveJava, license, paulo, otavio, poliana);
    }


    @Test
    void shouldHasPropertyFromAccessor() {

        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has(T.id, "notFound").next();

        assertFalse(edgeEntity.isPresent());
    }


    @Test
    void shouldHasProperty() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has("motivation", "hobby").next();

        assertTrue(edgeEntity.isPresent());
        assertEquals(reads.id(), edgeEntity.get().id());
    }

    @Test
    void shouldHasSupplierProperty() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has(() -> "motivation", "hobby").next();

        assertTrue(edgeEntity.isPresent());
        assertEquals(reads.id(), edgeEntity.get().id());
    }

    @Test
    void shouldHasPropertyPredicate() {

        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has("motivation", P.eq("hobby")).next();

        assertTrue(edgeEntity.isPresent());
        assertEquals(reads.id(), edgeEntity.get().id());
    }


    @Test
    void shouldHasPropertyKeySupplierPredicate() {

        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has(() -> "motivation", P.eq("hobby")).next();

        assertTrue(edgeEntity.isPresent());
        assertEquals(reads.id(), edgeEntity.get().id());
    }


    @Test
    void shouldReturnErrorWhenHasPropertyWhenKeyIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has((String) null, "hobby").next());
    }

    @Test
    void shouldReturnErrorWhenHasPropertyWhenValueIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has("motivation", null).next());
    }

    @Test
    void shouldHasNot() {
        List<EdgeEntity> edgeEntities = tinkerpopTemplate.traversalVertex()
                .outE(READS).hasNot("language")
                .stream()
                .toList();

        assertEquals(2, edgeEntities.size());
    }

    @Test
    void shouldCount() {
        long count = tinkerpopTemplate.traversalVertex().outE(READS).count();
        assertEquals(3L, count);
    }

    @Test
    void shouldReturnZeroWhenCountIsEmpty() {
        long count = tinkerpopTemplate.traversalVertex().outE("WRITES").count();
        assertEquals(0L, count);
    }

    @Test
    void shouldReturnErrorWhenHasNotIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalVertex().outE(READS).hasNot((String) null));
    }


    @Test
    void shouldDefinesLimit() {
        long count = tinkerpopTemplate.traversalEdge().limit(1L).count();
        assertEquals(1L, count);
        assertNotEquals(tinkerpopTemplate.traversalEdge().count(), count);
    }

    @Test
    void shouldDefinesRange() {
        long count = tinkerpopTemplate.traversalEdge().range(1, 3).count();
        assertEquals(2L, count);
        assertNotEquals(tinkerpopTemplate.traversalEdge().count(), count);
    }

    @Test
    void shouldMapValuesAsStream() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("motivation").stream().toList();

        assertFalse(maps.isEmpty());
        assertEquals(3, maps.size());

        List<String> names = new ArrayList<>();

        maps.forEach(m -> names.add(m.get("motivation").toString()));

        assertThat(names).contains("hobby", "love", "job");
    }

    @Test
    void shouldMapValuesAsStreamLimit() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("motivation").next(2).toList();

        assertFalse(maps.isEmpty());
        assertEquals(2, maps.size());
    }


    @Test
    void shouldReturnMapValueAsEmptyStream() {
        Stream<Map<String, Object>> stream = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("noFoundProperty").stream();
        assertTrue(stream.allMatch(m -> Objects.isNull(m.get("noFoundProperty"))));
    }

    @Test
    void shouldReturnNext() {
        Map<String, Object> map = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("motivation").next();

        assertNotNull(map);
        assertFalse(map.isEmpty());
    }


    @Test
    void shouldReturnHas() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake).add("when", "night");
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);


        Optional<EdgeEntity> result = tinkerpopTemplate.traversalEdge().has("when").next();
        assertNotNull(result);

        tinkerpopTemplate.deleteEdge(lion.getId());
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
        Optional<EdgeEntity> result = tinkerpopTemplate.traversalEdge().repeat().has("when").times(2).next();
        assertNotNull(result);
        assertEquals(snake, result.get().incoming());
        assertEquals(lion, result.get().outgoing());
    }

    @Test
    void shouldRepeatUntilTraversal() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake).add("when", "night");
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);

        Optional<EdgeEntity> result = tinkerpopTemplate.traversalEdge().repeat().has("when")
                .until().has("when").next();

        assertTrue(result.isPresent());

        assertEquals(snake, result.get().incoming());
        assertEquals(lion, result.get().outgoing());

    }

    @Test
    void shouldRepeatUntilHasValueTraversal() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake).add("when", "night");
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);

        Optional<EdgeEntity> result = tinkerpopTemplate.traversalEdge().repeat().has("when")
                .until().has("when", "night").next();

        assertTrue(result.isPresent());

        assertEquals(snake, result.get().incoming());
        assertEquals(lion, result.get().outgoing());

    }

    @Test
    void shouldRepeatUntilHasPredicateTraversal() {
        Creature lion = tinkerpopTemplate.insert(new Creature("lion"));
        Creature snake = tinkerpopTemplate.insert(new Creature("snake"));
        Creature mouse = tinkerpopTemplate.insert(new Creature("mouse"));
        Creature plant = tinkerpopTemplate.insert(new Creature("plant"));

        tinkerpopTemplate.edge(lion, "eats", snake).add("when", "night");
        tinkerpopTemplate.edge(snake, "eats", mouse);
        tinkerpopTemplate.edge(mouse, "eats", plant);

        EdgeEntity result = tinkerpopTemplate.traversalEdge().repeat().has("when")
                .until().has("when", new P<Object>((a, b) -> true, "night")).next().orElseThrow();


        SoftAssertions.assertSoftly(softly -> {
            Creature incoming = result.incoming();
            Creature outgoing = result.outgoing();
            softly.assertThat(incoming).isEqualTo(snake);
            softly.assertThat(outgoing).isEqualTo(lion);
        });

    }


    @Test
    void shouldReturnErrorWhenTheOrderIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalEdge().orderBy(null));
    }

    @Test
    void shouldReturnErrorWhenThePropertyDoesNotExist() {
       assertThrows(NoSuchElementException.class, () ->
               tinkerpopTemplate.traversalEdge().orderBy("wrong property").asc().next().get());
    }

    @Test
    void shouldOrderAsc() {
        String property = "motivation";

        List<String> properties = tinkerpopTemplate.traversalEdge()
                .has(property)
                .orderBy(property)
                .asc().stream()
                .map(e -> e.get(property))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(v -> v.get(String.class))
                .collect(toList());

        assertThat(properties).contains("hobby", "job", "love");
    }

    @Test
    void shouldOrderDesc() {
        String property = "motivation";

        List<String> properties = tinkerpopTemplate.traversalEdge()
                .has(property)
                .orderBy(property)
                .desc().stream()
                .map(e -> e.get(property))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(v -> v.get(String.class))
                .collect(toList());

        assertThat(properties).contains("love", "job", "hobby");
    }


    @Test
    void shouldReturnResultAsList() {
        List<EdgeEntity> entities = tinkerpopTemplate.traversalEdge().result()
                .toList();
        assertEquals(3, entities.size());
    }

    @Test
    void shouldReturnErrorWhenThereAreMoreThanOneInGetSingleResult() {
        assertThrows(NonUniqueResultException.class, () -> tinkerpopTemplate.traversalEdge().singleResult());
    }

    @Test
    void shouldReturnOptionalEmptyWhenThereIsNotResultInSingleResult() {
        Optional<EdgeEntity> entity = tinkerpopTemplate.traversalEdge("-1L").singleResult();
        assertFalse(entity.isPresent());
    }

    @Test
    void shouldReturnSingleResult() {
        String name = "Poliana";
        Optional<EdgeEntity> entity = tinkerpopTemplate.traversalEdge(reads.id()).singleResult();
        assertEquals(reads, entity.get());
    }

    @Test
    void shouldReturnErrorWhenPredicateIsNull() {
        assertThrows(NullPointerException.class, () -> tinkerpopTemplate.traversalEdge().filter(null));
    }

    @Test
    void shouldReturnFromPredicate() {
        long count = tinkerpopTemplate.traversalEdge().filter(reads::equals).count();
        assertEquals(1L, count);
    }

    @Test
    void shouldDedup() {

        tinkerpopTemplate.edge(otavio, "knows", paulo);
        tinkerpopTemplate.edge(paulo, "knows", otavio);
        tinkerpopTemplate.edge(otavio, "knows", poliana);
        tinkerpopTemplate.edge(poliana, "knows", otavio);
        tinkerpopTemplate.edge(poliana, "knows", paulo);
        tinkerpopTemplate.edge(paulo, "knows", poliana);

        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .inE("knows").result()
                .collect(Collectors.toList());

        assertEquals(6, edges.size());

        edges = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .inE("knows")
                .dedup()
                .result()
                .toList();

        assertEquals(6, edges.size());
    }
}
