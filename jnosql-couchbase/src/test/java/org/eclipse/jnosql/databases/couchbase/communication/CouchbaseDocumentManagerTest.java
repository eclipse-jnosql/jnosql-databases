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
package org.eclipse.jnosql.databases.couchbase.communication;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.json.JsonObject;
import org.assertj.core.api.Assertions;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DefaultSelectQuery;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.eclipse.jnosql.mapping.semistructured.MappingQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class CouchbaseDocumentManagerTest {

    public static final String COLLECTION_PERSON_NAME = "person";
    public static final String COLLECTION_APP_NAME = "AppointmentBook";
    private static Settings settings;
    private static CouchbaseDocumentManager entityManager;
    private static BucketManager keyValueEntityManagerForPerson;
    private static BucketManager keyValueEntityManagerForAppointmentBook;

    static {
        settings = Database.INSTANCE.getSettings();
        CouchbaseDocumentConfiguration configuration = Database.INSTANCE.getDocumentConfiguration();
        CouchbaseDocumentManagerFactory managerFactory = configuration.apply(settings);
        entityManager = managerFactory.apply(CouchbaseUtil.BUCKET_NAME);

        CouchbaseKeyValueConfiguration keyValueConfiguration = Database.INSTANCE.getKeyValueConfiguration();
        CouchbaseBucketManagerFactory keyValueEntityManagerFactory = keyValueConfiguration.apply(settings);
        keyValueEntityManagerForPerson = keyValueEntityManagerFactory
                .getBucketManager(CouchbaseUtil.BUCKET_NAME, COLLECTION_PERSON_NAME);
        keyValueEntityManagerForAppointmentBook = keyValueEntityManagerFactory
                .getBucketManager(CouchbaseUtil.BUCKET_NAME, COLLECTION_APP_NAME);
    }

    @BeforeEach
    @AfterEach
    void cleanUpData() throws InterruptedException {
        try {
            keyValueEntityManagerForPerson.delete("id");
            keyValueEntityManagerForPerson.delete("id2");
            keyValueEntityManagerForPerson.delete("id3");
        } catch (DocumentNotFoundException exp) {
            //IGNORE
        }
        try {
            keyValueEntityManagerForAppointmentBook.delete("ids");
        } catch (DocumentNotFoundException exp) {
            //IGNORE
        }
        Thread.sleep(1_000L);
    }

    @Test
    void shouldInsert() {
        CommunicationEntity entity = getEntity();
        CommunicationEntity documentEntity = entityManager.insert(entity);
        assertEquals(entity, documentEntity);
    }

    @Test
    void shouldInsertWithKey() {
        CommunicationEntity entity = getEntity();
        entity.add("_key", "anyvalue");
        CommunicationEntity documentEntity = entityManager.insert(entity);
        assertEquals(entity, documentEntity);
    }

    @Test
    void shouldUpdate() {
        CommunicationEntity entity = getEntity();
        CommunicationEntity documentEntity = entityManager.insert(entity);
        Element newField = Elements.of("newField", "10");
        entity.add(newField);
        CommunicationEntity updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").get());
    }

    @Test
    void shouldUpdateQuery() {
        List<CommunicationEntity> persistedEntities =
                StreamSupport.stream(entityManager.insert(getEntities()).spliterator(), false)
                        .toList();

        await().until(() ->
                entityManager
                        .count(COLLECTION_PERSON_NAME) >=3
        );

        CommunicationEntity entity = persistedEntities.stream().findFirst().orElseThrow();

        Element id = entity.find("_id").get();

        UpdateQuery updateQuery = new UpdateQuery() {
            @Override
            public String name() {
                return COLLECTION_PERSON_NAME;
            }

            @Override
            public List<Element> set() {
                return List.of(Elements.of("newField", "10"));
            }

            @Override
            public Optional<CriteriaCondition> condition() {
                return Optional.of(CriteriaCondition.eq(id));
            }

            @Override
            public SelectQuery toSelectQuery() {
                return new DefaultSelectQuery(0, 0, COLLECTION_PERSON_NAME, List.of(), List.of(), condition().orElseThrow(), false);
            }
        };
        entityManager.update(updateQuery);
        Iterable<CommunicationEntity> updatedEntities = entityManager.select(updateQuery.toSelectQuery()).toList();

        StreamSupport.stream(updatedEntities.spliterator(), false)
                .forEach(e -> {
                    Optional<Element> updateField = e.find("newField");
                    assertSoftly(soft -> {
                        soft.assertThat(updateField).isPresent();
                        soft.assertThat(updateField).get().extracting(Element::name).isEqualTo("newField");
                        soft.assertThat(updateField).get().extracting(Element::get).isEqualTo("10");
                    });
                });
    }


    @Test
    void shouldRemoveEntityByName() throws InterruptedException {
        CommunicationEntity documentEntity = entityManager.insert(getEntity());
        Thread.sleep(1_000L);
        Element name = documentEntity.find("name").get();
        SelectQuery query = select().from(COLLECTION_PERSON_NAME).where(name.name()).eq(name.get()).build();
        DeleteQuery deleteQuery = delete().from(COLLECTION_PERSON_NAME)
                .where(name.name()).eq(name.get()).build();
        entityManager.delete(deleteQuery);
        Thread.sleep(1_000L);
        assertTrue(entityManager.select(query).findAny().isEmpty());
    }

    @Test
    void shouldSaveSubDocument() {
        CommunicationEntity entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        CommunicationEntity entitySaved = entityManager.insert(entity);
        Element id = entitySaved.find("_id").get();
        SelectQuery query = select().from(COLLECTION_PERSON_NAME).where(id.name()).eq(id.get()).build();
        CommunicationEntity entityFound = entityManager.select(query).toList().get(0);
        Element subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"));
    }

    @Test
    void shouldSaveSubDocument2() throws InterruptedException {
        CommunicationEntity entity = getEntity();
        entity.add(Element.of("phones", asList(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231"))));
        CommunicationEntity entitySaved = entityManager.insert(entity);
        Thread.sleep(1_00L);
        Element id = entitySaved.find("_id").get();
        var query = select().from(COLLECTION_PERSON_NAME).where(id.name()).eq(id.get()).build();
        CommunicationEntity entityFound = entityManager.select(query).toList().get(0);
        Element subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"),
                Element.of("mobile2", "1231231"));
    }

    @Test
    void shouldSaveSetDocument() throws InterruptedException {
        Set<String> set = new HashSet<>();
        set.add("Acarajé");
        set.add("Munguzá");
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_PERSON_NAME);
        entity.add(Element.of("_id", "id"));
        entity.add(Element.of("foods", set));
        entityManager.insert(entity);
        Element id = entity.find("_id").get();
        Thread.sleep(1_000L);
        var query = select().from(COLLECTION_PERSON_NAME).where(id.name()).eq(id.get()).build();
        CommunicationEntity entityFound = entityManager.singleResult(query).get();
        Optional<Element> foods = entityFound.find("foods");
        Set<String> setFoods = foods.get().get(new TypeReference<>() {
        });
        assertEquals(set, setFoods);
    }

    @Test
    void shouldConvertFromListDocumentList() {
        CommunicationEntity entity = createSubdocumentList();
        entityManager.insert(entity);

    }

    @Test
    void shouldRetrieveListDocumentList() {
        CommunicationEntity entity = entityManager.insert(createSubdocumentList());
        Element key = entity.find("_id").get();
        var query = select().from(COLLECTION_APP_NAME).where(key.name()).eq(key.get()).build();

        CommunicationEntity documentEntity = entityManager.singleResult(query).get();
        assertNotNull(documentEntity);

        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").get().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldCount() throws InterruptedException {
        CommunicationEntity entity = entityManager.insert(createSubdocumentList());
        Thread.sleep(1_000L);

        Element key = entity.find("_id").get();
        var query = select().from(COLLECTION_APP_NAME).where(key.name()).eq(key.get()).build();

        assertSoftly(softly -> {
            softly.assertThat(entityManager.count(entity.name()))
                    .as("must return more than zero for person collection")
                    .isEqualTo(1L);

            softly.assertThat(entityManager.count(query))
                    .isEqualTo(1L);

            softly.assertThatThrownBy(() ->
                            entityManager.count((String) null))
                    .isInstanceOf(NullPointerException.class);

            softly.assertThatThrownBy(() ->
                            entityManager.count((SelectQuery) null))
                    .isInstanceOf(NullPointerException.class);

        });

    }

    private CommunicationEntity createSubdocumentList() {
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_APP_NAME);
        entity.add(Element.of("_id", "ids"));
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
    void shouldRunN1Ql() {
        CommunicationEntity entity = getEntity();
        entityManager.insert(entity);
        await().until(() ->
                !(entityManager
                        .n1qlQuery("select * from `jnosql`._default.person").findAny().isEmpty())
        );
    }

    @Test
    void shouldRunN1QlParameters() {
        CommunicationEntity entity = getEntity();
        entityManager.insert(entity);

        JsonObject params = JsonObject.create().put("name", entity.find("name", String.class).orElse(null));

        await().until(() ->
                !(entityManager
                        .n1qlQuery("select * from `jnosql`._default.person where name = $name", params).findAny().isEmpty())
        );
    }

    @Test
    void shouldCreateLimitOrderQuery() {
        CommunicationEntity entity = getEntity();
        entity.add("_id", "id2");
        entityManager.insert(entity);
        entityManager.insert(getEntity());

        var query = SelectQuery.select()
                .from(COLLECTION_PERSON_NAME).where("name")
                .eq("Poliana").and("city").eq("Salvador").limit(1).skip(1).build();
        List<CommunicationEntity> select = entityManager.select(query).toList();
        Assertions.assertThat(select).flatMap(d -> d.find("name").stream().toList())
                .containsOnly(Element.of("name", "Poliana"));
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
    void shouldFindContains() {
        var entity = getEntity();

        entityManager.insert(entity);

        await().until(
                () -> !(entityManager.select(select().from(entity.name()).build()).toList().isEmpty())
        );

        var query = new MappingQuery(Collections.emptyList(), 0L, 0L, CriteriaCondition.contains(Element.of("name",
                "lia")), COLLECTION_PERSON_NAME, Collections.emptyList());

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
        await().until(
                () -> !(entityManager.select(select().from(entity.name()).build()).toList().isEmpty())
        );

        var query = new MappingQuery(Collections.emptyList(), 0L, 0L, CriteriaCondition.startsWith(Element.of("name",
                "Pol")), COLLECTION_PERSON_NAME, Collections.emptyList());

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

        await().until(
                () -> !(entityManager.select(select().from(entity.name()).build()).toList().isEmpty())
        );

        var query = new MappingQuery(Collections.emptyList(), 0L, 0L, CriteriaCondition.endsWith(Element.of("name",
                "ana")), COLLECTION_PERSON_NAME, Collections.emptyList());

        var result = entityManager.select(query).toList();
        assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).find("name").orElseThrow().get(String.class)).isEqualTo("Poliana");
        });
    }

    private CommunicationEntity getEntity() {
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_PERSON_NAME);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        map.put("_id", "id");

        List<Element> documents = Elements.of(map);
        documents.forEach(entity::add);
        return entity;
    }

    private List<CommunicationEntity> getEntities() {

        List<CommunicationEntity> entities = new ArrayList<>();

        CommunicationEntity entity1 = CommunicationEntity.of(COLLECTION_PERSON_NAME);
        Elements.of(Map.of(
                "name", "Poliana",
                "city", "Salvador",
                "_id", "id"
        )).forEach(entity1::add);
        entities.add(entity1);

        CommunicationEntity entity2 = CommunicationEntity.of(COLLECTION_PERSON_NAME);
        Elements.of(Map.of(
                "name", "Otavio",
                "city", "Salvador",
                "_id", "id2"
        )).forEach(entity2::add);
        entities.add(entity2);

        CommunicationEntity entity3 = CommunicationEntity.of(COLLECTION_PERSON_NAME);
        Elements.of(Map.of(
                "name", "Maximillian",
                "city", "Sao Paulo",
                "_id", "id3"
        )).forEach(entity3::add);
        entities.add(entity3);

        return entities;
    }


}