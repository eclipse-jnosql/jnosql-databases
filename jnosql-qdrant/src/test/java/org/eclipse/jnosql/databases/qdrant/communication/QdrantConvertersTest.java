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

import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QdrantConvertersTest {

    @Nested
    @DisplayName("When converting identifiers to point ids")
    class WhenConvertingIds {

        @Test
        @DisplayName("Should keep non-negative integers and UUIDs")
        void shouldKeepNativeIds() {
            UUID uuid = UUID.randomUUID();
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(QdrantPointIds.of(10L).getNum()).isEqualTo(10L);
                soft.assertThat(QdrantPointIds.of(uuid).getUuid()).isEqualTo(uuid.toString());
                soft.assertThat(QdrantPointIds.of(uuid.toString()).getUuid()).isEqualTo(uuid.toString());
            });
        }

        @Test
        @DisplayName("Should map other identifiers to a deterministic UUID")
        void shouldMapToDeterministicUuid() {
            assertThat(QdrantPointIds.of("book-1")).isEqualTo(QdrantPointIds.of("book-1"))
                    .isNotEqualTo(QdrantPointIds.of("book-2"));
            assertThat(QdrantPointIds.of(-1L).hasUuid()).isTrue();
        }
    }

    @Nested
    @DisplayName("When converting payloads")
    class WhenConvertingPayloads {

        @Test
        @DisplayName("Should round trip scalars, lists and nested maps")
        void shouldRoundTrip() {
            var payload = QdrantPayloadConverter.toPayload(List.of(
                    Element.of("name", "Dune"),
                    Element.of("year", 1965),
                    Element.of("rating", 4.5d),
                    Element.of("available", true),
                    Element.of("tags", List.of("sf", "classic")),
                    Element.of("meta", Map.of("pages", 600))));

            var elements = QdrantPayloadConverter.toElements(payload);

            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(find(elements, "name")).isEqualTo("Dune");
                soft.assertThat(find(elements, "year")).isEqualTo(1965L);
                soft.assertThat(find(elements, "rating")).isEqualTo(4.5d);
                soft.assertThat(find(elements, "available")).isEqualTo(true);
                soft.assertThat(find(elements, "tags")).isEqualTo(List.of("sf", "classic"));
            });
        }

        @Test
        @DisplayName("Should reject a vector inside the payload")
        void shouldRejectVector() {
            assertThatThrownBy(() -> QdrantPayloadConverter.toValue(DenseVector.of(1f)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        private Object find(List<Element> elements, String name) {
            return elements.stream().filter(e -> e.name().equals(name)).findFirst().orElseThrow().get();
        }
    }
}
