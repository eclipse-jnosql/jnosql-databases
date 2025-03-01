/*
 *
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.neo4j.communication;

import net.datafaker.Faker;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Neo4JDatabaseManagerTest {

    public static final String COLLECTION_NAME = "person";
    private static Neo4JDatabaseManager entityManager;

    @BeforeAll
    public static void setUp() {
        entityManager = DatabaseContainer.INSTANCE.get("neo4j");
    }

    @BeforeEach
    void beforeEach() {
        removeAllEdges();
        delete().from(COLLECTION_NAME).delete(entityManager);
    }

    @Test
    void shouldInsert() {
        var entity = getEntity();
        var communicationEntity = entityManager.insert(entity);
        assertTrue(communicationEntity.elements().stream().map(Element::name).anyMatch(s -> s.equals("_id")));
    }

    @Test
    void shouldInsertEntities() {
        var entities = List.of(getEntity(), getEntity());
        var result = entityManager.insert(entities);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result).allMatch(e -> e.elements().stream().map(Element::name).anyMatch(s -> s.equals("_id")));
        });
    }

    @Test
    void shouldCount() {
        var entity = getEntity();
        entityManager.insert(entity);
        long count = entityManager.count(COLLECTION_NAME);
        assertTrue(count > 0);
    }

    @Test
    void shouldUpdate() {
        var entity = getEntity();
        var communicationEntity = entityManager.insert(entity);
        var id = communicationEntity.find("_id").orElseThrow().get();
        var update = CommunicationEntity.of(COLLECTION_NAME);
        update.add("_id", id);
        update.add("name", "Lucas");

        var result = entityManager.update(update);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotNull();
            softly.assertThat(result.find("name").orElseThrow().get()).isEqualTo("Lucas");
        });
        assertTrue(result.find("name").isPresent());
    }

    @Test
    void shouldUpdateEntities() {
        var entity = getEntity();
        var communicationEntity = entityManager.insert(entity);
        var id = communicationEntity.find("_id").orElseThrow().get();
        var update = CommunicationEntity.of(COLLECTION_NAME);
        update.add("_id", id);
        update.add("name", "Lucas");

        var result = entityManager.update(List.of(update));
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result).allMatch(e -> e.find("name").orElseThrow().get().equals("Lucas"));
        });
    }

    @Test
    void shouldSelectById() {
        var entity = getEntity();
        var communicationEntity = entityManager.insert(entity);
        var id = communicationEntity.find("_id").orElseThrow().get();
        var query = select().from(COLLECTION_NAME).where("_id").eq(id).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(1);
            softly.assertThat(entities).allMatch(e -> e.find("_id").isPresent());
        });
    }

    @Test
    void shouldSelectByIdWithName() {
        var entity = getEntity();
        var communicationEntity = entityManager.insert(entity);
        var id = communicationEntity.find("_id").orElseThrow().get();
        var query = select("name", "city").from(COLLECTION_NAME).where("_id").eq(id).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(1);
            softly.assertThat(entities.get(0).elements()).hasSize(2);
            softly.assertThat(entities.get(0).contains("name")).isTrue();
            softly.assertThat(entities.get(0).contains("city")).isTrue();
        });
    }

    @Test
    void shouldSelectLimit() {
        for (int index = 0; index < 10; index++) {
            entityManager.insert(getEntity());
        }
        var query = select().from(COLLECTION_NAME).limit(5).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(5);
        });
    }

    @Test
    void shouldSelectStart() {
        for (int index = 0; index < 10; index++) {
            entityManager.insert(getEntity());
        }
        var query = select().from(COLLECTION_NAME).skip(5).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(5);
        });
    }

    @Test
    void shouldSelectStartAndLimit() {
        for (int index = 0; index < 10; index++) {
            entityManager.insert(getEntity());
        }
        var query = select().from(COLLECTION_NAME).skip(5).limit(2).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(2);
        });
    }

    @Test
    void shouldSelectOrderAsc() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }

        var query = select().from(COLLECTION_NAME).orderBy("index").asc().build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(10);
            softly.assertThat(entities).allMatch(e -> e.find("index").isPresent());
            softly.assertThat(entities).extracting(e -> e.find("index").orElseThrow().get())
                    .containsExactly(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L);
        });
    }

    @Test
    void shouldSelectOrderDesc() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var query = select().from(COLLECTION_NAME).orderBy("index").desc().build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(10);
            softly.assertThat(entities).allMatch(e -> e.find("index").isPresent());
            softly.assertThat(entities).extracting(e -> e.find("index").orElseThrow().get())
                    .containsExactly(9L, 8L, 7L, 6L, 5L, 4L, 3L, 2L, 1L, 0L);
        });
    }

    @Test
    void shouldSelectFindEquals() {
        var entity = getEntity();
        entityManager.insert(entity);
        var query = select().from(COLLECTION_NAME).where("name").eq(entity.find("name").orElseThrow().get()).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(1);
            softly.assertThat(entities).allMatch(e -> e.find("name").isPresent());
        });
    }

    @Test
    void shouldSelectFindNotEquals() {
        var entity = getEntity();
        entityManager.insert(entity);
        entityManager.insert(getEntity());
        Object name = entity.find("name").orElseThrow().get();
        var query = select().from(COLLECTION_NAME).where("name").not()
                .eq(name).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(1);
            softly.assertThat(entities).allMatch(e -> !e.find("name").orElseThrow().get().equals(name));
        });
    }

    @Test
    void shouldSelectGreaterThan() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var query = select().from(COLLECTION_NAME).where("index").gt(index).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(5);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) > index);
        });
    }

    @Test
    void shouldSelectGreaterThanEqual() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var query = select().from(COLLECTION_NAME).where("index").gte(index).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(6);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) >= index);
        });
    }

    @Test
    void shouldSelectLesserThan() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var query = select().from(COLLECTION_NAME).where("index").lt(index).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(4);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) < index);
        });
    }

    @Test
    void shouldSelectLesserThanEqual() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var query = select().from(COLLECTION_NAME).where("index").lte(index).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(5);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) <= index);
        });
    }

    @Test
    void shouldSelectFindIn() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var query = select().from(COLLECTION_NAME).where("index").in(List.of(1, 2, 3)).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(3);
            softly.assertThat(entities).allMatch(e -> Stream.of(1, 2, 3)
                    .anyMatch(i -> i.equals(e.find("index").orElseThrow().get(Integer.class))));
        });
    }

    @Test
    void shouldSelectLike() {
        var entity = getEntity();
        entity.add("name", "Ada Lovelace");
        entityManager.insert(entity);
        var query = select().from(COLLECTION_NAME).where("name").like("Love").build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(1);
            softly.assertThat(entities).allMatch(e -> e.find("name").orElseThrow().get().toString().contains("Love"));
        });
    }

    @Test
    void shouldSelectAnd() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var entity = getEntity();
        entity.add("index", index);
        entityManager.insert(entity);
        var query = select().from(COLLECTION_NAME).where("index")
                .gte(index).and("name")
                .eq(entity.find("name")
                        .orElseThrow().get()).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(1);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) <= index);
        });
    }

    @Test
    void shouldSelectOr() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var entity = getEntity();
        entity.add("index", index);
        entityManager.insert(entity);
        var name = entity.find("name")
                .orElseThrow().get(String.class);
        var query = select().from(COLLECTION_NAME).where("index")
                .gte(index).or("name")
                .eq(name).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(7);
            Predicate<CommunicationEntity> get = e -> e.find("index").orElseThrow().get(Integer.class) >= index;
            Predicate<CommunicationEntity> eq = e -> e.find("name").orElseThrow().get(String.class).equals(name);
            softly.assertThat(entities).allMatch(get.or(eq));
        });
    }

    @Test
    void shouldDeleteById() {
        var entity = getEntity();
        var communicationEntity = entityManager.insert(entity);
        var id = communicationEntity.find("_id").orElseThrow().get();
        var deleteQuery = delete().from(COLLECTION_NAME).where("_id").eq(id).build();
        entityManager.delete(deleteQuery);
        var query = select().from(COLLECTION_NAME).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).isEmpty();
        });
    }

    @Test
    void shouldDeleteGreaterThan() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var deleteQuery = delete().from(COLLECTION_NAME).where("index").gt(index).build();
        entityManager.delete(deleteQuery);
        var query = select().from(COLLECTION_NAME).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(5);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) <= index);
        });
    }

    @Test
    void shouldDeleteGreaterThanEqual() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var deleteQuery = delete().from(COLLECTION_NAME).where("index").gte(index).build();
        entityManager.delete(deleteQuery);
        var query = select().from(COLLECTION_NAME).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(4);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) < index);
        });
    }

    @Test
    void shouldDeleteLesserThan() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var deleteQuery = delete().from(COLLECTION_NAME).where("index").lt(index).build();
        entityManager.delete(deleteQuery);
        var query = select().from(COLLECTION_NAME).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(6);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) >= index);
        });
    }

    @Test
    void shouldDeleteLesserThanEqual() {
        for (int index = 0; index < 10; index++) {
            var entity = getEntity();
            entity.add("index", index);
            entityManager.insert(entity);
        }
        var index = 4;
        var deleteQuery = delete().from(COLLECTION_NAME).where("index").lte(index).build();
        entityManager.delete(deleteQuery);
        var query = select().from(COLLECTION_NAME).build();
        var entities = entityManager.select(query).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(entities).hasSize(5);
            softly.assertThat(entities).allMatch(e -> e.find("index").orElseThrow().get(Integer.class) > index);
        });
    }

    @Test
    void shouldExecuteCustomQuery() {
        var entity = getEntity();
        entityManager.insert(entity);

        String cypher = "MATCH (e:person) RETURN e";
        var result = entityManager.executeQuery(cypher, new HashMap<>()).toList();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotEmpty();
            softly.assertThat(result.get(0).find("name")).isPresent();
        });
    }

    @Test
    void shouldTraverseGraph() {
        var person1 = getEntity();
        var person2 = getEntity();
        var person3 = getEntity();

        entityManager.insert(person1);
        entityManager.insert(person2);
        entityManager.insert(person3);

        entityManager.edge(person1, "FRIEND", person2);
        entityManager.edge(person2, "FRIEND", person3);

        var startNodeId = person1.find("_id").orElseThrow().get();
        var result = entityManager.traverse(startNodeId.toString(), "FRIEND", 2).toList();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(2);
            softly.assertThat(result).allMatch(e -> e.find("_id").isPresent());
        });
        entityManager.remove(person1, "FRIEND", person2);
        entityManager.remove(person2, "FRIEND", person3);
    }

    @Test
    void shouldCreateEdge() {
        var person1 = entityManager.insert(getEntity());
        var person2 = entityManager.insert(getEntity());

        String person1Id = entityManager.select(select().from(COLLECTION_NAME)
                        .where("_id").eq(person1.find("_id").orElseThrow().get()).build())
                .findFirst().orElseThrow().find("_id").orElseThrow().get(String.class);

        String person2Id = entityManager.select(select().from(COLLECTION_NAME)
                        .where("_id").eq(person2.find("_id").orElseThrow().get()).build())
                .findFirst().orElseThrow().find("_id").orElseThrow().get(String.class);

        person1.add("_id", person1Id);
        person2.add("_id", person2Id);

        entityManager.edge(person1, "FRIEND", person2);

        String cypher = "MATCH (p1:person) WHERE elementId(p1) = $id1 " +
                "MATCH (p2:person) WHERE elementId(p2) = $id2 " +
                "MATCH (p1)-[r:FRIEND]-(p2) RETURN r";

        Map<String, Object> parameters = Map.of(
                "id1", person1Id,
                "id2", person2Id
        );

        var result = entityManager.executeQuery(cypher, parameters).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isNotEmpty();
        });

        entityManager.remove(person1, "FRIEND", person2);
    }

    @Test
    void shouldRemoveEdge() {
        var person1 = getEntity();
        var person2 = getEntity();

        entityManager.insert(person1);
        entityManager.insert(person2);

        entityManager.edge(person1, "FRIEND", person2);

        var startNodeId = person1.find("_id").orElseThrow().get();
        var targetNodeId = person2.find("_id").orElseThrow().get();

        String cypher = "MATCH (p1:person { _id: $_id1 })-[r:FRIEND]-(p2:person { _id: $_id2 }) RETURN r";
        Map<String, Object> parameters = Map.of("_id1", startNodeId, "_id2", targetNodeId);

        var result = entityManager.executeQuery(cypher, parameters).toList();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).isEmpty();
        });
    }


    private CommunicationEntity getEntity() {
        Faker faker = new Faker();

        CommunicationEntity entity = CommunicationEntity.of(COLLECTION_NAME);
        Map<String, Object> map = new HashMap<>();
        map.put("name", faker.name().fullName());
        map.put("city", faker.address().city());
        map.put("age", faker.number().randomNumber());
        List<Element> documents = Elements.of(map);
        documents.forEach(entity::add);
        return entity;
    }

    private void removeAllEdges() {
        String cypher = "MATCH ()-[r]-() DELETE r";

        try {
            entityManager.executeQuery(cypher, new HashMap<>()).toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove edges before node deletion", e);
        }
    }
}
