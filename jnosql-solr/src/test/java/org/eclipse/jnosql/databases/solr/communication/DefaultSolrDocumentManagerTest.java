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

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
@DisplayName("Default Solr Document Manager")
public class DefaultSolrDocumentManagerTest {

    public static final String COLLECTION_NAME = "person";
    public static final String ID = "_id";
    private static final AtomicLong IDS = new AtomicLong(System.currentTimeMillis());
    private static SolrDocumentManager entityManager;

    @BeforeAll
    public static void setUp() {
        entityManager = DocumentDatabase.INSTANCE.get();
    }


    @Nested
    @DisplayName("When inserting documents at the database")
    class WhenInsertingAtDatabase {

        @Test
        @DisplayName("Should insert an entity and preserve its identifier field")
        void shouldInsert() {
            var entity = getEntity();
            var documentEntity = entityManager.insert(entity);
            assertThat(documentEntity.elements().stream().map(Element::name))
                    .contains(ID);
        }

        @Test
        @DisplayName("Should reject insert with TTL because Solr does not support TTL saves")
        void shouldThrowExceptionWhenInsertWithTTL() {
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> entityManager.insert(getEntity(), Duration.ofSeconds(10)));
        }

        @Test
        @DisplayName("Should reject null entity on insert")
        void shouldThrowExceptionWhenInsertIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> entityManager.insert((CommunicationEntity) null));
        }

        @Test
        @DisplayName("Should insert an entity with a null field value")
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
        @DisplayName("Should reject embedded sub-document fields during insert")
        void shouldReturnErrorWhenSaveSubDocument() {
            var entity = getEntity();
            entity.add(Element.of("phones", Element.of("mobile", "1231231")));
            assertThatExceptionOfType(SolrException.class)
                    .isThrownBy(() -> entityManager.insert(entity));
        }
    }

    @Nested
    @DisplayName("When updating documents at the database")
    class WhenUpdatingAtDatabase {

        @Test
        @DisplayName("Should update an existing entity by deleting and reinserting the identifier")
        void shouldUpdateSave() {
            var entity = getEntity();
            entityManager.insert(entity);
            var newField = Elements.of("newField", "10");
            entity.add(newField);
            var updated = entityManager.update(entity);
            assertThat(updated.find("newField")).get().isEqualTo(newField);
        }

        @Test
        @DisplayName("Should reject null entity on update")
        void shouldThrowExceptionWhenUpdateIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> entityManager.update((CommunicationEntity) null));
        }

        @Test
        @DisplayName("Should update an entity with a null field value")
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
    }

    @Nested
    @DisplayName("When deleting documents from the database")
    class WhenDeletingFromDatabase {

        @Test
        @DisplayName("Should remove an entity selected by its identifier")
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
            assertThat(entityManager.select(query).findAny()).isEmpty();
        }

        @Test
        @DisplayName("Should reject null delete query")
        void shouldThrowExceptionWhenDeleteQueryIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> entityManager.delete(null));
        }
    }

    @Nested
    @DisplayName("When selecting documents from the database")
    class WhenSelectingFromDatabase {

        @Test
        @DisplayName("Should find a document by identifier")
        void shouldFindDocument() {
            CommunicationEntity entity = entityManager.insert(getEntity());
            Optional<Element> id = entity.find(ID);

            var query = select().from(COLLECTION_NAME)
                    .where(ID).eq(id.get().get())
                    .build();

            List<CommunicationEntity> entities = entityManager.select(query).toList();
            assertThat(entities).isNotEmpty();
            final CommunicationEntity result = entities.getFirst();

            assertThat(result.find("name")).get().isEqualTo(entity.find("name").get());
            assertThat(result.find("city")).get().isEqualTo(entity.find("city").get());
        }

        @Test
        @DisplayName("Should find a document with multiple AND conditions")
        void shouldFindDocument2() {
            var entity = entityManager.insert(getEntity());
            Optional<Element> id = entity.find(ID);

            var query = select().from(COLLECTION_NAME)
                    .where("name").eq("Poliana")
                    .and("city").eq("Salvador").and(ID).eq(id.get().get())
                    .build();

            List<CommunicationEntity> entities = entityManager.select(query).toList();
            assertThat(entities).isNotEmpty();
            final CommunicationEntity result = entities.getFirst();

            assertThat(result.find("name")).get().isEqualTo(entity.find("name").get());
            assertThat(result.find("city")).get().isEqualTo(entity.find("city").get());
        }

        @Test
        @DisplayName("Should find a document with OR and AND conditions")
        void shouldFindDocument3() {
            var entity = entityManager.insert(getEntity());
            Optional<Element> id = entity.find(ID);
            var query = select().from(COLLECTION_NAME)
                    .where("name").eq("Poliana")
                    .or("city").eq("Salvador")
                    .and(id.get().name()).eq(id.get().get())
                    .build();

            List<CommunicationEntity> entities = entityManager.select(query).toList();
            assertThat(entities).isNotEmpty();
            final CommunicationEntity result = entities.getFirst();
            assertThat(result.find("name")).get().isEqualTo(entity.find("name").get());
            assertThat(result.find("city")).get().isEqualTo(entity.find("city").get());
        }

        @Test
        @DisplayName("Should find documents with a greater-than condition")
        void shouldFindDocumentGreaterThan() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

            var query = select().from(COLLECTION_NAME)
                    .where("age").gt(22)
                    .and("type").eq("V")
                    .build();

            List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
            assertThat(entitiesFound).hasSize(3);
        }

        @Test
        @DisplayName("Should find documents that do not match a condition")
        void shouldFindNot() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());
            var query = select().from(COLLECTION_NAME)
                    .where("name").not().eq("Lucas").build();
            List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
            assertThat(entitiesFound).hasSize(2);
        }

        @Test
        @DisplayName("Should find documents with a greater-than-or-equal condition")
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
            assertThat(entitiesFound).hasSize(2);
            assertThat(entitiesFound).isNotIn(entities.getFirst());
        }

        @Test
        @DisplayName("Should find documents with a less-than condition")
        void shouldFindDocumentLesserThan() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

            var query = select().from(COLLECTION_NAME)
                    .where("age").lt(23)
                    .and("type").eq("V")
                    .build();

            List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
            assertThat(entitiesFound).hasSize(2);
        }

        @Test
        @DisplayName("Should find documents with a less-than-or-equal condition")
        void shouldFindDocumentLesserEqualsThan() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

            var query = select().from(COLLECTION_NAME)
                    .where("age").lte(23)
                    .and("type").eq("V")
                    .build();

            List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
            assertThat(entitiesFound).hasSize(2);
        }

        @Test
        @DisplayName("Should find documents with LIKE wildcard matching")
        void shouldFindDocumentLike() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

            var query = select().from(COLLECTION_NAME)
                    .where("name").like("Lu*")
                    .and("type").eq("V")
                    .build();

            List<CommunicationEntity> entitiesFound = entityManager.select(query).toList();
            assertThat(entitiesFound).hasSize(2);
        }

        @Test
        @DisplayName("Should find documents with IN matching")
        void shouldFindDocumentIn() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

            var query = select().from(COLLECTION_NAME)
                    .where("location").in(asList("BR", "US"))
                    .and("type").eq("V")
                    .build();

            assertThat(entityManager.select(query).count()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should skip matching documents when start offset is set")
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
            assertThat(entitiesFound).hasSize(2);
            assertThat(entitiesFound).isNotIn(entities.getFirst());

            query = select().from(COLLECTION_NAME)
                    .where("age").gt(22)
                    .and("type").eq("V")
                    .skip(3L)
                    .build();

            entitiesFound = entityManager.select(query).collect(Collectors.toList());
            assertThat(entitiesFound).isEmpty();
        }

        @Test
        @DisplayName("Should limit the number of matching documents")
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
            assertThat(entitiesFound).hasSize(1);
            assertThat(entitiesFound).isNotIn(entities.getFirst());

            query = select().from(COLLECTION_NAME)
                    .where("age").gt(22)
                    .and("type").eq("V")
                    .limit(2L)
                    .build();

            entitiesFound = entityManager.select(query).collect(Collectors.toList());
            assertThat(entitiesFound).hasSize(2);
            entityManager.delete(deleteQuery);
        }

        @Test
        @DisplayName("Should sort matching documents by age")
        void shouldFindDocumentSort() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

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
        @DisplayName("Should find all documents in the collection")
        void shouldFindAll() {
            entityManager.insert(getEntity());
            var query = select().from(COLLECTION_NAME).build();
            List<CommunicationEntity> entities = entityManager.select(query).toList();
            assertThat(entities).isNotEmpty();
        }

        @Test
        @DisplayName("Should reject null select query")
        void shouldThrowExceptionWhenSelectQueryIsNull() {
            assertThatNullPointerException()
                    .isThrownBy(() -> entityManager.select(null));
        }
    }

    @Nested
    @DisplayName("When executing native Solr queries")
    class WhenExecutingNativeSolrQuery {

        @Test
        @DisplayName("Should execute a native Solr query")
        void shouldExecuteNativeQuery() {
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

            List<CommunicationEntity> entitiesFound = entityManager.solr("age:22 AND type:V AND _entity:person");
            assertThat(entitiesFound).hasSize(1);
        }

        @Test
        @DisplayName("Should execute a native Solr query with parameters")
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
            assertThat(entitiesFound).hasSize(1);
        }

        @Test
        @DisplayName("Should replace all occurrences of a native Solr query parameter")
        void shouldExecuteNativeQueryParamsReplaceAll() {
            entityManager.insert(getEntitiesWithValues());
            var deleteQuery = delete().from(COLLECTION_NAME).build();
            entityManager.delete(deleteQuery);
            entityManager.insert(getEntitiesWithValues());

            Map<String, Object> params = new HashMap<>();
            params.put("age", 22);

            List<CommunicationEntity> entitiesFound = entityManager.solr("age:@age AND age:@age", params);
            assertThat(entitiesFound).hasSize(1);
        }
    }

    @Nested
    @DisplayName("When counting documents in the database")
    class WhenCountingDocuments {

        @Test
        @DisplayName("Should count documents by collection name")
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
        @DisplayName("Should count documents by select query")
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
    }

    @Nested
    @DisplayName("When handling temporal fields")
    class WhenHandlingTemporalFields {

        @Test
        @DisplayName("Should create and read date values")
        void shouldCreateDate() {
            Date date = new Date();
            LocalDate now = LocalDate.now();

            var entity = CommunicationEntity.of("download");
            long id = nextId();
            entity.add(ID, id);
            entity.add("date", date);
            entity.add("now", now);

            entityManager.insert(entity);

            List<CommunicationEntity> entities = entityManager.select(select().from("download")
                    .where(ID).eq(id).build()).toList();

            assertThat(entities).hasSize(1);
            var documentEntity = entities.getFirst();
            assertThat(documentEntity.find("date")).get()
                    .extracting(element -> element.get(Date.class))
                    .isEqualTo(date);
            assertThat(documentEntity.find("date")).get()
                    .extracting(element -> element.get(LocalDate.class))
                    .isEqualTo(now);
        }
    }

    private CommunicationEntity createSubdocumentList() {
        CommunicationEntity entity = CommunicationEntity.of("AppointmentBook");
        entity.add(Element.of(ID, nextId()));
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
        map.put(ID, nextId());
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

    private long nextId() {
        return IDS.incrementAndGet();
    }

}
