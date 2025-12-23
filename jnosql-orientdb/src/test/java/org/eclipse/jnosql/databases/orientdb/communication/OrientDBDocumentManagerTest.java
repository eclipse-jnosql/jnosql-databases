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
 *   Lucas Furlaneto
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.orientdb.communication;

import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class OrientDBDocumentManagerTest {
    public static final String COLLECTION_NAME = "person";

    private OrientDBDocumentManager entityManager;

    @BeforeEach
    void setUp() {
        entityManager = DocumentDatabase.INSTANCE.get(Database.DATABASE);
    }

    @Test
    void shouldInsert() {
        var entity = getEntity();
        var documentEntity = entityManager.insert(entity);
        assertNotNull(documentEntity);
        Optional<Element> document = documentEntity.find(OrientDBConverter.RID_FIELD);
        assertTrue(document.isPresent());

    }

    @Test
    void shouldThrowExceptionWhenSaveWithTTL() {
        assertThrows(UnsupportedOperationException.class, () -> entityManager.insert(getEntity(), Duration.ZERO));
    }

    @Test
    void shouldUpdateSave() {
        var entity = entityManager.insert(getEntity());
        var newField = Elements.of("newField", "10");
        entity.add(newField);
        entityManager.update(entity);

        var id = entity.find(OrientDBConverter.RID_FIELD).get();
        var query = select().from(entity.name())
                .where(id.name()).eq(id.get())
                .build();
        Optional<CommunicationEntity> updated = entityManager.singleResult(query);

        assertTrue(updated.isPresent());
        assertEquals(newField, updated.get().find(newField.name()).get());
    }

    @Test
    void shouldUpdateWithRetry() {
        var entity = entityManager.insert(getEntity());
        entity.add(Element.of(OrientDBConverter.VERSION_FIELD, 0));
        var newField = Elements.of("newField", "99");
        entity.add(newField);
        entityManager.update(entity);

        var id = entity.find(OrientDBConverter.RID_FIELD).get();
        var query = select().from(entity.name())
                .where(id.name()).eq(id.get())
                .build();
        Optional<CommunicationEntity> updated = entityManager.singleResult(query);

        assertTrue(updated.isPresent());
        assertEquals(newField, updated.get().find(newField.name()).get());
    }

    @Test
    void shouldRemoveEntity() {
        CommunicationEntity documentEntity = entityManager.insert(getEntity());

        var id = documentEntity.find("name").get();

        var query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        var deleteQuery = delete().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        entityManager.delete(deleteQuery);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldFindDocument() {
        CommunicationEntity entity = entityManager.insert(getEntity());
        Element id = entity.find("name").get();

        var query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities, contains(entity));
    }

    @Test
    void shouldSQL() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find("name");

        List<CommunicationEntity> entities = entityManager.sql("select * from person where name = ?", id.get().get())
                .collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities, contains(entity));
    }

    @Test
    void shouldSQL2() {
        var entity = entityManager.insert(getEntity());
        Optional<Element> id = entity.find("name");

        List<CommunicationEntity> entities = entityManager.sql("select * from person where name = :name",
                singletonMap("name", id.get().get())).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities, contains(entity));
    }


    @Test
    void shouldSaveSubDocument() {
        var entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        var entitySaved = entityManager.insert(entity);
        var id = entitySaved.find("name").get();
        var query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        var entityFound = entityManager.select(query).toList().get(0);
        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents, contains(Element.of("mobile", "1231231")));
    }

    @Test
    void shouldSaveSubDocument2() {
        var entity = getEntity();
        entity.add(Element.of("phones", Arrays.asList(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231"))));
        var entitySaved = entityManager.insert(entity);
        Element id = entitySaved.find("name").get();
        var query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        var entityFound = entityManager.select(query).toList().get(0);
        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents, containsInAnyOrder(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231")));
    }

    @Test
    void shouldQueryAnd() {
        var entity = getEntity();
        entity.add(Element.of("age", 24));
        entityManager.insert(entity);


        var query = select().from(COLLECTION_NAME).where("name").eq("Poliana")
                .and("age").gte(10).build();

        var deleteQuery = delete().from(COLLECTION_NAME).where("name").eq("Poliana")
                .and("age").gte(10).build();

        assertFalse(entityManager.select(query).findAny().isEmpty());

        entityManager.delete(deleteQuery);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldQueryOr() {
        var entity = getEntity();
        entity.add(Element.of("age", 24));
        entityManager.insert(entity);


        var query = select().from(COLLECTION_NAME).where("name").eq("Poliana")
                .or("age").gte(10).build();

        var deleteQuery = delete().from(COLLECTION_NAME).where("name").eq("Poliana")
                .or("age").gte(10).build();

        assertFalse(entityManager.select(query).findAny().isEmpty());

        entityManager.delete(deleteQuery);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldQueryGreaterThan() {
        var entity = getEntity();
        entity.add("age", 25);
        entityManager.insert(entity);

        var query = select().from(COLLECTION_NAME)
                .where("age").gt(25)
                .build();
        assertTrue(entityManager.select(query).findAny().isEmpty());

        var query2 = select().from(COLLECTION_NAME)
                .where("age").gt(24)
                .build();
        assertEquals(1, (int) entityManager.select(query2).count());
    }

    @Test
    void shouldQueryLesserThan() {
        var entity = getEntity();
        entity.add("age", 25);
        entityManager.insert(entity);

        var query = select().from(COLLECTION_NAME)
                .where("age").lt(25)
                .build();
        assertTrue(entityManager.select(query).findAny().isEmpty());

        var query2 = select().from(COLLECTION_NAME)
                .where("age").lt(26)
                .build();
        assertEquals(1, (int) entityManager.select(query2).count());
    }

    @Test
    void shouldQueryLesserEqualsThan() {
        var entity = getEntity();
        entity.add("age", 25);
        entityManager.insert(entity);

        var query = select().from(COLLECTION_NAME)
                .where("age").lte(24)
                .build();
        assertTrue(entityManager.select(query).findAny().isEmpty());

        var query2 = select().from(COLLECTION_NAME)
                .where("age").lte(25)
                .build();
        assertEquals(1, (int) entityManager.select(query2).count());

        var query3 = select().from(COLLECTION_NAME)
                .where("age").lte(26)
                .build();
        assertEquals(1, (int) entityManager.select(query3).count());
    }

    @Test
    void shouldQueryIn() {
        entityManager.insert(getEntities());

        var query = select().from(COLLECTION_NAME)
                .where("city").in(asList("Salvador", "Assis"))
                .build();
        assertEquals(2, (int) entityManager.select(query).count());

        var deleteQuery = delete().from(COLLECTION_NAME)
                .where("city").in(asList("Salvador", "Assis", "Sao Paulo"))
                .build();
        entityManager.delete(deleteQuery);

        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldQueryLike() {
        List<CommunicationEntity> entitiesSaved = StreamSupport.stream(entityManager.insert(getEntities()).spliterator(), false)
                .toList();

        var query = select().from(COLLECTION_NAME)
                .where("city").like("Sa%")
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entities.size());
        assertThat(entities, containsInAnyOrder(entitiesSaved.get(0), entitiesSaved.get(1)));
    }

    @Test
    void shouldQueryNot() {
        List<CommunicationEntity> entitiesSaved = StreamSupport.stream(entityManager.insert(getEntities()).spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("city").not().eq("Assis")
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entities.size());
        assertThat(entities, containsInAnyOrder(entitiesSaved.get(0), entitiesSaved.get(1)));
    }

    @Test
    void shouldQueryStart() {
        entityManager.insert(getEntities());

        var query = select().from(COLLECTION_NAME)
                .skip(1)
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertEquals(2, entities.size());
    }

    @Test
    void shouldQueryLimit() {
        entityManager.insert(getEntities());

        var query = select().from(COLLECTION_NAME)
                .limit(2)
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertEquals(2, entities.size());
    }

    @Test
    void shouldQueryOrderBy() {
        List<CommunicationEntity> entitiesSaved = StreamSupport.stream(entityManager.insert(getEntities()).spliterator(), false).toList();

        var queryAsc = select().from(COLLECTION_NAME)
                .orderBy("name").asc()
                .build();

        var entitiesAsc = entityManager.select(queryAsc).collect(Collectors.toList());
        assertThat(entitiesAsc, contains(entitiesSaved.get(2), entitiesSaved.get(1), entitiesSaved.get(0)));

        var queryDesc = select().from(COLLECTION_NAME)
                .orderBy("name").desc()
                .build();

        var entitiesDesc = entityManager.select(queryDesc).collect(Collectors.toList());
        assertThat(entitiesDesc, contains(entitiesSaved.get(0), entitiesSaved.get(1), entitiesSaved.get(2)));
    }

    @Test
    void shouldQueryMultiOrderBy() {
        List<CommunicationEntity> entities = new ArrayList<>(getEntities());
        CommunicationEntity bruno = CommunicationEntity.of(COLLECTION_NAME);
        bruno.add(Element.of("name", "Bruno"));
        bruno.add(Element.of("city", "Sao Paulo"));
        entities.add(bruno);

        List<CommunicationEntity> entitiesSaved = StreamSupport.stream(entityManager.insert(entities).spliterator(), false)
                .toList();

        var query = select().from(COLLECTION_NAME)
                .orderBy("city").desc()
                .orderBy("name").asc()
                .build();

        var entitiesFound = entityManager.select(query).collect(Collectors.toList());
        assertThat(entitiesFound, contains(entitiesSaved.get(3), entitiesSaved.get(1), entitiesSaved.get(0), entitiesSaved.get(2)));
    }

    @Test
    void shouldLive() {
        AtomicBoolean condition = new AtomicBoolean(false);
        List<CommunicationEntity> entities = new ArrayList<>();
        OrientDBLiveCreateCallback<CommunicationEntity> callback = d -> {
            entities.add(d);
            condition.set(true);
        };

        entityManager.insert(getEntity());
        var query = select().from(COLLECTION_NAME).build();

        entityManager.live(query, OrientDBLiveCallbackBuilder.builder().onCreate(callback).build());
        entityManager.insert(getEntity());
        await().untilTrue(condition);
        assertFalse(entities.isEmpty());
    }

    @Test
    @Disabled
    void shouldLiveUpdateCallback() {

        AtomicBoolean condition = new AtomicBoolean(false);
        List<CommunicationEntity> entities = new ArrayList<>();
        OrientDBLiveUpdateCallback<CommunicationEntity> callback = d -> {
            entities.add(d);
            condition.set(true);
        };

        CommunicationEntity entity = entityManager.insert(getEntity());
        SelectQuery query = select().from(COLLECTION_NAME).build();

        entityManager.live(query, OrientDBLiveCallbackBuilder.builder().onUpdate(callback).build());
        Element newName = Element.of("name", "Lucas");
        entity.add(newName);
        entityManager.update(entity);
        await().untilTrue(condition);
        assertFalse(entities.isEmpty());
        assertFalse(entities.isEmpty());
    }

    @Test
    @Disabled
    void shouldLiveDeleteCallback() {
        AtomicBoolean condition = new AtomicBoolean(false);
        OrientDBLiveDeleteCallback<CommunicationEntity> callback = d -> condition.set(true);
        entityManager.insert(getEntity());
        SelectQuery query = select().from(COLLECTION_NAME).build();

        entityManager.live(query, OrientDBLiveCallbackBuilder.builder().onDelete(callback).build());
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        await().untilTrue(condition);
    }

    @Test
    void shouldLiveWithNativeQuery() {
        AtomicBoolean condition = new AtomicBoolean(false);
        List<CommunicationEntity> entities = new ArrayList<>();
        OrientDBLiveCreateCallback<CommunicationEntity> callback = d -> {
            entities.add(d);
            condition.set(true);
        };

        entityManager.insert(getEntity());

        entityManager.live("SELECT FROM person", OrientDBLiveCallbackBuilder.builder().onCreate(callback).build());
        entityManager.insert(getEntity());
        await().untilTrue(condition);
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldConvertFromListSubdocumentList() {
        var entity = createSubdocumentList();
        entityManager.insert(entity);

    }

    @Test
    void shouldRetrieveListSubdocumentList() {
        CommunicationEntity entity = entityManager.insert(createSubdocumentList());
        Element key = entity.find("_id").get();
        SelectQuery query = select().from("AppointmentBook").where(key.name()).eq(key.get()).build();

        var documentEntity = entityManager.singleResult(query).get();
        assertNotNull(documentEntity);

        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").get().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldCount() {
        CommunicationEntity entity = getEntity();
        CommunicationEntity documentEntity = entityManager.insert(entity);
        assertNotNull(documentEntity);
        assertTrue(entityManager.count(COLLECTION_NAME) > 0);

    }

    @Test
    void shouldCountWithSelectQuery() {

        Iterable<CommunicationEntity> entities = entityManager.insert(getEntities());

        Iterator<CommunicationEntity> iterator = entities.iterator();

        CommunicationEntity entity = iterator.next();
        CommunicationEntity entity2 = iterator.next();

        Element key = entity.find(OrientDBConverter.RID_FIELD).get();
        Element key2 = entity2.find(OrientDBConverter.RID_FIELD).get();

        SelectQuery query = select().from(COLLECTION_NAME)
                .where(key.name()).eq(key.get())
                .or(key2.name()).eq(key2.get())
                .build();

        assertSoftly(softly -> {
            softly.assertThat(entityManager.count(query))
                    .as("Count with query should be equal to 2")
                    .isEqualTo(2L);

        });
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

    private CommunicationEntity createSubdocumentList() {
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

    private List<CommunicationEntity> getEntities() {
        CommunicationEntity otavio = CommunicationEntity.of(COLLECTION_NAME);
        otavio.add(Element.of("name", "Otavio"));
        otavio.add(Element.of("city", "Sao Paulo"));

        var lucas = CommunicationEntity.of(COLLECTION_NAME);
        lucas.add(Element.of("name", "Lucas"));
        lucas.add(Element.of("city", "Assis"));

        return asList(getEntity(), otavio, lucas);
    }

    @AfterEach
    void removePersons() {
        entityManager.insert(getEntity());
        DeleteQuery query = delete().from(COLLECTION_NAME).build();
        entityManager.delete(query);
    }
}