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
package org.eclipse.jnosql.databases.elasticsearch.communication;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.awaitility.Awaitility;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class ElasticsearchDocumentManagerTest {

    private ElasticsearchDocumentManager entityManager;

    static {
        Awaitility.setDefaultPollDelay(100, MILLISECONDS);
        Awaitility.setDefaultTimeout(2L, SECONDS);
    }

    @NotNull
    private Callable<Long> numberOfEntitiesFrom(SearchRequest query) {
        return () -> entityManager.search(query).count();
    }


    @NotNull
    private Callable<Long> numberOfEntitiesFrom(SelectQuery query) {
        return () -> entityManager.select(query).count();
    }

    @NotNull
    private Callable<CommunicationEntity> getSingleResult(SelectQuery query) {
        return () -> entityManager.singleResult(query).orElse(null);
    }

    @NotNull
    private Callable<List<CommunicationEntity>> selectFrom(SelectQuery query) {
        return () -> entityManager.select(query).collect(Collectors.toList());
    }

    @BeforeEach
    public void setUp() {

        ElasticsearchDocumentManagerFactory managerFactory = DocumentDatabase.INSTANCE.get();
        entityManager = managerFactory.apply(DocumentEntityGerator.INDEX);

        DeleteQuery deleteQuery = delete().from(DocumentEntityGerator.COLLECTION_NAME).build();

        entityManager.delete(deleteQuery);

        SelectQuery query = select().from(DocumentEntityGerator.COLLECTION_NAME).build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(0L));
    }

    @Test
    public void shouldClose() {
        entityManager.close();
        assertThrows(RuntimeException.class,
                () -> entityManager.insert(DocumentEntityGerator.getEntity()));
    }

    @Test
    public void shouldInsert() {
        var entity = DocumentEntityGerator.getEntity();
        var documentEntity = entityManager.insert(entity);
        assertEquals(entity, documentEntity);

        var id = documentEntity.find(EntityConverter.ID_FIELD).get();
        var query = SelectQuery.select()
                .from(documentEntity.name())
                .where(id.name()).eq(id.value())
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(getSingleResult(query), equalTo(documentEntity));
    }

    @Test
    public void shouldInsertTTL() {
        assertThrows(UnsupportedOperationException.class, () -> entityManager.insert(DocumentEntityGerator.getEntity(), Duration.ofSeconds(1L)));
    }

    @Test
    public void shouldReturnAll() {
        var entity = DocumentEntityGerator.getEntity();
        entityManager.insert(entity);

        var query = select().
                from(DocumentEntityGerator.COLLECTION_NAME)
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), greaterThan(0L));
    }


    @Test
    public void shouldUpdateSave() {
        var entity = DocumentEntityGerator.getEntity();
        entityManager.insert(entity);

        var id = entity.find(EntityConverter.ID_FIELD).get();

        var query = select()
                .from(DocumentEntityGerator.COLLECTION_NAME)
                .where(id.name())
                .eq(id.get())
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));

        var newField = Elements.of("newField", "10");
        entity.add(newField);
        var updated = entityManager.update(entity);
        assertEquals(newField, updated.find("newField").get());

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));
    }

    @Test
    public void shouldUserSearchRequest() {
        var entity = DocumentEntityGerator.getEntity();
        entityManager.insert(entity);

        var query = SearchRequest.of(b -> b
                .index(DocumentEntityGerator.COLLECTION_NAME)
                .query(q -> q.term(tq -> tq.field("name").value("Poliana"))));

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query),equalTo(1L));

        assertThat(entityManager.search(query)
                .collect(Collectors.toList())).contains(entity);

    }


    @Test
    public void shouldRemoveEntityByName() {
        var documentEntity = entityManager.insert(DocumentEntityGerator.getEntity());
        var name = documentEntity.find("name").get();
        var query = select().from(DocumentEntityGerator.COLLECTION_NAME).where(name.name()).eq(name.get()).build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));

        var deleteQuery = delete().from(DocumentEntityGerator.COLLECTION_NAME).where(name.name()).eq(name.get()).build();
        entityManager.delete(deleteQuery);

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(0L));

    }

    @Test
    public void shouldRemoveEntityById() {
        var documentEntity = entityManager.insert(DocumentEntityGerator.getEntity());

        var id = documentEntity.find("_id").get();
        var query = select().from(DocumentEntityGerator.COLLECTION_NAME).where(id.name()).eq(id.get()).build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));


        var deleteQuery = delete().from(DocumentEntityGerator.COLLECTION_NAME).where(id.name()).eq(id.get()).build();
        entityManager.delete(deleteQuery);

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(0L));

    }

    @Test
    public void shouldFindDocumentByName() {
        var entity = entityManager.insert(DocumentEntityGerator.getEntity());
        var name = entity.find("name").get();
        var query = select().
                from(DocumentEntityGerator.COLLECTION_NAME).
                where(name.name())
                .eq(name.get())
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));

        var entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
        var names = entities.stream().map(e -> e.find("name").get())
                .distinct().collect(Collectors.toList());
        assertThat(names).contains(name);
    }

    @Test
    public void shouldFindDocumentById() {
        var entity = entityManager.insert(DocumentEntityGerator.getEntity());
        var id = entity.find("_id").get();

        var query = select()
                .from(DocumentEntityGerator.COLLECTION_NAME)
                .where(id.name()).eq(id.get())
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(selectFrom(query), contains(entity));

    }

    @Test
    public void shouldFindOrderByName() {
        var poliana = DocumentEntityGerator.getEntity();
        var otavio = DocumentEntityGerator.getEntity();
        poliana.add("name", "poliana");
        otavio.add("name", "otavio");
        otavio.add("_id", "id2");
        entityManager.insert(Arrays.asList(poliana, otavio));

        var query = SelectQuery.select()
                .from(DocumentEntityGerator.COLLECTION_NAME)
                .orderBy("name").asc()
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(2L));

        String[] names = entityManager.select(query)
                .map(d -> d.find("name"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(d -> d.get(String.class))
                .toArray(String[]::new);

        assertThat(names).containsExactly("otavio", "poliana");
    }

    @Test
    public void shouldFindOrderByNameDesc() {
        var poliana = DocumentEntityGerator.getEntity();
        var otavio = DocumentEntityGerator.getEntity();
        poliana.add("name", "poliana");
        otavio.add("name", "otavio");
        otavio.add("_id", "id2");
        entityManager.insert(Arrays.asList(poliana, otavio));

        var query = SelectQuery.select()
                .from(DocumentEntityGerator.COLLECTION_NAME)
                .orderBy("name").desc()
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(2L));

        String[] names = entityManager.select(query)
                .map(d -> d.find("name"))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(d -> d.get(String.class))
                .toArray(String[]::new);

        assertThat(names).containsExactly("poliana", "otavio");
    }


    @Test
    public void shouldFindAll() {
        entityManager.insert(DocumentEntityGerator.getEntity());

        var query = select()
                .from(DocumentEntityGerator.COLLECTION_NAME)
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(selectFrom(query), hasSize(1));

    }


    @Test
    public void shouldSaveSubDocument() {
        var entity = DocumentEntityGerator.getEntity();
        entity.add(Element.of("phones", Element.of("mobile", "1231231")));
        var entitySaved = entityManager.insert(entity);
        var id = entitySaved.find("_id").get();

        var query = select().from(DocumentEntityGerator.COLLECTION_NAME).where(id.name()).eq(id.get()).build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));

        var entityFound = entityManager.singleResult(query).get();
        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });
        assertThat(documents).contains(Element.of("mobile", "1231231"));
    }

    @Test
    public void shouldSaveSubDocument2() {
        var entity = DocumentEntityGerator.getEntity();

        var mobile = Element.of("mobile", "1231231");
        var mobile2 = Element.of("mobile2", "1231231");

        entity.add(Element.of("phones", Arrays.
                asList(mobile, mobile2)));

        var entitySaved = entityManager.insert(entity);

        var id = entitySaved.find("_id").get();
        var query = select().
                from(DocumentEntityGerator.COLLECTION_NAME).
                where(id.name()).eq(id.get())
                .build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));

        var entityFound = entityManager.select(query)
                .toList()
                .getFirst();

        var subDocument = entityFound.find("phones").get();
        List<Element> documents = subDocument.get(new TypeReference<>() {
        });

        assertThat(documents).contains(mobile, mobile2);
    }

    @Test
    public void shouldInsertAndRetrieveListSubdocumentList() {
        var entity = entityManager.insert(createSubdocumentList());

        var key = entity.find("_id").get();
        var query = select().from(DocumentEntityGerator.COLLECTION_NAME).where(key.name()).eq(key.get()).build();

        // it's required in order to avoid an eventual inconsistency
        await().until(numberOfEntitiesFrom(query), equalTo(1L));

        var documentEntity = entityManager.singleResult(query).orElse(null);
        assertNotNull(documentEntity);

        List<List<Element>> contacts = documentEntity.find("contacts")
                .orElseThrow()
                .get(List.class);

        assertEquals(3, contacts.size());
        assertTrue(contacts.stream().allMatch(d -> d.size() == 3));
    }

    @Test
    public void shouldCount() {
        var entity = DocumentEntityGerator.getEntity();
        var entity2 = DocumentEntityGerator.getEntity();
        entity2.add(Element.of("_id", "test"));
        entityManager.insert(entity);
        entityManager.insert(entity2);

        // it's required in order to avoid an eventual inconsistency
        await().until(() -> entityManager.count(DocumentEntityGerator.COLLECTION_NAME),
                equalTo(2L));
    }

    private CommunicationEntity createSubdocumentList() {
        CommunicationEntity entity = CommunicationEntity.of(DocumentEntityGerator.COLLECTION_NAME);
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


}