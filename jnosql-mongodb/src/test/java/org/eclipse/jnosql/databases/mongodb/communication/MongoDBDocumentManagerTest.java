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

package org.eclipse.jnosql.databases.mongodb.communication;

import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.mongodb.communication.type.Money;
import org.eclipse.jnosql.mapping.semistructured.MappingQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class MongoDBDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";
    private static DatabaseManager entityManager;

    @BeforeAll
    public static void setUp() {
        entityManager = DocumentDatabase.INSTANCE.get("database");
    }

    @BeforeEach
    void beforeEach() {
        delete().from(COLLECTION_NAME).delete(entityManager);
    }

    @Test
    void shouldInsert() {
        var entity = getEntity();
        var documentEntity = entityManager.insert(entity);
        assertTrue(documentEntity.elements().stream().map(Element::name).anyMatch(s -> s.equals("_id")));
    }

    @Test
    void shouldInsertAndIgnoreIdNull() {
        var entity = getEntity();
        entity.add("_id", null);
        var documentEntity = entityManager.insert(entity);
        var id = documentEntity.find("_id").orElseThrow();
        entityManager.delete(DeleteQuery.delete().from(COLLECTION_NAME).where("_id").eq(id.get()).build());
    }

    @Test
    void shouldThrowExceptionWhenInsertWithTTL() {
        var entity = getEntity();
        var ttl = Duration.ofSeconds(10);
        assertThrows(UnsupportedOperationException.class, () -> entityManager.insert(entity, ttl));
    }

    @Test
    void shouldUpdateSingleEntity() {
        var documentEntity = entityManager.insert(getEntity());
        var newField = Elements.of("newField", "10");
        documentEntity.add(newField);
        documentEntity = entityManager.update(documentEntity);
        var id = documentEntity.find(MongoDBUtils.ID_FIELD).orElseThrow();
        var entityRef = entityManager.singleResult(SelectQuery.select().from(documentEntity.name()).where(id.name()).eq(id.get()).build());

        assertSoftly(softly -> {
            softly.assertThat(entityRef).isPresent();
            softly.assertThat(entityRef)
                    .get()
                    .satisfies(updatedEntity -> {
                        softly.assertThat(updatedEntity.find("newField"))
                                .as("newField must be present")
                                .isPresent()
                                .get()
                                .extracting(Element::get)
                                .as("newField value is not correct")
                                .isEqualTo("10");

                    });
        });
    }

    @Test
    void shouldUpdateMultipleEntities() {

        List<CommunicationEntity> entitiesWithValues = getEntitiesWithValues();
        Iterable<CommunicationEntity> insertedEntities = entityManager.insert(entitiesWithValues);

        var entitiesToUpdate =
                StreamSupport.stream(insertedEntities.spliterator(),false)
                        .map(entity->{
                            var _id = entity.find(MongoDBUtils.ID_FIELD).orElseThrow();
                            entity.add(Elements.of("newField", _id.get()));
                            return entity;
                        }).toList();


        var updatedEntities = entityManager.update(entitiesToUpdate);

        for (CommunicationEntity updatedEntity : updatedEntities) {
            var _id = updatedEntity.find(MongoDBUtils.ID_FIELD).orElseThrow();
            var entityRef2 = entityManager.singleResult(SelectQuery.select().from(updatedEntity.name())
                    .where(_id.name()).eq(_id.get()).build());
            assertSoftly(softly -> {
                softly.assertThat(entityRef2).isPresent();
                softly.assertThat(entityRef2)
                        .get()
                        .satisfies(e -> {
                            softly.assertThat(e.find("newField"))
                                    .as("newField must be present")
                                    .isPresent()
                                    .get()
                                    .extracting(Element::get)
                                    .as("newField value is not correct")
                                    .isEqualTo(_id.get());
                        });
            });
        }

    }

    @Test
    void shouldRemoveEntity() {
        var documentEntity = entityManager.insert(getEntity());

        Optional<Element> id = documentEntity.find("_id");
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get().get())
                .build();
        var deleteQuery = delete().from(COLLECTION_NAME).where("_id")
                .eq(id.get().get())
                .build();

        entityManager.delete(deleteQuery);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldFindDocument() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find("_id");

        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get().get())
                .build();

        var entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocument2() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find("_id");

        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .and("city").eq("Salvador").and("_id").eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocument3() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find("_id");
        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .or("city").eq("Salvador")
                .and(id.get().name()).eq(id.get().get())
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

    @Test
    void shouldFindDocumentIn() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).collect(Collectors.toList());

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("location").in(asList("BR", "US"))
                .and("type").eq("V")
                .build();

        assertEquals(entities, entityManager.select(query).collect(Collectors.toList()));
    }

    @Test
    void shouldFindBetween() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());

        var query = select().from(COLLECTION_NAME)
                .where("age").between(22, 23)
                .build();

        var result = entityManager.select(query).toList();

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result).map(e -> e.find("age").orElseThrow().get(Integer.class)).contains(22, 23);
            softly.assertThat(result).map(e -> e.find("age").orElseThrow().get(Integer.class)).doesNotContain(25);
        });
    }

    @Test
    void shouldFindBetween2() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());

        var query = select().from(COLLECTION_NAME)
                .where("age").between(22, 23)
                .and("type").eq("V")
                .build();

        var result = entityManager.select(query).toList();

        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result).map(e -> e.find("age").orElseThrow().get(Integer.class)).contains(22, 23);
            softly.assertThat(result).map(e -> e.find("age").orElseThrow().get(Integer.class)).doesNotContain(25);
        });
    }


    @Test
    void shouldFindDocumentStart() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
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
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).
                toList();

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").asc()
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        List<Integer> ages = entitiesFound.stream()
                .map(e -> e.find("age").get().get(Integer.class))
                .collect(Collectors.toList());

        assertThat(ages).contains(23, 25);

        query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").desc()
                .build();

        entitiesFound = entityManager.select(query).toList();
        ages = entitiesFound.stream()
                .map(e -> e.find("age").get().get(Integer.class))
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
        assertEquals(2, entity.size());
        assertTrue(entity.find("name").isPresent());
        assertTrue(entity.find("_id").isPresent());
        assertFalse(entity.find("city").isPresent());
    }


    @Test
    void shouldSaveSubDocument() {
        CommunicationEntity entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        CommunicationEntity entitySaved = entityManager.insert(entity);
        Element id = entitySaved.find("_id").get();
        SelectQuery query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get())
                .build();

        CommunicationEntity entityFound = entityManager.select(query).toList().get(0);
        Element subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"));
    }

    @Test
    void shouldNotSupportTTLOnSaveAndUpdate() {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(() ->
                            entityManager.insert(getEntity(), Duration.ofSeconds(10)))
                    .isInstanceOf(UnsupportedOperationException.class);
            softly.assertThatThrownBy(() ->
                            entityManager.insert(getEntitiesWithValues(), Duration.ofSeconds(10)))
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    @Test
    void shouldSaveSubDocument2() {
        CommunicationEntity entity = getEntity();
        entity.add(Element.of("phones", asList(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231"))));
        CommunicationEntity entitySaved = entityManager.insert(entity);
        Element id = entitySaved.find("_id").get();

        SelectQuery query = select().from(COLLECTION_NAME)
                .where(id.name()).eq(id.get())
                .build();
        var entityFound = entityManager.select(query).toList().get(0);
        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"),
                Element.of("mobile2", "1231231"));
    }

    @Test
    void shouldCreateEntityByteArray() {
        byte[] contents = {1, 2, 3, 4, 5, 6};

        var entity = CommunicationEntity.of("download");
        long id = ThreadLocalRandom.current().nextLong();
        entity.add("_id", id);
        entity.add("contents", contents);

        entityManager.insert(entity);

        List<CommunicationEntity> entities = entityManager.select(select().from("download")
                .where("_id").eq(id).build()).toList();

        assertEquals(1, entities.size());
        CommunicationEntity documentEntity = entities.get(0);
        assertEquals(id, documentEntity.find("_id").get().get());

        assertArrayEquals(contents, (byte[]) documentEntity.find("contents").get().get());

    }

    @Test
    void shouldCreateDate() {
        Date date = new Date();
        LocalDate now = LocalDate.now();

        CommunicationEntity entity = CommunicationEntity.of("download");
        long id = ThreadLocalRandom.current().nextLong();
        entity.add("_id", id);
        entity.add("date", date);
        entity.add("now", now);

        entityManager.insert(entity);

        List<CommunicationEntity> entities = entityManager.select(select().from("download")
                .where("_id").eq(id).build()).toList();

        assertEquals(1, entities.size());
        CommunicationEntity documentEntity = entities.get(0);
        assertEquals(id, documentEntity.find("_id").get().get());
        assertEquals(date, documentEntity.find("date").get().get(Date.class));
        assertEquals(now, documentEntity.find("date").get().get(LocalDate.class));


    }

    @Test
    void shouldConvertFromListDocumentList() {
        CommunicationEntity entity = createDocumentList();
        assertDoesNotThrow(() -> entityManager.insert(entity));
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldRetrieveListDocumentList() {
        CommunicationEntity entity = entityManager.insert(createDocumentList());
        Element key = entity.find("_id").get();
        var query = select().from("AppointmentBook")
                .where(key.name())
                .eq(key.get()).build();

        var documentEntity = entityManager.singleResult(query).get();
        assertNotNull(documentEntity);

        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").get().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldCount() {
        entityManager.insert(getEntity());
        assertTrue(entityManager.count(COLLECTION_NAME) > 0);
    }

    @Test
    void shouldCountBySelectQuery() {
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gte(23)
                .and("type").eq("V")
                .build();

        assertThat(entityManager.count(query))
                .isEqualTo(2);
    }

    @Test
    void shouldCustomTypeWork() {
        var entity = getEntity();
        Currency currency = Currency.getInstance("USD");
        Money money = Money.of(currency, BigDecimal.valueOf(10D));
        entity.add("money", money);
        var documentEntity = entityManager.insert(entity);
        Element id = documentEntity.find("_id").get();
        SelectQuery query = SelectQuery.select().from(documentEntity.name())
                .where(id.name()).eq(id.get()).build();

        CommunicationEntity result = entityManager.singleResult(query).get();
        assertEquals(money, result.find("money").get().get(Money.class));

    }

    @Test
    void shouldSaveMap() {
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_NAME);
        String id = UUID.randomUUID().toString();
        entity.add("properties", Collections.singletonMap("hallo", "Welt"));
        entity.add("scope", "xxx");
        entity.add("_id", id);
        entityManager.insert(entity);
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id).and("scope").eq("xxx").build();
        final Optional<CommunicationEntity> optional = entityManager.select(query).findFirst();
        Assertions.assertTrue(optional.isPresent());
        CommunicationEntity documentEntity = optional.get();
        Element properties = documentEntity.find("properties").get();
        Map<String, Object> map = properties.get(new TypeReference<>() {
        });
        Assertions.assertNotNull(map);
    }

    @Test
    void shouldInsertNull() {
        CommunicationEntity entity = getEntity();
        entity.add(Element.of("name", null));
        CommunicationEntity documentEntity = entityManager.insert(entity);
        Optional<Element> name = documentEntity.find("name");
        assertSoftly(soft -> {
            soft.assertThat(name).isPresent();
            soft.assertThat(name).get().extracting(Element::name).isEqualTo("name");
            soft.assertThat(name).get().extracting(Element::get).isNull();
        });
    }

    @Test
    void shouldUpdateNull() {
        var entity = entityManager.insert(getEntity());
        entity.add(Element.of("name", null));
        var documentEntity = entityManager.update(entity);
        Optional<Element> name = documentEntity.find("name");
        assertSoftly(soft -> {
            soft.assertThat(name).isPresent();
            soft.assertThat(name).get().extracting(Element::name).isEqualTo("name");
            soft.assertThat(name).get().extracting(Element::get).isNull();
        });
    }

    @Test
    void shouldInsertUUID() {
        var entity = getEntity();
        entity.add("uuid", UUID.randomUUID());
        var documentEntity = entityManager.insert(entity);
        Optional<Element> uuid = documentEntity.find("uuid");
        assertSoftly(soft -> {
            soft.assertThat(uuid).isPresent();
            Element element = uuid.orElseThrow();
            soft.assertThat(element.name()).isEqualTo("uuid");
            soft.assertThat(element.get(UUID.class)).isInstanceOf(UUID.class);
        });
    }


    @Test
    void shouldFindContains() {
        var entity = getEntity();

        entityManager.insert(entity);
        var query = new MappingQuery(Collections.emptyList(), 0L, 0L, CriteriaCondition.contains(Element.of("name",
                "lia")), COLLECTION_NAME, Collections.emptyList());

        var result = entityManager.select(query).toList();
        assertSoftly(softly -> {
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
        assertSoftly(softly -> {
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
        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).find("name").orElseThrow().get(String.class)).isEqualTo("Poliana");
        });
    }


    private CommunicationEntity createDocumentList() {
        CommunicationEntity entity = CommunicationEntity.of("AppointmentBook");
        entity.add(Element.of("_id", new Random().nextInt()));
        List<List<Element>> documents = new ArrayList<>();

        documents.add(asList(Element.of("name", "Ada"), Element.of("type", ContactType.EMAIL),
                Element.of("information", "ada@lovelace.com")));

        documents.add(asList(Element.of("name", "Ada"), Element.of("type", ContactType.MOBILE),
                Element.of("information", "11 1231231 123")));

        documents.add(asList(Element.of("name", "Ada"), Element.of("type", ContactType.PHONE),
                Element.of("information", "phone")));

        entity.add(Element.of("contacts", documents));
        return entity;
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
        var lucas = CommunicationEntity.of(COLLECTION_NAME);
        lucas.add(Element.of("name", "Lucas"));
        lucas.add(Element.of("age", 22));
        lucas.add(Element.of("location", "BR"));
        lucas.add(Element.of("type", "V"));

        var luna = CommunicationEntity.of(COLLECTION_NAME);
        luna.add(Element.of("name", "Luna"));
        luna.add(Element.of("age", 23));
        luna.add(Element.of("location", "US"));
        luna.add(Element.of("type", "V"));

        var otavio = CommunicationEntity.of(COLLECTION_NAME);
        otavio.add(Element.of("name", "Otavio"));
        otavio.add(Element.of("age", 25));
        otavio.add(Element.of("location", "BR"));
        otavio.add(Element.of("type", "V"));


        return asList(lucas, otavio, luna);
    }

}
