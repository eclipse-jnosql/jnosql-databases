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

package org.eclipse.jnosql.databases.solr.communication;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class DefaultSolrDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";
    public static final String ID = "_id";
    private static SolrDocumentManager entityManager;

    @BeforeAll
    public static void setUp() {
        entityManager = DocumentDatabase.INSTANCE.get();
    }

    @Test
    void shouldInsert() {
        var entity = getEntity();
        var documentEntity = entityManager.insert(entity);
        assertTrue(documentEntity.elements().stream().map(Element::name).anyMatch(s -> s.equals(ID)));
    }

    @Test
    void shouldThrowExceptionWhenInsertWithTTL() {
        assertThrows(UnsupportedOperationException.class,
                () -> entityManager.insert(getEntity(), Duration.ofSeconds(10)));
    }

    @Test
    void shouldUpdateSave() {
        var entity = getEntity();
        entityManager.insert(entity);
        var newField = Elements.of("newField", "10");
        entity.add(newField);
        var updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").get());
    }

    @Test
    void shouldRemoveEntity() {
        var documentEntity = entityManager.insert(getEntity());

        Optional<Element> id = documentEntity.find(ID);
        var query = select().from(COLLECTION_NAME)
                .where(ID).eq(id.get().get())
                .build();
        var deleteQuery = delete().from(COLLECTION_NAME).where(ID)
                .eq(id.get().get())
                .build();

        entityManager.delete(deleteQuery);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldFindDocument() {
        CommunicationEntity entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find(ID);

        var query = select().from(COLLECTION_NAME)
                .where(ID).eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        final CommunicationEntity result = entities.get(0);

        assertEquals(entity.find("name").get(), result.find("name").get());
        assertEquals(entity.find("city").get(), result.find("city").get());

    }

    @Test
    void shouldFindDocument2() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find(ID);

        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .and("city").eq("Salvador").and(ID).eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        final CommunicationEntity result = entities.get(0);

        assertEquals(entity.find("name").get(), result.find("name").get());
        assertEquals(entity.find("city").get(), result.find("city").get());
    }

    @Test
    void shouldFindDocument3() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find(ID);
        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .or("city").eq("Salvador")
                .and(id.get().name()).eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        final CommunicationEntity result = entities.get(0);
        assertEquals(entity.find("name").get(), result.find("name").get());
        assertEquals(entity.find("city").get(), result.find("city").get());
    }

    @Test
    void shouldFindDocumentGreaterThan() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
        assertEquals(3, entitiesFound.size());
    }

    @Test
    void shouldFindNot() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());
        var query = select().from(COLLECTION_NAME)
                .where("name").not().eq("Lucas").build();
        List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
        assertEquals(2, entitiesFound.size());
    }

    @Test
    void shouldFindDocumentGreaterEqualsThan() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
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
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());

        var query = select().from(COLLECTION_NAME)
                .where("age").lt(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
        assertEquals(2, entitiesFound.size());
    }

    @Test
    void shouldFindDocumentLesserEqualsThan() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").lte(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
        assertEquals(2, entitiesFound.size());
    }

    @Test
    void shouldFindDocumentLike() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entities = entityManager.insert(getEntitiesWithValues());

        var query = select().from(COLLECTION_NAME)
                .where("name").like("Lu*")
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
        assertEquals(2, entitiesFound.size());
    }

    @Test
    void shouldFindDocumentIn() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("location").in(asList("BR", "US"))
                .and("type").eq("V")
                .build();

        assertEquals(3, (int) entityManager.select(query).count());
    }

    @Test
    void shouldFindDocumentStart() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .skip(1L)
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        assertThat(entitiesFound).isNotIn(entities.get(0));

        query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .skip(3L)
                .build();

        entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertTrue(entitiesFound.isEmpty());

    }

    @Test
    void shouldFindDocumentLimit() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
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
        entityManager.delete(deleteQuery);
    }

    @Test
    void shouldFindDocumentSort() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = entityManager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").asc()
                .build();

        List<CommunicationEntity> entitiesFound = entityManager.select(query).collect(Collectors.toList());
        List<Integer> ages = entitiesFound.stream()
                .map(e -> e.find("age").get().get(Integer.class))
                .collect(Collectors.toList());

        assertThat(ages).contains(22, 23, 25);

        query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .orderBy("age").desc()
                .build();

        entitiesFound = entityManager.select(query).toList();
        ages = entitiesFound.stream()
                .map(e -> e.find("age").get().get(Integer.class))
                .collect(Collectors.toList());
        assertThat(ages).contains(25, 23, 22);

    }

    @Test
    void shouldExecuteNativeQuery() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());

        List<CommunicationEntity> entitiesFound = entityManager.solr("age:22 AND type:V AND _entity:person");
        assertEquals(1, entitiesFound.size());
    }

    @Test
    void shouldExecuteNativeQueryParams() {
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());

        Map<String, Object> params = new HashMap<>();
        params.put("age", 22);
        params.put("type", "V");
        params.put("entity", "person");

        List<CommunicationEntity> entitiesFound = entityManager.solr("age:@age AND type:@type AND _entity:@entity",
                params);
        assertEquals(1, entitiesFound.size());
    }

    @Test
    void shouldExecuteNativeQueryParamsReplaceAll() {
        entityManager.insert(getEntitiesWithValues());
        var deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());

        Map<String, Object> params = new HashMap<>();
        params.put("age", 22);

        List<CommunicationEntity> entitiesFound = entityManager.solr("age:@age AND age:@age", params);
        assertEquals(1, entitiesFound.size());
    }

    @Test
    void shouldFindAll() {
        entityManager.insert(getEntity());
        var query = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldReturnErrorWhenSaveSubDocument() {
        var entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        Assertions.assertThrows(SolrException.class, () -> entityManager.insert(entity));

    }

    @Test
    void shouldSaveSubDocument2() {
        var entity = getEntity();
        entity.add(Element.of("phones", asList(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231"))));
        Assertions.assertThrows(SolrException.class, () -> entityManager.insert(entity));
    }

    @Test
    void shouldCreateDate() {
        Date date = new Date();
        LocalDate now = LocalDate.now();

        var entity = CommunicationEntity.of("download");
        long id = ThreadLocalRandom.current().nextLong(1, 10);
        entity.add(ID, id);
        entity.add("date", date);
        entity.add("now", now);

        entityManager.insert(entity);

        List<CommunicationEntity> entities = entityManager.select(select().from("download")
                .where(ID).eq(id).build()).toList();

        assertEquals(1, entities.size());
        var documentEntity = entities.get(0);
        assertEquals(date, documentEntity.find("date").get().get(Date.class));
        assertEquals(now, documentEntity.find("date").get().get(LocalDate.class));
    }

    @Test
    void shouldRetrieveListSubdocumentList() {
        Assertions.assertThrows(SolrException.class, () -> entityManager.insert(createSubdocumentList()));
    }

    @Test
    void shouldCount() {
        entityManager.insert(getEntity());
        assertSoftly(softly -> {
            softly.assertThat(entityManager.count(COLLECTION_NAME))
                    .as("should count collection")
                    .isGreaterThan(0);
            softly.assertThat(entityManager.count("unknown_collection"))
                    .as("should count unknown collection")
                    .isEqualTo(0);
            softly.assertThatCode(() -> entityManager.count((String) null))
                    .as("should throw exception when count with null collection name")
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void shouldCountWithSelectQuery() {
        entityManager.insert(getEntity());
        var query = SelectQuery.select()
                .from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .build();

        assertSoftly(softly -> {
            softly.assertThat(entityManager.count(query))
                    .as("should count with select query")
                    .isEqualTo(1);
            softly.assertThatCode(() -> entityManager.count((SelectQuery) null))
                    .as("should throw exception when count with null select query")
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void shouldInsertNull() {
        var entity = getEntity();
        entity.add(Element.of("name", null));
        var documentEntity = entityManager.insert(entity);
        Optional<Element> name = documentEntity.find("name");
        SoftAssertions.assertSoftly(soft -> {
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
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(name).isPresent();
            soft.assertThat(name).get().extracting(Element::name).isEqualTo("name");
            soft.assertThat(name).get().extracting(Element::get).isNull();
        });
    }

    private CommunicationEntity createSubdocumentList() {
        CommunicationEntity entity = CommunicationEntity.of("AppointmentBook");
        entity.add(Element.of(ID, new Random().nextInt()));
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
        map.put(ID, ThreadLocalRandom.current().nextLong(1, 10));
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

        CommunicationEntity otavio = CommunicationEntity.of(COLLECTION_NAME);
        otavio.add(Element.of("name", "Otavio"));
        otavio.add(Element.of("age", 25));
        otavio.add(Element.of("location", "BR"));
        otavio.add(Element.of("type", "V"));

        CommunicationEntity luna = CommunicationEntity.of(COLLECTION_NAME);
        luna.add(Element.of("name", "Luna"));
        luna.add(Element.of("age", 23));
        luna.add(Element.of("location", "US"));
        luna.add(Element.of("type", "V"));

        return asList(lucas, otavio, luna);
    }

}