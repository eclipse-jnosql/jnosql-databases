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
package org.eclipse.jnosql.databases.milvus.communication;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MilvusRowConverterTest {

    private static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Nested
    @DisplayName("When writing a row")
    class WhenTheRowIsWritten {

        @Test
        @DisplayName("Should write scalars, UUID, vector and skip nulls")
        void shouldWrite() {
            var entity = CommunicationEntity.of("c");
            entity.add("_id", ID);
            entity.add("title", "Dune");
            entity.add("year", 1965);
            entity.add("rating", 4.5d);
            entity.add("available", true);
            entity.add("note", (String) null);
            entity.add("tags", List.of("sf", "classic"));
            entity.add("embedding", DenseVector.of(1f, 0f));

            JsonObject row = MilvusRowConverter.toRow(entity, MilvusRowConverter.vectorElement(entity));

            assertSoftly(soft -> {
                soft.assertThat(row.get("_id").getAsString()).isEqualTo(ID.toString());
                soft.assertThat(row.get("title").getAsString()).isEqualTo("Dune");
                soft.assertThat(row.get("year").getAsLong()).isEqualTo(1965L);
                soft.assertThat(row.get("rating").getAsDouble()).isEqualTo(4.5d);
                soft.assertThat(row.get("available").getAsBoolean()).isTrue();
                soft.assertThat(row.has("note")).isFalse();
                soft.assertThat(row.getAsJsonArray("tags")).hasSize(2);
                soft.assertThat(row.getAsJsonArray("embedding")).hasSize(2);
            });
        }

        @Test
        @DisplayName("Should write sub-documents as JSON objects")
        void shouldWriteSubDocument() {
            var entity = CommunicationEntity.of("c");
            entity.add("embedding", DenseVector.of(1f));
            entity.add("address", List.of(Element.of("city", "Lisbon"), Element.of("zip", 1000)));

            JsonObject row = MilvusRowConverter.toRow(entity, MilvusRowConverter.vectorElement(entity));

            assertThat(row.getAsJsonObject("address").get("city").getAsString()).isEqualTo("Lisbon");
        }

        @Test
        @DisplayName("Should not stringify an unsupported value")
        void shouldRejectUnsupported() {
            assertThatThrownBy(() -> MilvusRowConverter.toJson(new Object()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("Should generate an id when the entity has none")
        void shouldGenerateId() {
            var entity = CommunicationEntity.of("c");

            Object id = MilvusRowConverter.ensureId(entity);

            assertThat(entity.find("_id").orElseThrow().get()).isEqualTo(id);
        }

        @Test
        @DisplayName("Should require exactly one vector")
        void shouldRequireOneVector() {
            var entity = CommunicationEntity.of("c");
            entity.add("title", "x");

            assertThatThrownBy(() -> MilvusRowConverter.vectorElement(entity))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("When reading a row")
    class WhenTheRowIsRead {

        @Test
        @DisplayName("Should read scalars, vector and JSON values, skipping nulls and dynamic metadata")
        void shouldRead() {
            JsonObject address = new JsonObject();
            address.addProperty("city", "Lisbon");
            address.addProperty("zip", 1000);
            JsonArray tags = new JsonArray();
            tags.add("sf");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("_id", "a");
            row.put("year", 1965L);
            row.put("embedding", List.of(1f, 0.5f));
            row.put("address", address);
            row.put("tags", tags);
            row.put("note", null);
            row.put("$meta", new JsonObject());

            var entity = MilvusRowConverter.toEntity("c", row, "embedding");

            assertSoftly(soft -> {
                soft.assertThat(entity.name()).isEqualTo("c");
                soft.assertThat(entity.elements()).extracting(Element::name)
                        .containsExactlyInAnyOrder("_id", "year", "embedding", "address", "tags");
                soft.assertThat(((DenseVector) entity.find("embedding").orElseThrow().get()).values())
                        .containsExactly(1f, 0.5f);
                soft.assertThat(entity.find("year").orElseThrow().get()).isEqualTo(1965L);
                soft.assertThat((Object) entity.find("tags").orElseThrow().value().get()).isEqualTo(List.of("sf"));
            });
        }

        @Test
        @DisplayName("Should keep a user property named like the sparse metadata in dense and multi collections")
        void shouldKeepSparseDimensionsPropertyWhenNotSparse() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("_id", "a");
            row.put("_sparse_dimensions", 7L);
            row.put("embedding", List.of(1f, 0.5f));

            var dense = MilvusRowConverter.toEntity("c", row, "embedding", MilvusVectors.Kind.DENSE);

            assertThat(dense.find("_sparse_dimensions").orElseThrow().get()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Should consume the sparse metadata only in sparse collections")
        void shouldConsumeSparseDimensions() {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("_id", "a");
            row.put("_sparse_dimensions", 10L);
            row.put("embedding", new java.util.TreeMap<>(Map.of(2L, 7.0f, 8L, 3.0f)));

            var sparse = MilvusRowConverter.toEntity("c", row, "embedding", MilvusVectors.Kind.SPARSE);

            assertThat(sparse.find("_sparse_dimensions")).isEmpty();
        }
    }
}
