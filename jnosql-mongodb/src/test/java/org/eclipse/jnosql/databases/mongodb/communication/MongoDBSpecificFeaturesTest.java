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
package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import org.bson.BsonValue;
import org.bson.conversions.Bson;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class MongoDBSpecificFeaturesTest {

    public static final String COLLECTION_NAME = "person";
    private static MongoDBDocumentManager entityManager;

    @BeforeAll
    public static void setUp() {
        entityManager = DocumentDatabase.INSTANCE.get("database");
    }

    @BeforeEach
    void beforeEach() {
        DeleteQuery.delete().from(COLLECTION_NAME).delete(entityManager);
    }

    @Test
    void shouldReturnErrorOnSelectWhenThereIsNullParameter() {
        Bson filter = eq("name", "Poliana");

        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.select(null, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.select(COLLECTION_NAME, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.select(null, filter));
    }

    @Test
    void shouldFindDocument() {
        CommunicationEntity entity = entityManager.insert(getEntity());

        List<CommunicationEntity> entities = entityManager.select(COLLECTION_NAME,
                eq("name", "Poliana")).collect(Collectors.toList());
        assertFalse(entities.isEmpty());
        assertThat(entities).contains(entity);
    }

    @Test
    void shouldReturnErrorOnDeleteWhenThereIsNullParameter() {
        Bson filter = eq("name", "Poliana");

        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.delete(null, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.delete(COLLECTION_NAME, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.delete(null, filter));
    }

    @Test
    void shouldDelete() {
        entityManager.insert(getEntity());

        long result = entityManager.delete(COLLECTION_NAME,
                eq("name", "Poliana"));

        Assertions.assertEquals(1L, result);
        List<CommunicationEntity> entities = entityManager.select(COLLECTION_NAME,
                eq("name", "Poliana")).toList();
        Assertions.assertTrue(entities.isEmpty());
    }

    @Test
    void shouldReturnErrorOnAggregateWhenThereIsNullParameter() {
        Bson bson = eq("name", "Poliana");
        List<Bson> pipeline = Collections.singletonList(bson);
        var pipelineArray = new Bson[]{bson};

        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.aggregate(null, (List<Bson>) null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.aggregate(COLLECTION_NAME, (List<Bson>) null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.aggregate(null, (Bson[]) null));
    }

    @Test
    void shouldAggregate() {
        Bson[] predicates = {
                Aggregates.match(eq("name", "Poliana")),
                Aggregates.group("$stars", Accumulators.sum("count", 1))
        };
        entityManager.insert(getEntity());
        Stream<Map<String, BsonValue>> aggregate = entityManager.aggregate(COLLECTION_NAME, predicates);
        Assertions.assertNotNull(aggregate);
        Map<String, BsonValue> result = aggregate.findFirst()
                .orElseThrow(() -> new IllegalStateException("There is an issue with the aggregate test result"));

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
        BsonValue count = result.get("count");
        Assertions.assertEquals(1L, count.asNumber().longValue());

    }

    @Test
    void shouldAggregateEntity() {
        List<Bson> predicates = Arrays.asList(
                Aggregates.match(eq("name", "Poliana")),
                Aggregates.limit(1)
        );
        entityManager.insert(getEntity());
        Stream<CommunicationEntity> aggregate = entityManager.aggregate(COLLECTION_NAME, predicates);
        Assertions.assertNotNull(aggregate);
        CommunicationEntity result = aggregate.findFirst()
                .orElseThrow(() -> new IllegalStateException("There is an issue with the aggregate test result"));

        Assertions.assertNotNull(result);
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

    @Test
    void shouldCountWithFilter() {

        entityManager.insert(getEntity());
        var filter = eq("name", "Poliana");
        Assertions.assertEquals(1L, entityManager.count(COLLECTION_NAME, filter));

        var filter2 = and(filter, eq("city", "Salvador"));
        Assertions.assertEquals(1L, entityManager.count(COLLECTION_NAME, filter2));

        var filter3 = and(filter, eq("city", "São Paulo"));
        Assertions.assertEquals(0L, entityManager.count(COLLECTION_NAME, filter3));

    }

    @Test
    void shouldReturnErrorOnCountWithInvalidFilter() {

        Bson filter = eq("name", "Poliana");

        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.count(null, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.count(COLLECTION_NAME, null));
        Assertions.assertThrows(NullPointerException.class,
                () -> entityManager.count(null, filter));

    }


}
