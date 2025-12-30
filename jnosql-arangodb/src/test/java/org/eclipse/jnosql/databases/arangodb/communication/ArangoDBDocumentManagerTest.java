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

package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoDB;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class ArangoDBDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";
    private static final String DATABASE = "database";
    private ArangoDBDocumentManager entityManager;
    private Random random;
    private final String KEY_NAME = "_key";

    @BeforeEach
    void setUp() {
        random = new Random();
        entityManager = DocumentDatabase.INSTANCE.get().apply(DATABASE);
        entityManager.delete(delete().from(COLLECTION_NAME).build());

    }

    @AfterEach
    void after() {
        entityManager.delete(delete().from(COLLECTION_NAME).build());
    }

    @Test
    void shouldSave() {
        var entity = getEntity();

        CommunicationEntity documentEntity = entityManager.insert(entity);
        assertTrue(documentEntity.elements().stream().map(Element::name).anyMatch(s -> s.equals(KEY_NAME)));
    }

    @Test
    void shouldUpdateSave() {
        CommunicationEntity entity = getEntity();
        entityManager.insert(entity);
        Element newField = Elements.of("newField", "10");
        entity.add(newField);
        CommunicationEntity updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").get());
    }

    @Test
    void shouldRemoveEntity() {
        CommunicationEntity documentEntity = entityManager.insert(getEntity());
        Element id = documentEntity.find("_key").get();
        SelectQuery select = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        entityManager.delete(deleteQuery);
        assertThat(entityManager.select(select)).hasSize(0);
    }

    @Test
    void shouldRemoveEntity2() {
        CommunicationEntity documentEntity = entityManager.insert(getEntity());
        Element id = documentEntity.find("name").get();
        SelectQuery select = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        entityManager.delete(deleteQuery);
        assertThat(entityManager.select(select)).hasSize(0);
    }


    @Test
    void shouldFindDocument() {
        CommunicationEntity entity = entityManager.insert(getEntity());
        Element id = entity.find(KEY_NAME).get();
        SelectQuery query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        CommunicationEntity documentEntity = entities.get(0);
        assertEquals(entity.find(KEY_NAME).get().value().get(String.class), documentEntity.find(KEY_NAME).get()
                .value().get(String.class));
        assertEquals(entity.find("name").get(), documentEntity.find("name").get());
        assertEquals(entity.find("city").get(), documentEntity.find("city").get());
    }


    @Test
    void shouldSaveSubDocument() {
        CommunicationEntity entity = getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        CommunicationEntity entitySaved = entityManager.insert(entity);
        Element id = entitySaved.find(KEY_NAME).get();
        SelectQuery query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        CommunicationEntity entityFound = entityManager.select(query).toList().get(0);
        Element subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"));
    }

    @Test
    void shouldSaveSubDocument2() {
        CommunicationEntity entity = getEntity();
        entity.add(Element.of("phones", Arrays.asList(Element.of("mobile", "1231231"), Element.of("mobile2", "1231231"))));
        CommunicationEntity entitySaved = entityManager.insert(entity);
        Element id = entitySaved.find(KEY_NAME).get();
        SelectQuery query = select().from(COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        CommunicationEntity entityFound = entityManager.select(query).toList().get(0);
        Element subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"),
                Element.of("mobile2", "1231231"));
    }


    @Test
    void shouldConvertFromListSubdocumentList() {
        CommunicationEntity entity = createDocumentList();
        entityManager.insert(entity);

    }

    @Test
    void shouldRetrieveListSubdocumentList() {
        CommunicationEntity entity = entityManager.insert(createDocumentList());
        Element key = entity.find(KEY_NAME).get();
        SelectQuery query = select().from("AppointmentBook").where(key.name()).eq(key.get()).build();

        CommunicationEntity documentEntity = entityManager.singleResult(query).get();
        assertNotNull(documentEntity);

        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").get().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldConvertFromListSubdocumentListNotUsingKey() {
        CommunicationEntity entity = createDocumentListNotHavingId();
        entityManager.insert(entity);

    }

    @Test
    void shouldRetrieveListSubdocumentListNotUsingKey() {
        CommunicationEntity entity = entityManager.insert(createDocumentListNotHavingId());
        Element key = entity.find(KEY_NAME).get();
        SelectQuery query = select().from("AppointmentBook").where(key.name()).eq(key.get()).build();

        CommunicationEntity documentEntity = entityManager.singleResult(query).get();
        assertNotNull(documentEntity);

        List<List<Element>> contacts = (List<List<Element>>) documentEntity.find("contacts").get().get();

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    void shouldRunAQL() {
        CommunicationEntity entity = getEntity();
        CommunicationEntity entitySaved = entityManager.insert(entity);

        String aql = "FOR a IN person FILTER a.name == @name RETURN a";
        List<CommunicationEntity> entities = entityManager.aql(aql,
                singletonMap("name", "Poliana")).collect(Collectors.toList());
        assertNotNull(entities);
    }


    @Test
    void shouldCount() {
        CommunicationEntity entity = getEntity();
        entityManager.insert(entity);

        assertTrue(entityManager.count(COLLECTION_NAME) > 0);
    }

    @Test
    void shouldCountWithSelectQuery() {
        CommunicationEntity entity = entityManager.insert(createDocumentListNotHavingId());
        Element key = entity.find(KEY_NAME).get();
        SelectQuery query = select().from("AppointmentBook").where(key.name()).eq(key.get()).build();

        assertSoftly(softly -> {
            softly.assertThat(entityManager.count(query))
                    .as("should count documents matching the query")
                    .isEqualTo(1L);
            softly.assertThatThrownBy(() -> entityManager.count((SelectQuery) null))
                    .as("must not accept null query")
                    .isInstanceOf(NullPointerException.class);
        });
    }

    @Test
    void shouldReadFromDifferentBaseDocumentUsingInstance() {
        entityManager.insert(getEntity());
        ArangoDB arangoDB = DefaultArangoDBDocumentManager.class.cast(entityManager).getArangoDB();
        arangoDB.db(DATABASE).collection(COLLECTION_NAME).insertDocument(new Human());
        SelectQuery select = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(select).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldReadFromDifferentBaseDocumentUsingMap() {
        entityManager.insert(getEntity());
        ArangoDB arangoDB = DefaultArangoDBDocumentManager.class.cast(entityManager).getArangoDB();
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        arangoDB.db(DATABASE).collection(COLLECTION_NAME).insertDocument(map);
        SelectQuery select = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(select).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldExecuteAQLWithTypeParams() {
        entityManager.insert(getEntity());
        String aql = "FOR a IN person FILTER a.name == @name RETURN a.name";
        List<String> entities = entityManager.aql(aql,
                singletonMap("name", "Poliana"), String.class).toList();

        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldExecuteAQLWithType() {
        entityManager.insert(getEntity());
        String aql = "FOR a IN person RETURN a.name";
        List<String> entities = entityManager.aql(aql, String.class).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    void shouldInsertNull() {
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_NAME);
        entity.add(Element.of(KEY_NAME, String.valueOf(random.nextLong())));
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
    void shouldUpdateNull(){
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
    void shouldUpdateByQuery(){
        var attributesNames = new LinkedHashSet<String>();

        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
            entity.elements().forEach(e -> attributesNames.add(e.name()));
        }
        var index = 4;

        final var updateQuery = mock(UpdateQuery.class);
        when(updateQuery.name()).thenReturn(COLLECTION_NAME);
        when(updateQuery.condition()).thenReturn(Optional.of(CriteriaCondition.gt(Element.of("index", index))));
        when(updateQuery.set()).then(inv -> List.of(Element.of("country", "Brazil")));
        when(updateQuery.toSelectQuery()).then(inv -> select().from(COLLECTION_NAME).where("index").gt(index).build());

        var entities = entityManager.update(updateQuery);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(5);
            softly.assertThat(entities)
                    .allMatch(e -> e.find("country").orElseThrow().get().equals("Brazil"))
                    .allSatisfy( e-> {
                        for (String attributeName : attributesNames) {
                            softly.assertThat(e.find(attributeName))
                                    .as("Attribute " + attributeName + " should be present")
                                    .isPresent();
                        }
                    });

        });
    }

    @Test
    void shouldDeleteAll() {
        for (int index = 0; index < 20; index++) {
            var entity = getEntity();
            entityManager.insert(entity);
        }
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
        SelectQuery select = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(select).toList();
        assertThat(entities).isEmpty();
    }


    @Test
    void shouldIncludeLimit() {
        for (int index = 0; index < 20; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var select = select().from(COLLECTION_NAME).orderBy("index").asc().limit(4).build();
        var entities = entityManager.select(select).toList();
        var indexes = entities.stream().map(e -> e.find("index").orElseThrow().get()).toList();
        org.assertj.core.api.Assertions.assertThat(indexes).hasSize(4).contains(0, 1, 2, 3);
        DeleteQuery deleteQuery = delete().from(COLLECTION_NAME).build();
        entityManager.delete(deleteQuery);
    }

    @Test
    void shouldIncludeSkipLimit() {
        for (int index = 0; index < 20; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var select = select().from(COLLECTION_NAME).orderBy("index").asc().skip(3).limit(4).build();
        var entities = entityManager.select(select).toList();
        var indexes = entities.stream().map(e -> e.find("index").orElseThrow().get()).toList();
        org.assertj.core.api.Assertions.assertThat(indexes).hasSize(4).contains(3, 4, 5, 6);
    }

    @Test
    void shouldIncludeSkip() {
        for (int index = 0; index < 20; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var select = select().from(COLLECTION_NAME).orderBy("index").asc().skip(5).build();
        var entities = entityManager.select(select).toList();
        var indexes = entities.stream().map(e -> e.find("index").orElseThrow().get()).toList();
        org.assertj.core.api.Assertions.assertThat(indexes).hasSize(15);
    }

    @Test
    void shouldExposeArangoDB() {
        ArangoDB adb = entityManager.getArangoDB();
        assertThat(adb).isNotNull();
        assertThat(adb.getVersion()).isNotNull();
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
    void shouldFindBetween() {
        var deleteQuery = delete().from(COLLECTION_NAME).where("type").eq("V").build();
        entityManager.delete(deleteQuery);
        entityManager.insert(getEntitiesWithValues());

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
        entityManager.insert(getEntitiesWithValues());

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
        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_NAME);
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Poliana");
        map.put("city", "Salvador");
        entity.add(Element.of(KEY_NAME, String.valueOf(random.nextLong())));
        List<Element> documents = Elements.of(map);
        documents.forEach(entity::add);
        return entity;
    }

    private CommunicationEntity createDocumentList() {
        String id = UUID.randomUUID().toString();
        CommunicationEntity entity = CommunicationEntity.of("AppointmentBook");
        entity.add(Element.of("_key", id));
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

    private CommunicationEntity createDocumentListNotHavingId() {
        CommunicationEntity entity = CommunicationEntity.of("AppointmentBook");
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
