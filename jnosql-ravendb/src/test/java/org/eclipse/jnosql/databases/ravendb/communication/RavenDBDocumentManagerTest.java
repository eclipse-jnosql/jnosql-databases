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

package org.eclipse.jnosql.databases.ravendb.communication;

import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RavenDBDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";
    private static final long TIME_LIMIT = 500L;
    private static final String APPOINTMENT_BOOK = "AppointmentBook";
    private static DatabaseManager manager;

    @BeforeAll
    public static void setUp() {
        manager = DocumentConfigurationUtils.INSTANCE.get().apply("database");
    }

    @BeforeEach
    public void before() {
        manager.delete(delete().from(COLLECTION_NAME).build());
        manager.delete(delete().from(APPOINTMENT_BOOK).build());
    }

    @AfterEach
    public void after() {
        manager.delete(delete().from(COLLECTION_NAME).build());
        manager.delete(delete().from(APPOINTMENT_BOOK).build());
    }


    @Test
    public void shouldInsert() {
        var entity = getEntity();
        var documentEntity = manager.insert(entity);
        assertTrue(documentEntity.elements().stream().map(Element::name).anyMatch(s -> s.equals("_id")));
    }

    @Test
    public void shouldThrowExceptionWhenInsertWithTTL() {
        var entity = manager.insert(getEntity(), Duration.ofMillis(1));
        Optional<Element> id = entity.find("_id");
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get().get())
                .build();

    }

    @Test
    public void shouldUpdate() {
        CommunicationEntity entity = getEntity();
        CommunicationEntity documentEntity = manager.insert(entity);
        var newField = Elements.of("newField", "10");
        entity.add(newField);
        var updated = manager.update(entity);
        assertEquals(newField, updated.find("newField").get());
    }

    @Test
    public void shouldRemoveEntity() {
        var entity = getEntity();
        var documentEntity = manager.insert(entity);

        var id = documentEntity.find("_id");
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get().get())
                .build();
        var deleteQuery = delete().from(COLLECTION_NAME).where("_id")
                .eq(id.get().get())
                .build();

        manager.delete(deleteQuery);
        assertTrue(manager.select(query).findAny().isEmpty());
    }

    @Test
    public void shouldFindDocument() {
        var entity = manager.insert(getEntity());
        var id = entity.find("_id");

        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = manager.select(query)
                .collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    public void shouldRunSingleResult() {
        CommunicationEntity entity = manager.insert(getEntity());
        Optional<Element> id = entity.find("_id");

        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get().get())
                .build();

        Optional<CommunicationEntity> result = manager.singleResult(query);
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
    }

    @Test
    public void shouldFindDocument2() {
        CommunicationEntity entity = manager.insert(getEntity());
        Optional<Element> id = entity.find("_id");

        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .and("city").eq("Salvador").and("_id").eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = manager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    public void shouldFindDocument3() {
        var entity = manager.insert(getEntity());
        Optional<Element> id = entity.find("_id");
        var query = select().from(COLLECTION_NAME)
                .where("name").eq("Poliana")
                .or("city").eq("Salvador")
                .and(id.get().name()).eq(id.get().get())
                .build();

        List<CommunicationEntity> entities = manager.select(query)
                .collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    public void shouldFindDocumentGreaterThan() throws InterruptedException {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        manager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = manager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gt(22)
                .and("type").eq("V")
                .build();

        Thread.sleep(TIME_LIMIT);
        List<CommunicationEntity> entitiesFound = manager.select(query).collect(Collectors.toList());
        assertThat(entitiesFound).hasSize(2).isNotIn(entities.getFirst());
    }

    @Test
    public void shouldFindDocumentGreaterEqualsThan() throws InterruptedException {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        manager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = manager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").gte(23)
                .and("type").eq("V")
                .build();

        Thread.sleep(TIME_LIMIT);
        List<CommunicationEntity> entitiesFound = manager.select(query).collect(Collectors.toList());
        assertEquals(2, entitiesFound.size());
        assertThat(entitiesFound).isNotIn(entities.getFirst());
    }

    @Test
    public void shouldFindDocumentLesserThan() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        manager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = manager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").lt(23)
                .and("type").eq("V")
                .build();

        List<CommunicationEntity> entitiesFound = manager.select(query).collect(Collectors.toList());
        assertThat(entitiesFound).hasSize(1).contains(entities.getFirst());
    }

    @Test
    public void shouldFindDocumentLesserEqualsThan() throws InterruptedException {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        manager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = manager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).toList();

        var query = select().from(COLLECTION_NAME)
                .where("age").lte(23)
                .and("type").eq("V")
                .build();

        Thread.sleep(TIME_LIMIT);
        List<CommunicationEntity> entitiesFound = manager.select(query).collect(Collectors.toList());
        System.out.println(entitiesFound);
        assertThat(entitiesFound)
                .hasSize(2)
                .contains(entities.get(0), entities.get(2));
    }


    @Test
    public void shouldFindDocumentIn() throws InterruptedException {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        manager.delete(deleteQuery);
        Iterable<CommunicationEntity> entitiesSaved = manager.insert(getEntitiesWithValues());
        List<CommunicationEntity> entities = StreamSupport.stream(entitiesSaved.spliterator(), false).collect(Collectors.toList());

        var query = select().from(COLLECTION_NAME)
                .where("location").in(asList("BR", "US"))
                .and("type").eq("V")
                .build();
        Thread.sleep(TIME_LIMIT);
        assertEquals(entities, manager.select(query).collect(Collectors.toList()));
    }

    @Test
    public void shouldFindAll() {
        manager.insert(getEntity());
        var query = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = manager.select(query).toList();
        assertFalse(entities.isEmpty());
    }


    @Test
    public void shouldSaveSubDocument() {
        var entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        var entitySaved = manager.insert(entity);
        var id = entitySaved.find("_id").get();
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get())
                .build();

        var entityFound = manager.select(query).toList().getFirst();
        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"));
    }

    @Test
    public void shouldSaveSubDocument2() {
        var entity = getEntity();
        entity.add(Element.of("phones", asList(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231"))));
        var entitySaved = manager.insert(entity);
        var id = entitySaved.find("_id").get();

        var query = select().from(COLLECTION_NAME)
                .where(id.name()).eq(id.get())
                .build();
        var entityFound = manager.select(query).toList().getFirst();
        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"),
                Element.of("mobile2", "1231231"));
    }

    @Test
    public void shouldConvertFromListSubdocumentList() {
        var entity = createSubdocumentList();
        manager.insert(entity);

    }

    @Test
    public void shouldRetrieveListSubdocumentList() {
        var entity = manager.insert(createSubdocumentList());
        var key = entity.find("_id").get();
        var query = select().from(APPOINTMENT_BOOK)
                .where(key.name())
                .eq(key.get()).build();

        var documentEntity = manager.singleResult(query).get();
        assertNotNull(documentEntity);

        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").get().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    private CommunicationEntity createSubdocumentList() {
        CommunicationEntity entity = CommunicationEntity.of(APPOINTMENT_BOOK);
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
    public void shouldCount() {
        CommunicationEntity entity = getEntity();
        manager.insert(entity);
        assertTrue(manager.count(COLLECTION_NAME) > 0);
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
