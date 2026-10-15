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

import org.eclipse.jnosql.communication.Value;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Qdrant value write decorator")
class QdrantValueWriteDecoratorTest {

    private static final UUID ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @Nested
    @DisplayName("When converting a single value")
    class WhenConvertingValue {

        @Test
        @DisplayName("Should convert a UUID to its String representation")
        void shouldConvertUuid() {
            assertThat(QdrantValueWriteDecorator.convert(Value.of(ID)))
                    .isEqualTo("550e8400-e29b-41d4-a716-446655440000");
        }

        @Test
        @DisplayName("Should keep non-UUID values untouched")
        void shouldKeepOtherValues() {
            assertThat(QdrantValueWriteDecorator.convert(Value.of("text"))).isEqualTo("text");
            assertThat(QdrantValueWriteDecorator.convert(Value.of(10L))).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should keep null as null")
        void shouldKeepNull() {
            assertThat(QdrantValueWriteDecorator.convert(Value.ofNull())).isNull();
        }
    }

    @Nested
    @DisplayName("When converting a list of values")
    class WhenConvertingList {

        @Test
        @DisplayName("Should convert every UUID of the list")
        void shouldConvertUuids() {
            assertThat(QdrantValueWriteDecorator.convertToList(Value.of(List.of(ID, "other"))))
                    .containsExactly("550e8400-e29b-41d4-a716-446655440000", "other");
        }
    }

    @Nested
    @DisplayName("When testing supported types")
    class WhenTestingTypes {

        @Test
        @DisplayName("Should support only UUID")
        void shouldSupportOnlyUuid() {
            var decorator = new QdrantValueWriteDecorator<UUID, String>();

            assertThat(decorator.test(UUID.class)).isTrue();
            assertThat(decorator.test(String.class)).isFalse();
        }

        @Test
        @DisplayName("Should write a UUID as String")
        void shouldWrite() {
            assertThat(new QdrantValueWriteDecorator<UUID, String>().write(ID)).isEqualTo(ID.toString());
        }
    }
}
