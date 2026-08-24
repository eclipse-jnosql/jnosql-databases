/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.couchbase.communication;


import com.couchbase.client.core.env.TimeoutConfig;
import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class DocumentQueryTest {

    public static final String COLLECTION_NAME = "person";
    private static CouchbaseDocumentManager entityManager;
    private static BucketManager keyValueEntityManager;

    static {
        var settings = Database.INSTANCE.getSettings();
        var configuration = Database.INSTANCE.getDocumentConfiguration();
        CouchbaseDocumentManagerFactory managerFactory = configuration.apply(settings);
        entityManager = managerFactory.apply(CouchbaseUtil.BUCKET_NAME);
        BucketManagerFactory keyValueEntityManagerFactory =
                Database.INSTANCE.getKeyValueConfiguration().apply(settings);
        keyValueEntityManager = keyValueEntityManagerFactory.apply(CouchbaseUtil.BUCKET_NAME);
    }

    @AfterEach
    public void afterEach() {
        List.of("id", "id2", "id3", "id4")
                .forEach(key -> ignoreException(() -> keyValueEntityManager.delete(key)));

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);
    }


    @BeforeEach
    public void beforeEach() {

        CommunicationEntity entity = CommunicationEntity.of("person", asList(Element.of("_id", "id")
                , Element.of("name", "name")));
        CommunicationEntity entity2 = CommunicationEntity.of("person", asList(Element.of("_id", "id2")
                , Element.of("name", "name")));
        CommunicationEntity entity3 = CommunicationEntity.of("person", asList(Element.of("_id", "id3")
                , Element.of("name", "name")));
        CommunicationEntity entity4 = CommunicationEntity.of("person", asList(Element.of("_id", "id4")
                , Element.of("name", "name3")));

        entityManager.update(Arrays.asList(entity, entity2, entity3, entity4));

        await().atLeast(TimeoutConfig.DEFAULT_KV_DURABLE_TIMEOUT);
    }

    private void ignoreException(Runnable runnable) {
        try {
            runnable.run();
        } catch (DocumentNotFoundException
                 | DocumentExistsException ex) {
            //IGNORED
        }
    }

    @Test
    public void shouldShouldDefineLimit() {

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("name").eq("name")
                .limit(2L)
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertEquals(2, entities.size());

    }

    @Test
    public void shouldShouldDefineStart() {
        SelectQuery query = select().from(COLLECTION_NAME)
                .where("name").eq("name")
                .skip(1L)
                .build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertEquals(2, entities.size());

    }

    @Test
    public void shouldShouldDefineLimitAndStart() {

        List<CommunicationEntity> entities = entityManager.select(select().from(COLLECTION_NAME).build()).collect(Collectors.toList());
        assertEquals(4, entities.size());

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("name").eq("name")
                .skip(1L)
                .limit(2L)
                .build();

        entities = entityManager.select(query).toList();
        assertEquals(2, entities.size());

    }


    @Test
    public void shouldSelectAll() {
        SelectQuery query = select().from(COLLECTION_NAME).build();
        List<CommunicationEntity> entities = entityManager.select(query).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).hasSize(4);
    }


    @Test
    public void shouldFindDocumentByName() {

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("name")
                .eq("name")
                .build();
        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());
    }

    @Test
    public void shouldFindDocumentByNameSortAsc() {

        SelectQuery query = select().from(COLLECTION_NAME)
                .orderBy("name").asc()
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        List<String> result = entities.stream()
                .flatMap(e -> e.elements().stream())
                .filter(d -> "name".equals(d.name()))
                .map(d -> d.get(String.class))
                .collect(Collectors.toList());

        assertFalse(result.isEmpty());
        assertThat(result).contains("name", "name", "name", "name3");
    }

    @Test
    public void shouldFindDocumentByNameSortDesc() {

        SelectQuery query = select().from(COLLECTION_NAME)
                .orderBy("name").desc()
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();

        List<String> result = entities.stream().flatMap(e -> e.elements().stream())
                .filter(d -> "name".equals(d.name()))
                .map(d -> d.get(String.class))
                .collect(Collectors.toList());

        assertFalse(result.isEmpty());
        assertThat(result).contains("name3", "name", "name", "name");
    }

    @Test
    public void shouldFindDocumentById() {

        SelectQuery query = select().from(COLLECTION_NAME)
                .where("_id").eq("id")
                .build();

        List<CommunicationEntity> entities = entityManager.select(query).toList();
        assertFalse(entities.isEmpty());

    }

}
