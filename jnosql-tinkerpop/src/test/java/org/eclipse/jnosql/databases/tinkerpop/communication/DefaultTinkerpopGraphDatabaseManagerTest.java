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
package org.eclipse.jnosql.databases.tinkerpop.communication;

import net.datafaker.Faker;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.graph.CommunicationEdge;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.tinkerpop.cdi.TestGraphSupplier;
import org.eclipse.jnosql.mapping.semistructured.MappingQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.eclipse.jnosql.databases.tinkerpop.communication.TinkerpopGraphDatabaseManager.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParameterizedClass
@EnumSource(TestGraphSupplier.class)
public class DefaultTinkerpopGraphDatabaseManagerTest {

    static final String COLLECTION_NAME = "Person";

    private TinkerpopGraphDatabaseManager entityManager;

    private final Faker faker = new Faker();

    @Parameter
    private Supplier<Graph> graphSupplier;

    @BeforeEach
    void setUp() {
        this.entityManager = TinkerpopGraphDatabaseManager.of(graphSupplier.get());
    }

    @BeforeEach
    void beforeEach() {
        delete().from(COLLECTION_NAME).delete(entityManager);
    }

    @AfterEach
    void close() {
        entityManager.close();
    }

    @Test
    void shouldInsertEntity() {
        String name = faker.name().fullName();
        var age = faker.number().randomDigit();
        var entity = CommunicationEntity.of("Person");
        entity.add("name", name);
        entity.add("age", age);
        var communicationEntity = entityManager.insert(entity);
        assertNotNull(communicationEntity);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(communicationEntity.find("name", String.class)).get().isEqualTo(name);
            softly.assertThat(communicationEntity.find("age", int.class)).get().isEqualTo(age);
            softly.assertThat(communicationEntity.find(ID)).isPresent();
        });
    }

    @Test
    void shouldInsertEntities() {
        String name = faker.name().fullName();
        var age = faker.number().randomDigit();
        var entity = CommunicationEntity.of("Person");
        entity.add("name", name);
        entity.add("age", age);

        String name2 = faker.name().fullName();
        var age2 = faker.number().randomDigit();
        var entity2 = CommunicationEntity.of("Person");
        entity2.add("name", name2);
        entity2.add("age", age2);

        var communicationEntities = StreamSupport
                .stream(entityManager.insert(List.of(entity, entity2)).spliterator(), false).toList();

        assertNotNull(communicationEntities);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(communicationEntities).hasSize(2);
            softly.assertThat(communicationEntities.get(0).find("name", String.class)).get().isEqualTo(name);
            softly.assertThat(communicationEntities.get(0).find("age", int.class)).get().isEqualTo(age);
            softly.assertThat(communicationEntities.get(0).find(ID)).isPresent();

            softly.assertThat(communicationEntities.get(1).find("name", String.class)).get().isEqualTo(name2);
            softly.assertThat(communicationEntities.get(1).find("age", int.class)).get().isEqualTo(age2);
            softly.assertThat(communicationEntities.get(1).find(ID)).isPresent();
        });

    }

    @Test
    void shouldInsert() {
        var entity = getEntity();
        var documentEntity = entityManager.insert(entity);
        assertTrue(documentEntity.elements().stream().map(Element::name).anyMatch(s -> s.equals(ID)));
    }

    @Test
    void shouldThrowExceptionWhenInsertWithTTL() {
        var entity = getEntity();
        var ttl = Duration.ofSeconds(10);
        assertThrows(UnsupportedOperationException.class, () -> entityManager.insert(entity, ttl));
    }

    @Test
    void shouldUpdate() {
        var entity = entityManager.insert(getEntity());
        var newField = Elements.of("newField", "10");
        entity.add(newField);
        var updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").orElseThrow());
    }

    @Test
    void shouldRemoveEntity() {
        var documentEntity = entityManager.insert(getEntity());

        Optional<Element> id = documentEntity.find(ID);
        var query = select().from(COLLECTION_NAME)
                .where(ID).eq(id.orElseThrow().get())
                .build();
        var deleteQuery = delete().from(COLLECTION_NAME).where(ID)
                .eq(id.get().get())
                .build();

        entityManager.delete(deleteQuery);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldFindDocument() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find(ID);

        var query = select().from(COLLECTION_NAME)
                .where(ID).eq(id.orElseThrow().get())
                .build();

        var entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocument2() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find(ID);

        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .and("city").eq("Salvador")
                .and(ID).eq(id.orElseThrow().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocument3() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find(ID);
        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .or("city").eq("Salvador")
                .and(ID).eq(id.orElseThrow().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocumentGreaterThan() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        assertThat(entitiesFound).isNotIn(entities.get(0));
    }

    @Test
    void shouldFindDocumentGreaterEqualsThan() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gte(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        assertThat(entitiesFound).isNotIn(entities.get(0));
    }

    @Test
    void shouldFindDocumentLesserThan() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false)
                .toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").lt(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(1, entitiesFound.size());
        assertThat(entitiesFound).contains(entities.get(0));
    }

    @Test
    void shouldFindDocumentLesserEqualsThan() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("age").lte(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        assertThat(entitiesFound).contains(entities.get(0), entities.get(2));
    }


    @Test
    void shouldFindDocumentIn() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("location").in(asList("BR", "US"))
                .and("type").eq("V")
                .build();

        Assertions.assertThat(entityManager.select(query).toList()).containsAll(entities);
    }

    @Test
    void shouldFindDocumentBetween() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("age").between(22, 25)
                .build();


        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(2);
            softly.assertThat(entities).extracting(e -> e.find("age").orElseThrow().get(Integer.class))
                    .contains(22, 23);
            softly.assertThat(entities).extracting(e -> e.find("name").orElseThrow().get(String.class))
                    .contains("Luna", "Lucas");
        });


    }

    @Test
    void shouldFindDocumentStart() {
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .skip(1L)
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(1, entitiesFound.size());
        assertThat(entitiesFound).isNotIn(entities.get(0));

        query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .skip(2L)
                .build();

        entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertTrue(entitiesFound.isEmpty());

    }

    @Test
    void shouldFindDocumentLimit() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .limit(1L)
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(1, entitiesFound.size());
        assertThat(entitiesFound).isNotIn(entities.get(0));

        query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .limit(2L)
                .build();

        entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());

    }

    @Test
    void shouldFindDocumentSort() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").asc()
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        List<Integer> ages = entitiesFound.stream()
                .map(e -> e.find("age").orElseThrow().get(Integer.class))
                .collect(Collectors.toList());

        assertThat(ages).contains(23, 25);

        query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").desc()
                .build();

        entitiesFound = entityManager.select(query).toList();
        ages = entitiesFound.stream()
                .map(e -> e.find("age").orElseThrow().get(Integer.class))
                .collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        assertThat(ages).contains(25, 23);

    }

    @Test
    void shouldFindAll() {
        entityManager.insert(getEntity());
        SelectQuery query = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldDeleteAll() {
        entityManager.insert(getEntity());
        SelectQuery query = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        entities = entityManager.select(query).toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldFindAllByFields() {
        entityManager.insert(getEntity());
        SelectQuery query = select("name").from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        final CommunicationEntity entity = entities.get(0);
        assertEquals(3, entity.size());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entity.find("name")).isPresent();
            softly.assertThat(entity.find(ID)).isPresent();
            softly.assertThat(entity.find("city")).isPresent();
        });
    }

    @Test
    void shouldCountByCollectionName() {

        List<CommunicationEntity> entitiesWithValues = getEntitiesWithValues();

        entityManager.insert(entitiesWithValues);

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").asc()
                .build();


        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entityManager.count(COLLECTION_NAME))
                    .isEqualTo(entitiesWithValues.size());
            softly.assertThatThrownBy(()-> entityManager.count((String)null))
                    .as("should throw exception when collection name is null")
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void shouldCountBySelectQuery() {

        List<CommunicationEntity> entitiesWithValues = getEntitiesWithValues();

        entityManager.insert(entitiesWithValues);

        var query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").asc()
                .build();


        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entityManager.count(query))
                    .isEqualTo(2);
            softly.assertThatThrownBy(()-> entityManager.count((SelectQuery) null))
                    .as("should throw exception when select query is null")
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void shouldCreateEdge() {
        var person1 = entityManager.insert(getEntity());
        var person2 = entityManager.insert(getEntity());

        String label = "friend";
        Map<String, Object> properties = Map.of("since", 2023);

        var edge = entityManager.edge(person1, label, person2, properties);

        assertNotNull(edge);
        assertEquals(label, edge.label());
        assertEquals(person1.find(ID).orElseThrow().get(), edge.source().find(ID).orElseThrow().get());
        assertEquals(person2.find(ID).orElseThrow().get(), edge.target().find(ID).orElseThrow().get());
        assertEquals(properties, edge.properties());
    }

    @Test
    void shouldRemoveEdge() {
        var person1 = entityManager.insert(getEntity());
        var person2 = entityManager.insert(getEntity());

        CommunicationEdge communicationEdge = entityManager.edge(person1, "friend", person2, Map.of());

        entityManager.remove(person1, "friend", person2);

        var edges = entityManager.findEdgeById(communicationEdge.id());

        assertThat(edges).isEmpty();
    }

    @Test
    void shouldDeleteEdgeById() {
        var person1 = entityManager.insert(getEntity());
        var person2 = entityManager.insert(getEntity());

        var edge = entityManager.edge(person1, "friend", person2, Map.of());

        entityManager.deleteEdge(edge.id());

        Optional<CommunicationEdge> foundEdge = entityManager.findEdgeById(edge.id());
        assertFalse(foundEdge.isPresent());
    }

    @Test
    void shouldFindEdgeById() {
        var person1 = entityManager.insert(getEntity());
        var person2 = entityManager.insert(getEntity());

        var edge = entityManager.edge(person1, "friend", person2, Map.of("since", 2023));

        Optional<CommunicationEdge> foundEdge = entityManager.findEdgeById(edge.id());

        assertTrue(foundEdge.isPresent());
        assertEquals(edge.id(), foundEdge.get().id());
        assertEquals(edge.label(), foundEdge.get().label());
        assertEquals(edge.source().find(ID).orElseThrow().get(), foundEdge.get().source().find(ID).orElseThrow().get());
        assertEquals(edge.target().find(ID).orElseThrow().get(), foundEdge.get().target().find(ID).orElseThrow().get());
    }

    @Test
    void shouldFindContains() {
        var entity = getEntity();

        entityManager.insert(entity);
        var query = new MappingQuery(Collections.emptyList(), 0L, 0L, CriteriaCondition.contains(Element.of("name",
                "lia")), COLLECTION_NAME, Collections.emptyList());

        var result = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).find("name").orElseThrow().get(String.class)).isEqualTo("Poliana");
        });
    }

    @Test
    void shouldStartsWith() {
        var entity = getEntity();

        entityManager.insert(entity);
        var query = new MappingQuery(Collections.emptyList(), 0L, 0L, CriteriaCondition.startsWith(Element.of("name",
                "Pol")), COLLECTION_NAME, Collections.emptyList());

        var result = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).find("name").orElseThrow().get(String.class)).isEqualTo("Poliana");
        });
    }

    @Test
    void shouldEndsWith() {
        var entity = getEntity();

        entityManager.insert(entity);
        var query = new MappingQuery(Collections.emptyList(), 0L, 0L, CriteriaCondition.endsWith(Element.of("name",
                "ana")), COLLECTION_NAME, Collections.emptyList());

        var result = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).find("name").orElseThrow().get(String.class)).isEqualTo("Poliana");
        });
    }

    @Test
    void shouldFindDocumentLike() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("name").like("Lu%")
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        assertThat(entitiesFound).contains(entities.get(0), entities.get(2));
    }

    private CommunicationEntity getEntity() {
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_NAME);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        List<Element> documents = Elements.of(map);
        documents.forEach(entity::add);
        return entity;
    }

    private List<CommunicationEntity> getEntitiesWithValues() {
        CommunicationEntity lucas = CommunicationEntity.of(COLLECTION_NAME);
        lucas.add(Element.of("name", "Lucas"));
        lucas.add(Element.of("age", 22));
        lucas.add(Element.of("location", "BR"));
        lucas.add(Element.of("type", "V"));

        CommunicationEntity luna = CommunicationEntity.of(COLLECTION_NAME);
        luna.add(Element.of("name", "Luna"));
        luna.add(Element.of("age", 23));
        luna.add(Element.of("location", "US"));
        luna.add(Element.of("type", "V"));

        CommunicationEntity otavio = CommunicationEntity.of(COLLECTION_NAME);
        otavio.add(Element.of("name", "Otavio"));
        otavio.add(Element.of("age", 25));
        otavio.add(Element.of("location", "BR"));
        otavio.add(Element.of("type", "V"));


        return asList(lucas, otavio, luna);
    }
}
