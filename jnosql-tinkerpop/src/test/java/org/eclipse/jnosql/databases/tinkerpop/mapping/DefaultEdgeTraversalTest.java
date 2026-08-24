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
import org.assertj.core.api.SoftAssertions;
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
abstract class DefaultEdgeTraversalTest extends AbstractTraversalTest {

    @AddPackages(ArangoDBGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class ArangoDBTest extends DefaultEdgeTraversalTest {
    }

    @AddPackages(Neo4jGraphProducer.class)
    @EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
    static class Neo4jTest extends DefaultEdgeTraversalTest {
    }

    @AddPackages(TinkerGraphProducer.class)
    static class TinkerGraphTest extends DefaultEdgeTraversalTest {
    }

    @Test
    void shouldReturnErrorWhenEdgeIdIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalEdge(null));
    }

    @Test
    void shouldReturnEdgeId() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalEdge(reads.id())
                .next();

        assertThat(edgeEntity.isPresent()).isTrue();
        assertThat(edgeEntity.get().id()).isEqualTo(reads.id());
    }

    @Test
    void shouldReturnOutE() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().outE(READS)
                .stream()
                .collect(toList());

        assertThat(edges.size()).isEqualTo(3);
        assertThat(edges).contains(reads, reads2, reads3);
    }

    @Test
    void shouldReturnOutEWithSupplier() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().outE(() -> READS)
                .stream()
                .collect(toList());

        assertThat(edges.size()).isEqualTo(3);
        assertThat(edges).contains(reads, reads2, reads3);
    }

    @Test
    void shouldReturnErrorOutEWhenIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().outE((String) null)
                .stream()
                .toList());
    }

    @Test
    void shouldReturnInE() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().inE(READS)
                .stream()
                .collect(toList());

        assertThat(edges.size()).isEqualTo(3);
        assertThat(edges).contains(reads, reads2, reads3);
    }

    @Test
    void shouldReturnInEWitSupplier() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().inE(() -> READS)
                .stream()
                .collect(toList());

        assertThat(edges.size()).isEqualTo(3);
        assertThat(edges).contains(reads, reads2, reads3);
    }


    @Test
    void shouldReturnErrorWhenInEIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().inE((String) null)
                .stream()
                .toList());

    }

    @Test
    void shouldReturnBothE() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().bothE(READS)
                .stream()
                .toList();

        assertThat(edges.size()).isEqualTo(6);
    }

    @Test
    void shouldReturnBothEWithSupplier() {
        List<EdgeEntity> edges = tinkerpopTemplate.traversalVertex().bothE(() -> READS)
                .stream()
                .toList();

        assertThat(edges.size()).isEqualTo(6);
    }

    @Test
    void shouldReturnErrorWhenBothEIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().bothE((String) null)
                .stream()
                .toList());
    }


    @Test
    void shouldReturnOut() {
        List<Human> people = tinkerpopTemplate.traversalVertex().outE(READS).outV().<Human>result().collect(toList());
        assertThat(people.size()).isEqualTo(3);
        assertThat(people).contains(poliana, otavio, paulo);
    }

    @Test
    void shouldReturnIn() {
        List<Magazine> magazines = tinkerpopTemplate.traversalVertex().outE(READS).inV().<Magazine>result().collect(toList());
        assertThat(magazines.size()).isEqualTo(3);
        assertThat(magazines).contains(shack, effectiveJava, license);
    }


    @Test
    void shouldReturnBoth() {
        List<Object> entities = tinkerpopTemplate.traversalVertex().outE(READS).bothV().result().collect(toList());
        assertThat(entities.size()).isEqualTo(6);
        assertThat(entities).contains(shack, effectiveJava, license, paulo, otavio, poliana);
    }


    @Test
    void shouldHasPropertyFromAccessor() {

        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has(T.id, "notFound").next();

        assertThat(edgeEntity.isPresent()).isFalse();
    }


    @Test
    void shouldHasProperty() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has("motivation", "hobby").next();

        assertThat(edgeEntity.isPresent()).isTrue();
        assertThat(edgeEntity.get().id()).isEqualTo(reads.id());
    }

    @Test
    void shouldHasSupplierProperty() {
        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has(() -> "motivation", "hobby").next();

        assertThat(edgeEntity.isPresent()).isTrue();
        assertThat(edgeEntity.get().id()).isEqualTo(reads.id());
    }

    @Test
    void shouldHasPropertyPredicate() {

        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has("motivation", P.eq("hobby")).next();

        assertThat(edgeEntity.isPresent()).isTrue();
        assertThat(edgeEntity.get().id()).isEqualTo(reads.id());
    }


    @Test
    void shouldHasPropertyKeySupplierPredicate() {

        Optional<EdgeEntity> edgeEntity = tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has(() -> "motivation", P.eq("hobby")).next();

        assertThat(edgeEntity.isPresent()).isTrue();
        assertThat(edgeEntity.get().id()).isEqualTo(reads.id());
    }


    @Test
    void shouldReturnErrorWhenHasPropertyWhenKeyIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has((String) null, "hobby").next());
    }

    @Test
    void shouldReturnErrorWhenHasPropertyWhenValueIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex()
                .outE(READS)
                .has("motivation", null).next());
    }

    @Test
    void shouldHasNot() {
        List<EdgeEntity> edgeEntities = tinkerpopTemplate.traversalVertex()
                .outE(READS).hasNot("language")
                .stream()
                .toList();

        assertThat(edgeEntities.size()).isEqualTo(2);
    }

    @Test
    void shouldCount() {
        long count = tinkerpopTemplate.traversalVertex().outE(READS).count();
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void shouldReturnZeroWhenCountIsEmpty() {
        long count = tinkerpopTemplate.traversalVertex().outE("WRITES").count();
        assertThat(count).isEqualTo(0L);
    }

    @Test
    void shouldReturnErrorWhenHasNotIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalVertex().outE(READS).hasNot((String) null));
    }


    @Test
    void shouldDefinesLimit() {
        long count = tinkerpopTemplate.traversalEdge().limit(1L).count();
        assertThat(count).isEqualTo(1L);
        assertThat(count).isNotEqualTo(tinkerpopTemplate.traversalEdge().count());
    }

    @Test
    void shouldDefinesRange() {
        long count = tinkerpopTemplate.traversalEdge().range(1, 3).count();
        assertThat(count).isEqualTo(2L);
        assertThat(count).isNotEqualTo(tinkerpopTemplate.traversalEdge().count());
    }

    @Test
    void shouldMapValuesAsStream() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("motivation").stream().toList();

        assertThat(maps.isEmpty()).isFalse();
        assertThat(maps.size()).isEqualTo(3);

        List<String> names = new ArrayList<>();

        maps.forEach(m -> names.add(m.get("motivation").toString()));

        assertThat(names).contains("hobby", "love", "job");
    }

    @Test
    void shouldMapValuesAsStreamLimit() {
        List<Map<String, Object>> maps = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("motivation").next(2).toList();

        assertThat(maps.isEmpty()).isFalse();
        assertThat(maps.size()).isEqualTo(2);
    }


    @Test
    void shouldReturnMapValueAsEmptyStream() {
        Stream<Map<String, Object>> stream = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("noFoundProperty").stream();
        assertThat(stream.allMatch(m -> Objects.isNull(m.get("noFoundProperty")))).isTrue();
    }

    @Test
    void shouldReturnNext() {
        Map<String, Object> map = tinkerpopTemplate.traversalVertex().inE("reads")
                .valueMap("motivation").next();

        assertThat(map).isNotNull();
        assertThat(map.isEmpty()).isFalse();
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
        assertThat(result).isNotNull();

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
        assertThat(result).isNotNull();
        assertThat((Object) result.get().incoming()).isEqualTo(snake);
        assertThat((Object) result.get().outgoing()).isEqualTo(lion);
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

        assertThat(result.isPresent()).isTrue();

        assertThat((Object) result.get().incoming()).isEqualTo(snake);
        assertThat((Object) result.get().outgoing()).isEqualTo(lion);

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

        assertThat(result.isPresent()).isTrue();

        assertThat((Object) result.get().incoming()).isEqualTo(snake);
        assertThat((Object) result.get().outgoing()).isEqualTo(lion);

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
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalEdge().orderBy(null));
    }

    @Test
    void shouldReturnErrorWhenThePropertyDoesNotExist() {
       assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
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
        assertThat(entities.size()).isEqualTo(3);
    }

    @Test
    void shouldReturnErrorWhenThereAreMoreThanOneInGetSingleResult() {
        assertThatExceptionOfType(NonUniqueResultException.class).isThrownBy(() -> tinkerpopTemplate.traversalEdge().singleResult());
    }

    @Test
    void shouldReturnOptionalEmptyWhenThereIsNotResultInSingleResult() {
        Optional<EdgeEntity> entity = tinkerpopTemplate.traversalEdge("-1").singleResult();
        assertThat(entity.isPresent()).isFalse();
    }

    @Test
    void shouldReturnSingleResult() {
        String name = "Poliana";
        Optional<EdgeEntity> entity = tinkerpopTemplate.traversalEdge(reads.id()).singleResult();
        assertThat(entity.get()).isEqualTo(reads);
    }

    @Test
    void shouldReturnErrorWhenPredicateIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> tinkerpopTemplate.traversalEdge().filter(null));
    }

    @Test
    void shouldReturnFromPredicate() {
        long count = tinkerpopTemplate.traversalEdge().filter(reads::equals).count();
        assertThat(count).isEqualTo(1L);
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

        assertThat(edges.size()).isEqualTo(6);

        edges = tinkerpopTemplate.traversalVertex()
                .hasLabel(Human.class)
                .inE("knows")
                .dedup()
                .result()
                .toList();

        assertThat(edges.size()).isEqualTo(6);
    }
}
