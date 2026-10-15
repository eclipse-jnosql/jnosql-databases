/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.QdrantClient;
import jakarta.data.Sort;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class QdrantVectorManagerOperationsTest {

    private static final QdrantClient client = VectorDatabase.INSTANCE.client();

    private static final QdrantVectorManager manager = new QdrantVectorManager(client, "library",
            new QdrantCollectionSettings(QdrantDistance.COSINE, null));

    private String collection;

    private String newCollection() {
        collection = "c" + UUID.randomUUID().toString().replace("-", "");
        return collection;
    }

    private CommunicationEntity book(String id, String title, long year, float... vector) {
        CommunicationEntity entity = CommunicationEntity.of(collection);
        if (id != null) {
            entity.add("_id", id);
        }
        entity.add("title", title);
        entity.add("year", year);
        entity.add("embedding", DenseVector.of(vector));
        return entity;
    }

    private record Query(String name, CriteriaCondition conditionValue, long limit, long skip,
                         List<String> columns, List<Sort<?>> sorts,
                         org.eclipse.jnosql.mapping.vector.Vector vector, Float thresholdValue)
            implements VectorSelectQuery {

        @Override
        public Optional<CriteriaCondition> condition() {
            return Optional.ofNullable(conditionValue);
        }

        @Override
        public Optional<Float> threshold() {
            return Optional.ofNullable(thresholdValue);
        }
    }

    private Query search(float[] vector, CriteriaCondition condition, long limit, Float threshold) {
        return new Query(collection, condition, limit, 0, List.of(), List.of(), DenseVector.of(vector), threshold);
    }

    private void seed() {
        manager.insert(List.of(
                book("a", "Dune", 1965, 1f, 0f, 0f),
                book("b", "Neuromancer", 1984, 0f, 1f, 0f),
                book("c", "Foundation", 1951, 0.9f, 0.1f, 0f)));
    }

    @Nested
    @DisplayName("When inserting")
    class WhenInserting {

        @Test
        @DisplayName("Should insert an entity, create the collection and generate the _id")
        void shouldInsert() {
            newCollection();
            var inserted = manager.insert(book(null, "Dune", 1965, 1f, 0f, 0f));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(inserted.find("_id")).isPresent();
                soft.assertThat(manager.count(collection)).isEqualTo(1);
            });
        }

        @Test
        @DisplayName("Should insert several entities in one batch")
        void shouldInsertBatch() {
            newCollection();
            seed();

            assertThat(manager.count(collection)).isEqualTo(3);
        }

        @Test
        @DisplayName("Should reject a vector whose dimension differs from the collection")
        void shouldRejectDimensionMismatch() {
            newCollection();
            seed();

            assertThatThrownBy(() -> manager.insert(book("d", "X", 2000, 1f, 0f)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should not create the collection when the distance is not configured")
        void shouldRequireDistance() {
            newCollection();
            var noDistance = new QdrantVectorManager(client, "library", QdrantCollectionSettings.EMPTY);

            assertThatThrownBy(() -> noDistance.insert(book("a", "Dune", 1965, 1f, 0f)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("When selecting")
    class WhenSelecting {

        @Test
        @DisplayName("Should return the entity with payload and vector")
        void shouldRoundTrip() {
            newCollection();
            seed();

            var result = manager.select(select().from(collection).where("_id").eq("a").build()).toList();

            assertThat(result).hasSize(1);
            var entity = result.get(0);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(entity.find("title").orElseThrow().get()).isEqualTo("Dune");
                soft.assertThat(entity.find("year").orElseThrow().get()).isEqualTo(1965L);
                soft.assertThat(entity.find("_id").orElseThrow().get()).isEqualTo("a");
                soft.assertThat(((DenseVector) entity.find("embedding").orElseThrow().get()).values())
                        .containsExactly(1f, 0f, 0f);
            });
        }

        @Test
        @DisplayName("Should select all when there is no condition")
        void shouldSelectAll() {
            newCollection();
            seed();

            assertThat(manager.select(select().from(collection).build())).hasSize(3);
        }

        @Test
        @DisplayName("Should filter with comparison, AND, OR, IN and BETWEEN conditions")
        void shouldFilter() {
            newCollection();
            seed();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(titles(select().from(collection).where("year").gt(1960L).build()))
                        .containsExactlyInAnyOrder("Dune", "Neuromancer");
                soft.assertThat(titles(select().from(collection).where("year").lte(1951L).build()))
                        .containsExactly("Foundation");
                soft.assertThat(titles(select().from(collection).where("year").between(1950L, 1970L).build()))
                        .containsExactlyInAnyOrder("Dune", "Foundation");
                soft.assertThat(titles(select().from(collection).where("title").in(List.of("Dune", "Foundation")).build()))
                        .containsExactlyInAnyOrder("Dune", "Foundation");
                soft.assertThat(titles(select().from(collection).where("title").eq("Dune")
                        .and("year").eq(1965L).build())).containsExactly("Dune");
                soft.assertThat(titles(select().from(collection).where("title").eq("Dune")
                        .or("title").eq("Neuromancer").build())).containsExactlyInAnyOrder("Dune", "Neuromancer");
                soft.assertThat(titles(select().from(collection).where("title").not().eq("Dune").build()))
                        .containsExactlyInAnyOrder("Neuromancer", "Foundation");
            });
        }

        @Test
        @DisplayName("Should sort, skip and limit")
        void shouldSortSkipLimit() {
            newCollection();
            seed();

            var query = select().from(collection).orderBy("year").asc().skip(1).limit(1).build();

            assertThat(titles(query)).containsExactly("Dune");
        }

        @Test
        @DisplayName("Should sort descending")
        void shouldSortDescending() {
            newCollection();
            seed();

            assertThat(titles(select().from(collection).orderBy("year").desc().build()))
                    .containsExactly("Neuromancer", "Dune", "Foundation");
        }

        @Test
        @DisplayName("Should project the requested columns")
        void shouldProject() {
            newCollection();
            seed();

            var entity = manager.select(select("title").from(collection).where("_id").eq("a").build())
                    .findFirst().orElseThrow();

            assertThat(entity.elements()).extracting(Element::name).containsExactlyInAnyOrder("title", "_id");
        }

        @Test
        @DisplayName("Should return nothing for a missing collection")
        void shouldReturnEmptyForMissingCollection() {
            assertThat(manager.select(select().from("missing" + UUID.randomUUID().toString().replace("-", "")).build()))
                    .isEmpty();
        }

        @Test
        @DisplayName("Should reject unsupported conditions")
        void shouldRejectUnsupported() {
            newCollection();
            seed();

            assertThatThrownBy(() -> manager.select(select().from(collection).where("title").like("Du%").build()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        private List<String> titles(SelectQuery query) {
            return manager.select(query).map(e -> e.find("title").orElseThrow().get(String.class)).toList();
        }
    }

    @Nested
    @DisplayName("When counting")
    class WhenCounting {

        @Test
        @DisplayName("Should count all and by condition")
        void shouldCount() {
            newCollection();
            seed();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(manager.count(collection)).isEqualTo(3);
                soft.assertThat(manager.count(select().from(collection).where("year").gt(1960L).build()))
                        .isEqualTo(2);
                soft.assertThat(manager.exists(select().from(collection).where("_id").eq("a").build())).isTrue();
            });
        }

        @Test
        @DisplayName("Should count zero for a missing collection")
        void shouldCountZeroForMissing() {
            assertThat(manager.count("missing" + UUID.randomUUID().toString().replace("-", ""))).isZero();
        }
    }

    @Nested
    @DisplayName("When updating")
    class WhenUpdating {

        @Test
        @DisplayName("Should merge the payload and replace the vector")
        void shouldUpdate() {
            newCollection();
            seed();
            var update = CommunicationEntity.of(collection);
            update.add("_id", "a");
            update.add("title", "Dune Messiah");
            update.add("embedding", DenseVector.of(0f, 0f, 1f));
            manager.update(update);

            var entity = manager.select(select().from(collection).where("_id").eq("a").build()).findFirst().orElseThrow();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(entity.find("title").orElseThrow().get()).isEqualTo("Dune Messiah");
                soft.assertThat(entity.find("year").orElseThrow().get()).isEqualTo(1965L);
                soft.assertThat(((DenseVector) entity.find("embedding").orElseThrow().get()).values())
                        .containsExactly(0f, 0f, 1f);
            });
        }

        @Test
        @DisplayName("Should update several entities")
        void shouldUpdateBatch() {
            newCollection();
            seed();
            var a = CommunicationEntity.of(collection);
            a.add("_id", "a");
            a.add("year", 1L);
            var b = CommunicationEntity.of(collection);
            b.add("_id", "b");
            b.add("year", 2L);
            manager.update(List.of(a, b));

            assertThat(manager.count(select().from(collection).where("year").lt(10L).build())).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not create a point that does not exist")
        void shouldNotUpsert() {
            newCollection();
            seed();
            var missing = CommunicationEntity.of(collection);
            missing.add("_id", "zzz");
            missing.add("title", "Ghost");
            manager.update(missing);

            assertThat(manager.count(collection)).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("When using UUID identifiers")
    class WhenUsingUuid {

        private final UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        @Test
        @DisplayName("Should store, select and delete an entity by UUID id")
        void shouldRoundTrip() {
            newCollection();
            CommunicationEntity entity = CommunicationEntity.of(collection);
            entity.add("_id", id);
            entity.add("externalId", UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
            entity.add("embedding", DenseVector.of(1f, 0f, 0f));
            manager.insert(entity);

            var found = manager.select(select().from(collection).where("_id").eq(id).build()).toList();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(found).hasSize(1);
                soft.assertThat(found.get(0).find("_id").orElseThrow().get())
                        .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
                soft.assertThat(found.get(0).find("externalId").orElseThrow().get())
                        .isEqualTo("123e4567-e89b-12d3-a456-426614174000");
            });

            manager.delete(delete().from(collection).where("_id").eq(id).build());
            assertThat(manager.count(collection)).isZero();
        }
    }

    @Nested
    @DisplayName("When deleting")
    class WhenDeleting {

        @Test
        @DisplayName("Should delete by condition")
        void shouldDeleteByCondition() {
            newCollection();
            seed();
            manager.delete(delete().from(collection).where("_id").eq("a").build());

            assertThat(manager.count(collection)).isEqualTo(2);
        }

        @Test
        @DisplayName("Should delete everything when there is no condition")
        void shouldDeleteAll() {
            newCollection();
            seed();
            manager.delete(DeleteQuery.delete().from(collection).build());

            assertThat(manager.count(collection)).isZero();
        }

        @Test
        @DisplayName("Should ignore a missing collection")
        void shouldIgnoreMissing() {
            manager.delete(delete().from("missing" + UUID.randomUUID().toString().replace("-", "")).build());
        }
    }

    @Nested
    @DisplayName("When searching by vector")
    class WhenSearching {

        @Test
        @DisplayName("Should return the nearest entities first")
        void shouldSearch() {
            newCollection();
            seed();

            var result = manager.search(search(new float[]{1f, 0f, 0f}, null, 2, null));

            assertThat(result).extracting(e -> e.find("title").orElseThrow().get())
                    .containsExactly("Dune", "Foundation");
        }

        @Test
        @DisplayName("Should apply the threshold")
        void shouldApplyThreshold() {
            newCollection();
            seed();

            var result = manager.search(search(new float[]{1f, 0f, 0f}, null, 10, 0.95f));

            assertThat(result).extracting(e -> e.find("title").orElseThrow().get())
                    .containsExactly("Dune", "Foundation");
        }

        @Test
        @DisplayName("Should apply the filter")
        void shouldApplyFilter() {
            newCollection();
            seed();
            var condition = select().from(collection).where("year").gt(1960L).build().condition().orElseThrow();

            var result = manager.search(search(new float[]{1f, 0f, 0f}, condition, 10, null));

            assertThat(result).extracting(e -> e.find("title").orElseThrow().get())
                    .containsExactly("Dune", "Neuromancer");
        }

        @Test
        @DisplayName("Should skip results")
        void shouldSkip() {
            newCollection();
            seed();
            var query = new Query(collection, null, 10, 1, List.of(), List.of(),
                    DenseVector.of(1f, 0f, 0f), null);

            assertThat(manager.search(query)).hasSize(2);
        }

        @Test
        @DisplayName("Should return the vector in the result")
        void shouldReturnVector() {
            newCollection();
            seed();

            var first = manager.search(search(new float[]{1f, 0f, 0f}, null, 1, null)).get(0);

            assertThat(((DenseVector) first.find("embedding").orElseThrow().get()).values())
                    .containsExactly(1f, 0f, 0f);
        }

        @Test
        @DisplayName("Should reject a query vector with a different dimension")
        void shouldRejectDimension() {
            newCollection();
            seed();

            assertThatThrownBy(() -> manager.search(search(new float[]{1f, 0f}, null, 1, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should return nothing for a missing collection")
        void shouldReturnEmptyForMissing() {
            newCollection();

            assertThat(manager.search(search(new float[]{1f, 0f}, null, 1, null))).isEmpty();
        }
    }
}
