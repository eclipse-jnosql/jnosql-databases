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
package org.eclipse.jnosql.databases.weaviate.communication;

import jakarta.data.Sort;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.eclipse.jnosql.communication.semistructured.DeleteQuery.delete;
import static org.eclipse.jnosql.communication.semistructured.SelectQuery.select;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class WeaviateVectorManagerOperationsTest {

    private static final VectorManager manager = VectorDatabase.INSTANCE.get("library");

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

    private List<String> titles(SelectQuery query) {
        return manager.select(query).map(e -> e.find("title").orElseThrow().get(String.class)).toList();
    }

    @Nested
    @DisplayName("When inserting")
    class WhenTheInsertion {

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
        @DisplayName("Should reject a property that is not in the fixed collection schema")
        void shouldRejectUnknownProperty() {
            newCollection();
            seed();
            CommunicationEntity entity = book("d", "Hyperion", 1989, 0f, 0f, 1f);
            entity.add("note", "read");

            assertThatThrownBy(() -> manager.insert(entity)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("note");
        }

        @Test
        @DisplayName("Should insert several entities in one batch")
        void shouldInsertBatch() {
            newCollection();
            seed();

            assertThat(manager.count(collection)).isEqualTo(3);
        }

        @Test
        @DisplayName("Should replace the entity that has the same id")
        void shouldUpsert() {
            newCollection();
            seed();
            manager.insert(book("a", "Dune Messiah", 1969, 1f, 0f, 0f));

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(manager.count(collection)).isEqualTo(3);
                soft.assertThat(titles(select().from(collection).where("_id").eq("a").build()))
                        .containsExactly("Dune Messiah");
            });
        }

        @Test
        @DisplayName("Should store numeric, boolean, null and collection attributes")
        void shouldStoreScalarTypes() {
            newCollection();
            CommunicationEntity entity = CommunicationEntity.of(collection);
            entity.add("_id", 7L);
            entity.add("rating", 4.5d);
            entity.add("count", 3);
            entity.add("available", true);
            entity.add("note", (String) null);
            entity.add("tags", List.of("sf", "classic"));
            entity.add("embedding", DenseVector.of(1f, 0f, 0f));
            manager.insert(entity);

            var found = manager.select(select().from(collection).where("_id").eq(7L).build()).findFirst().orElseThrow();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(found.find("rating").orElseThrow().get()).isEqualTo(4.5d);
                soft.assertThat(found.find("count").orElseThrow().get()).isEqualTo(3L);
                soft.assertThat(found.find("available").orElseThrow().get()).isEqualTo(true);
                soft.assertThat(found.find("note")).isEmpty();
                soft.assertThat(found.find("tags").orElseThrow().get(new org.eclipse.jnosql.communication.TypeReference<List<String>>() {
                }))
                        .containsExactly("sf", "classic");
                soft.assertThat(found.find("_id").orElseThrow().get()).isEqualTo(7L);
            });
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
        @DisplayName("Should reject batch vectors with different dimensions")
        void shouldRejectBatchDimensionMismatch() {
            newCollection();

            assertThatThrownBy(() -> manager.insert(List.of(book("a", "X", 1, 1f, 0f), book("b", "Y", 2, 1f))))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject a value whose type differs from the collection schema")
        void shouldRejectSchemaMismatch() {
            newCollection();
            seed();
            CommunicationEntity entity = CommunicationEntity.of(collection);
            entity.add("_id", "z");
            entity.add("title", 10L);
            entity.add("embedding", DenseVector.of(1f, 0f, 0f));

            assertThatThrownBy(() -> manager.insert(entity)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject an id whose type differs from the collection")
        void shouldRejectIdMismatch() {
            newCollection();
            CommunicationEntity first = CommunicationEntity.of(collection);
            first.add("_id", 1L);
            first.add("embedding", DenseVector.of(1f, 0f, 0f));
            manager.insert(first);
            CommunicationEntity entity = CommunicationEntity.of(collection);
            entity.add("_id", "text");
            entity.add("embedding", DenseVector.of(1f, 0f, 0f));

            assertThatThrownBy(() -> manager.insert(entity)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject an entity without a vector")
        void shouldRejectMissingVector() {
            newCollection();
            CommunicationEntity entity = CommunicationEntity.of(collection);
            entity.add("_id", "a");

            assertThatThrownBy(() -> manager.insert(entity)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject a value that Weaviate cannot store")
        void shouldRejectUnsupportedValue() {
            newCollection();
            CommunicationEntity entity = book("a", "X", 1, 1f, 0f);
            entity.add("price", new java.math.BigDecimal("1.5"));

            assertThatThrownBy(() -> manager.insert(entity)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should not support a time to live")
        void shouldRejectTtl() {
            newCollection();

            assertThatThrownBy(() -> manager.insert(book("a", "X", 1, 1f, 0f), Duration.ofSeconds(1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("When selecting")
    class WhenTheSelection {

        @Test
        @DisplayName("Should return the entity with fields and vector")
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
        @DisplayName("Should filter by equality and inequality")
        void shouldFilterByEquality() {
            newCollection();
            seed();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(titles(select().from(collection).where("title").eq("Dune").build()))
                        .containsExactly("Dune");
                soft.assertThat(titles(select().from(collection).where("title").not().eq("Dune").build()))
                        .containsExactlyInAnyOrder("Neuromancer", "Foundation");
            });
        }

        @Test
        @DisplayName("Should filter by GT, GTE, LT and LTE")
        void shouldFilterByComparison() {
            newCollection();
            seed();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(titles(select().from(collection).where("year").gt(1965L).build()))
                        .containsExactly("Neuromancer");
                soft.assertThat(titles(select().from(collection).where("year").gte(1965L).build()))
                        .containsExactlyInAnyOrder("Dune", "Neuromancer");
                soft.assertThat(titles(select().from(collection).where("year").lt(1965L).build()))
                        .containsExactly("Foundation");
                soft.assertThat(titles(select().from(collection).where("year").lte(1965L).build()))
                        .containsExactlyInAnyOrder("Dune", "Foundation");
            });
        }

        @Test
        @DisplayName("Should filter by IN and BETWEEN")
        void shouldFilterByInAndBetween() {
            newCollection();
            seed();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(titles(select().from(collection).where("title").in(List.of("Dune", "Foundation")).build()))
                        .containsExactlyInAnyOrder("Dune", "Foundation");
                soft.assertThat(titles(select().from(collection).where("year").between(1950L, 1970L).build()))
                        .containsExactlyInAnyOrder("Dune", "Foundation");
            });
        }

        @Test
        @DisplayName("Should filter by AND, OR and NOT")
        void shouldFilterByLogicalConditions() {
            newCollection();
            seed();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(titles(select().from(collection).where("title").eq("Dune")
                        .and("year").eq(1965L).build())).containsExactly("Dune");
                soft.assertThat(titles(select().from(collection).where("title").eq("Dune")
                        .or("title").eq("Neuromancer").build())).containsExactlyInAnyOrder("Dune", "Neuromancer");
                soft.assertThat(titles(select().from(collection).where("year").gt(1950L)
                        .and("year").lt(1970L).build())).containsExactlyInAnyOrder("Dune", "Foundation");
            });
        }

        @Test
        @DisplayName("Should filter by a null attribute")
        void shouldFilterByNull() {
            newCollection();
            CommunicationEntity entity = book("d", "Hyperion", 1989, 0f, 0f, 1f);
            entity.add("note", "read");
            manager.insert(entity);
            seed();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(manager.select(new Query(collection, CriteriaCondition.eq(Element.of("note", null)), 0, 0,
                                List.of(), List.of(), DenseVector.of(1f, 0f, 0f), null))
                        .map(e -> e.find("title").orElseThrow().get(String.class)).toList())
                        .containsExactlyInAnyOrder("Dune", "Neuromancer", "Foundation");
                soft.assertThat(titles(select().from(collection).where("note").eq("read").build()))
                        .containsExactly("Hyperion");
                soft.assertThat(manager.select(select().from(collection).where("note").eq("read").build()))
                        .hasSize(1);
            });
        }

        @Test
        @DisplayName("Should skip and limit")
        void shouldSkipAndLimit() {
            newCollection();
            seed();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(manager.select(select().from(collection).limit(2).build())).hasSize(2);
                soft.assertThat(manager.select(select().from(collection).skip(1).build())).hasSize(2);
                soft.assertThat(manager.select(select().from(collection).skip(1).limit(1).build())).hasSize(1);
                soft.assertThat(manager.select(select().from(collection).skip(5).build())).isEmpty();
            });
        }

        @Test
        @DisplayName("Should project the requested columns")
        void shouldProject() {
            newCollection();
            seed();

            var entity = manager.select(select("title").from(collection).where("_id").eq("a").build())
                    .findFirst().orElseThrow();

            assertThat(entity.elements()).extracting(Element::name)
                    .containsExactlyInAnyOrder("title", "_id");
        }

        @Test
        @DisplayName("Should project the vector when it is requested")
        void shouldProjectVector() {
            newCollection();
            seed();

            var entity = manager.select(select("embedding").from(collection).where("_id").eq("a").build())
                    .findFirst().orElseThrow();

            assertThat(entity.elements()).extracting(Element::name)
                    .containsExactlyInAnyOrder("_id", "embedding");
        }

        @Test
        @DisplayName("Should return nothing for a missing collection")
        void shouldReturnEmptyForMissingCollection() {
            assertThat(manager.select(select().from("missing" + UUID.randomUUID().toString().replace("-", "")).build()))
                    .isEmpty();
        }

        @Test
        @DisplayName("Should not support sorting")
        void shouldRejectSort() {
            newCollection();
            seed();

            assertThatThrownBy(() -> manager.select(select().from(collection).orderBy("year").asc().build()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should reject conditions that Weaviate cannot express")
        void shouldRejectUnsupported() {
            newCollection();
            seed();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> manager.select(select().from(collection).where("title").like("Du%").build()))
                        .isInstanceOf(UnsupportedOperationException.class);
            });
        }
    }

    @Nested
    @DisplayName("When counting")
    class WhenTheCount {

        @Test
        @DisplayName("Should count all and by condition")
        void shouldCount() {
            newCollection();
            seed();

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(manager.count(collection)).isEqualTo(3);
                soft.assertThat(manager.count(select().from(collection).where("year").gt(1960L).build()))
                        .isEqualTo(2);
                soft.assertThat(manager.count(select().from(collection).limit(2).build())).isEqualTo(3);
                soft.assertThat(manager.exists(select().from(collection).where("_id").eq("a").build())).isTrue();
                soft.assertThat(manager.exists(select().from(collection).where("_id").eq("zzz").build())).isFalse();
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
    class WhenTheUpdate {

        @Test
        @DisplayName("Should replace the entity")
        void shouldUpdate() {
            newCollection();
            seed();
            manager.update(book("a", "Dune Messiah", 1969, 0f, 0f, 1f));

            var entity = manager.select(select().from(collection).where("_id").eq("a").build()).findFirst().orElseThrow();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(entity.find("title").orElseThrow().get()).isEqualTo("Dune Messiah");
                soft.assertThat(entity.find("year").orElseThrow().get()).isEqualTo(1969L);
                soft.assertThat(((DenseVector) entity.find("embedding").orElseThrow().get()).values())
                        .containsExactly(0f, 0f, 1f);
            });
        }

        @Test
        @DisplayName("Should update several entities")
        void shouldUpdateBatch() {
            newCollection();
            seed();
            manager.update(List.of(book("a", "A", 1, 1f, 0f, 0f), book("b", "B", 2, 0f, 1f, 0f)));

            assertThat(manager.count(select().from(collection).where("year").lt(10L).build())).isEqualTo(2);
        }

        @Test
        @DisplayName("Should not create an entity that does not exist")
        void shouldNotUpsert() {
            newCollection();
            seed();
            manager.update(book("zzz", "Ghost", 1, 1f, 0f, 0f));

            assertThat(manager.count(collection)).isEqualTo(3);
        }

        @Test
        @DisplayName("Should not create a collection that does not exist")
        void shouldIgnoreMissingCollection() {
            newCollection();
            manager.update(book("a", "Ghost", 1, 1f, 0f, 0f));

            assertThat(manager.count(collection)).isZero();
        }

        @Test
        @DisplayName("Should require the _id")
        void shouldRequireId() {
            newCollection();

            assertThatThrownBy(() -> manager.update(book(null, "Ghost", 1, 1f, 0f, 0f)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("When using UUID identifiers")
    class WhenTheIdIsUuid {

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

        @Test
        @DisplayName("Should filter by a UUID attribute")
        void shouldFilterByUuidAttribute() {
            newCollection();
            UUID external = UUID.randomUUID();
            CommunicationEntity entity = CommunicationEntity.of(collection);
            entity.add("_id", id);
            entity.add("externalId", external);
            entity.add("embedding", DenseVector.of(1f, 0f, 0f));
            manager.insert(entity);

            assertThat(manager.count(select().from(collection).where("externalId").eq(external).build()))
                    .isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("When deleting")
    class WhenTheRemoval {

        @Test
        @DisplayName("Should delete by id")
        void shouldDeleteById() {
            newCollection();
            seed();
            manager.delete(delete().from(collection).where("_id").eq("a").build());

            assertThat(manager.count(collection)).isEqualTo(2);
        }

        @Test
        @DisplayName("Should delete by condition")
        void shouldDeleteByCondition() {
            newCollection();
            seed();
            manager.delete(delete().from(collection).where("year").gt(1960L).build());

            assertThat(titles(select().from(collection).build())).containsExactly("Foundation");
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
        @DisplayName("Should delete everything when the id is numeric")
        void shouldDeleteAllWithNumericId() {
            newCollection();
            CommunicationEntity entity = CommunicationEntity.of(collection);
            entity.add("_id", 5L);
            entity.add("embedding", DenseVector.of(1f, 0f, 0f));
            manager.insert(entity);
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
    class WhenTheSearch {

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
        @DisplayName("Should combine filter, threshold and limit")
        void shouldCombineFilterThresholdLimit() {
            newCollection();
            seed();
            var condition = select().from(collection).where("year").lt(1970L).build().condition().orElseThrow();

            var result = manager.search(search(new float[]{1f, 0f, 0f}, condition, 1, 0.5f));

            assertThat(result).extracting(e -> e.find("title").orElseThrow().get()).containsExactly("Dune");
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
        @DisplayName("Should project the requested columns")
        void shouldProject() {
            newCollection();
            seed();
            var query = new Query(collection, null, 1, 0, List.of("title"), List.of(),
                    DenseVector.of(1f, 0f, 0f), null);

            var first = manager.search(query).get(0);

            assertThat(first.elements()).extracting(Element::name)
                    .containsExactlyInAnyOrder("title", "_id");
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
        @DisplayName("Should reject a search without a positive limit")
        void shouldRejectLimit() {
            newCollection();
            seed();

            assertThatThrownBy(() -> manager.search(search(new float[]{1f, 0f, 0f}, null, 0, null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should return nothing for a missing collection")
        void shouldReturnEmptyForMissing() {
            newCollection();

            assertThat(manager.search(search(new float[]{1f, 0f}, null, 1, null))).isEmpty();
        }
    }

    @Nested
    @DisplayName("When searching with the configured distance")
    class WhenTheDistanceIsConfigured {

        private void run(WeaviateDistance distance, float threshold, String... expected) {
            newCollection();
            VectorManager configured = VectorDatabase.INSTANCE.get("library", distance);
            configured.insert(List.of(
                    book("a", "Near", 1, 1f, 0f),
                    book("b", "Far", 2, 4f, 4f)));

            var query = search(new float[]{1f, 0f}, null, 10, threshold);
            var result = configured.search(query);

            assertThat(result).extracting(e -> e.find("title").orElseThrow().get()).containsExactly(expected);
        }

        @Test
        @DisplayName("Should use the maximum distance as threshold for the Euclidean distance")
        void shouldUseEuclidean() {
            run(WeaviateDistance.EUCLIDEAN, 2f, "Near");
        }

        @Test
        @DisplayName("Should compare the Euclidean threshold with the real distance, not the squared one")
        void shouldUseRealEuclideanDistance() {
            newCollection();
            VectorManager configured = VectorDatabase.INSTANCE.get("library", WeaviateDistance.EUCLIDEAN);
            configured.insert(List.of(
                    book("a", "Mid", 1, 2.5f, 0f),
                    book("b", "Far", 2, 4f, 4f)));

            var inside = configured.search(search(new float[]{1f, 0f}, null, 10, 2f));
            var outside = configured.search(search(new float[]{1f, 0f}, null, 10, 1f));

            assertThat(inside).extracting(e -> e.find("title").orElseThrow().get()).containsExactly("Mid");
            assertThat(outside).isEmpty();
        }

        @Test
        @DisplayName("Should use the minimum score as threshold for the dot product distance")
        void shouldUseInnerProduct() {
            run(WeaviateDistance.DOT, 2f, "Far");
        }

        @Test
        @DisplayName("Should use the maximum Manhattan distance as threshold")
        void shouldUseManhattan() {
            run(WeaviateDistance.MANHATTAN, 3f, "Near");
        }

        @Test
        @DisplayName("Should use the minimum similarity as threshold for the cosine distance")
        void shouldUseCosine() {
            run(WeaviateDistance.COSINE, 0.9f, "Near");
        }
    }

    @Nested
    @DisplayName("When the vector is invalid")
    class WhenTheVectorIsInvalid {

        @Test
        @DisplayName("Should reject more than one vector in an entity")
        void shouldRejectSeveralVectors() {
            newCollection();
            CommunicationEntity entity = book("a", "X", 1, 1f, 0f);
            entity.add("other", DenseVector.of(1f, 0f));

            assertThatThrownBy(() -> manager.insert(entity)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should expose the Weaviate database as the manager name")
        void shouldExposeName() {
            assertThat(manager.name()).isEqualTo("library");
            assertThat(Map.of("a", 1)).isNotEmpty();
        }
    }
}
