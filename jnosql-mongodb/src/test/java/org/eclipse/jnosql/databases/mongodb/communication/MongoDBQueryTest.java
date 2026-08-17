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

package org.eclipse.jnosql.databases.mongodb.communication;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class MongoDBQueryTest {

    public static final String COLLECTION_NAME = "person";
    private static DatabaseManager entityManager;

    @BeforeAll
    static void setUp() {
        entityManager = DocumentDatabase.INSTANCE.get("database");
    }

    @BeforeEach
    void beforeEach() {
        delete().from(COLLECTION_NAME).delete(entityManager);
    }

    @Test
    @DisplayName("The query should execute A or B")
    void shouldQuery() {
        var entity = getEntity();
        entityManager.insert(entity);

        var query = select()
                .from(COLLECTION_NAME)
                .where("name").eq("Otavio")
                .or("name").eq("Poliana").build();

        Stream<CommunicationEntity> stream = entityManager.select(query);
        long count = stream.count();
        assertThat(count).isEqualTo(1L);
        entityManager.delete(delete().from(COLLECTION_NAME).build());

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

}
