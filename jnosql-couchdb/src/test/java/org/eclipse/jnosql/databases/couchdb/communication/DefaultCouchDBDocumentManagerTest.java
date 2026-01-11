/*
 *
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.databases.couchdb.communication.configuration.DocumentDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class DefaultCouchDBDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";

    private CouchDBDocumentManager entityManager;

    {
        CouchDBDocumentManagerFactory managerFactory = DocumentDatabase.INSTANCE.get();
        entityManager = managerFactory.apply("people");
    }

    @BeforeEach
    void setUp() {
        var query = delete().from(COLLECTION_NAME).build();
        entityManager.delete(query);
    }

    @Test
    void shouldInsert() {
        var entity = getEntity();
        var documentEntity = entityManager.insert(entity);
        assertEquals(entity, documentEntity);
    }

    @Test
    void shouldInsertNotId() {
        var entity = getEntity();
        entity.remove(CouchDBConstant.ID);
        var documentEntity = entityManager.insert(entity);
        assertTrue(documentEntity.find(CouchDBConstant.ID).isPresent());
    }

    @Test
    void shouldUpdate() {
        var entity = getEntity();
        entity.remove(CouchDBConstant.ID);
        var documentEntity = entityManager.insert(entity);
        var newField = Elements.of("newField", "10");
        entity.add(newField);
        var updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").get());
    }

    @Test
    void shouldReturnErrorOnUpdate() {
        assertThrows(NullPointerException.class, () -> entityManager.update((CommunicationEntity) null));
        assertThrows(CouchDBHttpClientException.class, () -> {
            var entity = getEntity();
            entity.remove(CouchDBConstant.ID);
            entityManager.update(entity);

        });

        assertThrows(CouchDBHttpClientException.class, () -> {
            var entity = getEntity();
            entity.add(CouchDBConstant.ID, "not_found");
            entityManager.update(entity);

        });
    }


    @Test
    void shouldSelect() {
        var entity = getEntity();
        entity.remove(CouchDBConstant.ID);
        entity = entityManager.insert(entity);
        Object id = entity.find(CouchDBConstant.ID).map(Element::get).get();
        var query = select().from(COLLECTION_NAME).where(CouchDBConstant.ID).eq(id).build();
        var documentFound = entityManager.singleResult(query).get();
        assertEquals(entity, documentFound);
    }

    @Test
    void shouldSelectEmptyResult() {
        var query = select().from(COLLECTION_NAME).where("no_field").eq("not_found").build();
        var entities = entityManager.select(query).toList();
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldRemoveEntityByName() {
        CommunicationEntity entity = getEntity();
        entity.remove(CouchDBConstant.ID);
        entity = entityManager.insert(entity);

        var name = entity.find("name").get();
        var query = select().from(COLLECTION_NAME).where(name.name()).eq(name.get()).build();
        var deleteQuery = delete().from(COLLECTION_NAME)
                .where(name.name()).eq(name.get()).build();
        entityManager.delete(deleteQuery);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldCount() {
        var entity = getEntity();
        entity.remove(CouchDBConstant.ID);
        entityManager.insert(entity);
        long count = entityManager.count();
        assertTrue(count > 0);
    }

    @Test
    void shouldSelectWithCouchDBDocumentQuery() {

        for (int index = 0; index < 4; index++) {
            CommunicationEntity entity = getEntity();
            entity.remove(CouchDBConstant.ID);
            entity.add("index", index);
            entityManager.insert(entity);
        }
        CouchDBDocumentQuery query = CouchDBDocumentQuery.of(select().from(COLLECTION_NAME)
                .where("index").in(asList(0, 1, 2, 3, 4)).limit(2).build());

        assertFalse(query.getBookmark().isPresent());
        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entities.size());
        assertTrue(query.getBookmark().isPresent());
        String bookmark = query.getBookmark().get();

        entities = entityManager.select(query).collect(Collectors.toList());
        assertEquals(2, entities.size());
        assertTrue(query.getBookmark().isPresent());
        assertNotEquals(bookmark, query.getBookmark().get());

        entities = entityManager.select(query).collect(Collectors.toList());
        assertTrue(entities.isEmpty());
    }

    @Test
    void shouldExecuteInStringQueryAtCouchbase() {
        for (int index = 0; index < 4; index++) {
            CommunicationEntity entity = getEntity();
            entity.remove(CouchDBConstant.ID);
            entity.add("index", index);
            entityManager.insert(entity);
        }
        CouchDBDocumentQuery query = CouchDBDocumentQuery.of(select().from(COLLECTION_NAME)
                .where("name").in(Arrays.asList("Poliana", "Poliana")).build());
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertEquals(4, entities.size());
    }

    @Test
    void shouldConvertFromListSubdocumentList() {
        CommunicationEntity entity = createDocumentList();
        entityManager.insert(entity);

    }

    @Test
    void shouldRetrieveListDocumentList() {
        var entity = entityManager.insert(createDocumentList());
        var key = entity.find(CouchDBConstant.ID).get();
        var query = select().from("AppointmentBook").where(key.name()).eq(key.get()).build();

        var documentEntity = entityManager.singleResult(query).get();
        assertNotNull(documentEntity);
        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").get().get();
        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldSaveSubDocument() {
        var entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        var entitySaved = entityManager.insert(entity);
        var id = entitySaved.find("_id").get();
        var query = select().from(COLLECTION_NAME)
                .where("_id").eq(id.get())
                .build();

        var entityFound = entityManager.select(query).toList().get(0);
        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"));
    }

    @Test
    void shouldSaveSubDocument2() {
        var entity = getEntity();
        entity.add(Element.of("phones", asList(Element.of("mobile", "1231231"),
                Element.of("mobile2", "1231231"))));
        var entitySaved = entityManager.insert(entity);
        var id = entitySaved.find("_id").get();

        var query = select().from(COLLECTION_NAME)
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
    void shouldSaveMap() {
        var entity = CommunicationEntity.of(COLLECTION_NAME);
        String id = UUID.randomUUID().toString();
        entity.add("properties", Collections.singletonMap("hallo", "Welt"));
        entity.add("scope", "xxx");
        entity.add("_id", id);
        entityManager.insert(entity);
        final SelectQuery query = select().from(COLLECTION_NAME)
                .where("_id").eq(id).and("scope").eq("xxx").build();
        final Optional<CommunicationEntity> optional = entityManager.select(query).findFirst();
        Assertions.assertTrue(optional.isPresent());
        var documentEntity = optional.get();
        var properties = documentEntity.find("properties").get();
        Assertions.assertNotNull(properties);
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
    void shouldUpdateNull(){
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

    @Test
    void shouldInsertByteArray() {
        CommunicationEntity entity = CommunicationEntity.of("Failure");
        entity.add(CouchDBConstant.ID, "id");
        entity.add("data", new byte[]{'a','b','c','d'});
        var communication = entityManager.insert(entity);

        SoftAssertions.assertSoftly(soft -> {
           soft.assertThat(communication).isNotNull();
              soft.assertThat(communication.find("data").get().get()).isInstanceOf(byte[].class);
        });
    }

    private CommunicationEntity createDocumentList() {
        CommunicationEntity entity = CommunicationEntity.of("AppointmentBook");
        List<List<Element>> documents = new ArrayList<>();

        documents.add(asList(Element.of("name", "Ada"), Element.of("type", "EMAIL"),
                Element.of("information", "ada@lovelace.com")));

        documents.add(asList(Element.of("name", "Ada"), Element.of("type", "MOBILE"),
                Element.of("information", "11 1231231 123")));

        documents.add(asList(Element.of("name", "Ada"), Element.of("type", "PHONE"),
                Element.of("information", "phone")));

        entity.add(Element.of("contacts", documents));
        return entity;
    }

    private CommunicationEntity getEntity() {
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_NAME);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        map.put(CouchDBConstant.ID, UUID.randomUUID().toString());

        List<Element> documents = Elements.of(map);
        documents.forEach(entity::add);
        return entity;
    }
}