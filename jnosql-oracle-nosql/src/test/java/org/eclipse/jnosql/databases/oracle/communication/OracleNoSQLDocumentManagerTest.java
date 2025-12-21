/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.mapping.semistructured.MappingQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
class OracleNoSQLDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";
    private static OracleNoSQLDocumentManager entityManager;

    @BeforeAll
    public static void setUp() {
        entityManager = (OracleNoSQLDocumentManager) Database.INSTANCE.managerFactory().apply("database");
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
    void shouldThrowExceptionWhenInsertWithTTL() {
        var entity = getEntity();
        var ttl = Duration.ofSeconds(10);
        assertThrows(UnsupportedOperationException.class, () -> entityManager.insert(entity, ttl));
    }

    @Test
    void shouldUpdate() {
        var entity = getEntity();
        var documentEntity = entityManager.insert(entity);
        var newField = Elements.of("newField", "10");
        entity.add(newField);
        var updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").orElseThrow());
    }

    @Test
    void shouldRemoveEntity() {
        var documentEntity = entityManager.insert(getEntity());

        Optional<Element> id = documentEntity.find("_id");
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.orElseThrow().get())
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
        var id = entity.find("_id");

        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.orElseThrow().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocument2() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find("_id");

        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .and("city").eq("Salvador")
                .and("_id").eq(id.orElseThrow().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocument3() {
        var entity = entityManager.insert(getEntity());
        var id = entity.find("_id");
        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .or("city").eq("Salvador")
                .and(id.orElseThrow().name()).eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldFindDocumentGreaterThan() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
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
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
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
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false)
                .toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").lt(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();

        assertSoftly(soft -> {
            soft.assertThat(entitiesFound).hasSize(1);

            List<String> namesFound = entitiesFound.stream()
                    .flatMap(d -> d.find("name").stream())
                    .map(d -> d.get(String.class))
                    .toList();
            soft.assertThat(namesFound).contains("Lucas");
        });
    }

    @Test
    void shouldFindDocumentLesserEqualsThan() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());

        var query = select().from(COLLECTION_NAME)
                .where("age").lte(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
        assertEquals(2, entitiesFound.size());
    }

    @Test
    void shouldFindDocumentIn() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("location").in(asList("BR", "US"))
                .and("type").eq("V")
                .build();

        assertSoftly(soft -> {
            List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
            soft.assertThat(entitiesFound).hasSize(entities.size());
            List<String> namesFound = entitiesFound.stream()
                    .flatMap(d -> d.find("name").stream())
                    .map(d -> d.get(String.class))
                    .toList();
            List<String> names = entities.stream()
                    .flatMap(d -> d.find("name").stream())
                    .map(d -> d.get(String.class))
                    .toList();
            soft.assertThat(namesFound).containsAll(names);
        });

    }

    @Test
    void shouldFindDocumentStart() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
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
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
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
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());

        var query = select().from(COLLECTION_NAME)
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
        var query = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldDeleteAll() {
        entityManager.insert(getEntity());
        var query = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        entities = entityManager.select(query).toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldFindAllByFields() {
        entityManager.insert(getEntity());
        var query = select("name").from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        final CommunicationEntity entity = entities.get(0);
        assertSoftly(soft -> {
            soft.assertThat(entity.find("name")).isPresent();
            soft.assertThat(entity.find("_id")).isPresent();
            soft.assertThat(entity.find("city")).isNotPresent();
        });
    }


    @Test
    void shouldSaveSubDocument() {
        var entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        var entitySaved = entityManager.insert(entity);
        var id = entitySaved.find("_id").orElseThrow();
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get())
                .build();

        var entityFound = entityManager.select(query).toList().get(0);
        var subDocument = entityFound.find("phones").orElseThrow();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"));
    }

    @Test
    void shouldSaveSubDocument2() {
        var entity = getEntity();
        entity.add(Element.of("phones", asList(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231"))));
        var entitySaved = entityManager.insert(entity);
        var id = entitySaved.find("_id").orElseThrow();

        var query = select().from(COLLECTION_NAME)
                .where(id.name()).eq(id.get())
                .build();
        var entityFound = entityManager.select(query).toList().get(0);
        var subDocument = entityFound.find("phones").orElseThrow();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"),
                Element.of("mobile2", "1231231"));
    }

    @Test
    @Disabled
    void shouldCreateEntityByteArray() {
        byte[] contents = {1, 2, 3, 4, 5, 6};

        CommunicationEntity entity = CommunicationEntity.of("download");
        long id = ThreadLocalRandom.current().nextLong();
        entity.add("_id", id);
        entity.add("contents", contents);

        entityManager.insert(entity);

        List<CommunicationEntity> entities = entityManager.select(select().from("download")
                .where("_id").eq(id).build()).toList();

        assertEquals(1, entities.size());
        var documentEntity = entities.get(0);
        assertEquals(id, documentEntity.find("_id").orElseThrow().get(Long.class));

        assertArrayEquals(contents, documentEntity.find("contents").orElseThrow().get(byte[].class));

    }

    @Test
    void shouldCreateDate() {
        LocalDate now = LocalDate.now();

        var entity = CommunicationEntity.of("download");
        long id = ThreadLocalRandom.current().nextLong();
        entity.add("_id", id);
        entity.add("now", now);

        entityManager.insert(entity);

        List<CommunicationEntity> entities = entityManager.select(select().from("download")
                .where("_id").eq(id).build()).toList();

        assertEquals(1, entities.size());
        var documentEntity = entities.get(0);
        assertSoftly(soft -> {
            soft.assertThat(id).isEqualTo(documentEntity.find("_id").orElseThrow().get(Long.class));
            soft.assertThat(now).isEqualTo(documentEntity.find("now").orElseThrow().get(LocalDate.class));
        });
    }

    @Test
    void shouldConvertFromListDocumentList() {
        var entity = createDocumentList();
        assertDoesNotThrow(() -> entityManager.insert(entity));
    }

    @Test
    void shouldRetrieveListDocumentList() {
        var entity = entityManager.insert(createDocumentList());
        var key = entity.find("_id").orElseThrow();
        var query = select().from("AppointmentBook")
                .where(key.name())
                .eq(key.get()).build();

        var documentEntity = entityManager.singleResult(query).orElseThrow();
        assertNotNull(documentEntity);

        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").orElseThrow().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldCount() {
        entityManager.insert(getEntity());
        assertTrue(entityManager.count(COLLECTION_NAME) > 0);
    }

    @Test
    void shouldCountWithSelectQuery() {
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        assertSoftly(softly -> {
            softly.assertThat(entityManager.count(
                    select().from(COLLECTION_NAME)
                            .where("age").gt(22)
                            .and("type").eq("V")
                            .build()
            )).isEqualTo(2);

            softly.assertThat(entityManager.count(
                    select().from(COLLECTION_NAME)
                            .where("name").eq("Otavio")
                            .build()
            )).isEqualTo(1);
        });

    }

    @Test
    void shouldSaveMap() {
        var entity = CommunicationEntity.of(COLLECTION_NAME);
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
        var properties = documentEntity.find("properties").orElseThrow();
        var document = properties.get(Element.class);
        assertThat(document).isNotNull().isEqualTo(Element.of("hallo", "Welt"));
    }

    @Test
    void shouldInsertNull() {
        var entity = getEntity();
        entity.add(Element.of("name", null));
        var documentEntity = entityManager.insert(entity);
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
    void shouldQuery() {
        entityManager.insert(getEntity());

        var query = "select * from database where database.content.name = 'Poliana'";
        Stream<CommunicationEntity> entities = entityManager.sql(query);
        List<String> names = entities.map(d -> d.find("name").orElseThrow().get(String.class))
                .toList();
        assertThat(names).contains("Poliana");
    }

    @Test
    void shouldQueryParams() {
        entityManager.insert(getEntity());

        var query = "select * from database where database.content.name = ?";
        Stream<CommunicationEntity> entities = entityManager.sql(query, "Poliana");
        List<String> names = entities.map(d -> d.find("name").orElseThrow().get(String.class))
                .toList();
        assertThat(names).contains("Poliana");
    }

    private CommunicationEntity createDocumentList() {
        var entity = CommunicationEntity.of("AppointmentBook");
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

        assertSoftly(soft -> {
            soft.assertThat(entitiesFound).hasSize(2);
            var names = entitiesFound.stream()
                    .flatMap(d -> d.find("name").stream())
                    .map(d -> d.get(String.class))
                    .toList();
            soft.assertThat(names).contains("Lucas", "Luna");

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

    private CommunicationEntity getEntity() {
        var entity = CommunicationEntity.of(COLLECTION_NAME);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        map.put("_id", UUID.randomUUID().toString());
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

        var otavio = CommunicationEntity.of(COLLECTION_NAME);
        otavio.add(Element.of("name", "Otavio"));
        otavio.add(Element.of("age", 25));
        otavio.add(Element.of("location", "BR"));
        otavio.add(Element.of("type", "V"));

        var luna = CommunicationEntity.of(COLLECTION_NAME);
        luna.add(Element.of("name", "Luna"));
        luna.add(Element.of("age", 23));
        luna.add(Element.of("location", "US"));
        luna.add(Element.of("type", "V"));

        return asList(lucas, otavio, luna);
    }

}