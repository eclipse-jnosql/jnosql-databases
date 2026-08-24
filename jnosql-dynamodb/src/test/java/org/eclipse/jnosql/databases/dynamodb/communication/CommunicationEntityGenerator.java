/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import net.datafaker.Faker;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

final class CommunicationEntityGenerator {

    static final String COLLECTION_NAME = "entityA";
    static final Faker faker = new Faker();

    static CommunicationEntity createRandomEntity() {
        return createRandomEntityWithSubDocuments(0);
    }

    static CommunicationEntity createRandomEntity(String collectionName) {
        return createRandomEntityWithSubDocuments(collectionName,0);
    }

    static CommunicationEntity createRandomEntityWithSubDocuments(int levels) {
        return createRandomEntityWithSubDocuments(COLLECTION_NAME, levels);
    }

    @NotNull
    private static CommunicationEntity createRandomEntityWithSubDocuments(String collectionName, int levels) {
        Map<String, Object> map = new HashMap<>();
        map.put(DynamoDBConverter.ID, UUID.randomUUID().toString());
        map.put("name", faker.name().firstName());
        map.put("hoje", LocalDate.now());
        map.put("agora", LocalDateTime.now());
        map.put("guessingNumber", faker.random().nextInt(1, 10));
        map.put("bigdecimal", BigDecimal.valueOf(10.10));
        map.put("city", faker.address().city());
        map.put("texts", List.of("A", "B", "C"));
        if (levels > 0) {
            addSubDocument(m -> map.put("level" + levels, m), levels - 1);
        }
        var entity = CommunicationEntity.of(collectionName);
        List<Element> documents = Elements.of(map);
        documents.forEach(entity::add);
        return entity;
    }

    static void addSubDocument(Consumer<Map<String, Object>> owner, int level) {
        Map<String, Object> map = new HashMap<>();
        map.put("level", level);
        map.put("text", UUID.randomUUID().toString());
        map.put("hoje", LocalDate.now());
        map.put("agora", LocalDateTime.now());
        map.put("integerNumber", faker.random().nextInt(1, 10));
        map.put("floatNumber", (float) faker.random().nextDouble(1.0, 10.0));
        map.put("doubleNumber", faker.random().nextDouble(1.0, 10.0));
        map.put("bigdecimal", BigDecimal.valueOf(10.10));
        if (level > 0) {
            addSubDocument(m -> map.put("level" + level, m), level - 1);
        }
        owner.accept(map);
    }
}