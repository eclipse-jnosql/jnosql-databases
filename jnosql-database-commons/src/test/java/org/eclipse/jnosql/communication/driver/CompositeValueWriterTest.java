/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.communication.driver;


import org.eclipse.jnosql.communication.ValueWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Month;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompositeValueWriterTest {

    // Stubs to simulate custom writers
    private static class StringToIntegerWriter implements ValueWriter<String, Integer> {
        @Override
        public boolean test(Class<?> type) {
            return String.class.equals(type);
        }

        @Override
        public Integer write(String value) {
            return value == null ? null : Integer.parseInt(value);
        }
    }

    private static class DummyUUIDWriter implements ValueWriter<UUID, String> {
        @Override
        public boolean test(Class<?> type) {
            return UUID.class.equals(type);
        }

        @Override
        public String write(UUID value) {
            return "CUSTOM-" + value.toString();
        }
    }

    @Nested
    @DisplayName("When validating supported value types")
    class WhenTheValidation {

        @Test
        @DisplayName("Should return true when at least one registered custom writer supports the type")
        void shouldReturnTrueWhenCustomWriterSupportsTheType() {
            // Given
            CompositeValueWriter<Object, Object> composite = new CompositeValueWriter<>(
                    new StringToIntegerWriter(),
                    new DummyUUIDWriter()
            );

            // When
            boolean supportsString = composite.test(String.class);
            boolean supportsUUID = composite.test(UUID.class);

            // Then
            assertThat(supportsString).isTrue();
            assertThat(supportsUUID).isTrue();
        }

        @Test
        @DisplayName("Should fall back to the system default configuration when no custom writer matches")
        void shouldFallbackToDefaultWhenNoCustomWriterMatches() {
            // Given
            CompositeValueWriter<Object, Object> composite = new CompositeValueWriter<>(
                    new StringToIntegerWriter()
            );

            // When
            boolean result = composite.test(Void.class);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("When converting values")
    class WhenTheConversion {

        @Test
        @DisplayName("Should delegate to the appropriate custom writer when a matching type is provided")
        void shouldDelegateToMatchingCustomWriter() {
            // Given
            CompositeValueWriter<Object, Object> composite = new CompositeValueWriter<>(
                    new StringToIntegerWriter(),
                    new DummyUUIDWriter()
            );
            UUID targetUuid = UUID.randomUUID();

            // When
            Object result = composite.write(targetUuid);

            // Then
            assertThat(result)
                    .isInstanceOf(String.class)
                    .isEqualTo("CUSTOM-" + targetUuid);
        }

        @Test
        @DisplayName("Should respect registration order and use the first matching writer if multiple support the type")
        void shouldRespectOrderWhenMultipleWritersMatch() {
            // Given
            ValueWriter<String, String> firstMatchingWriter = new ValueWriter<>() {
                @Override public boolean test(Class<?> type) { return String.class.equals(type); }
                @Override public String write(String type) { return "FIRST"; }
            };

            ValueWriter<String, String> secondMatchingWriter = new ValueWriter<>() {
                @Override public boolean test(Class<?> type) { return String.class.equals(type); }
                @Override public String write(String type) { return "SECOND"; }
            };

            CompositeValueWriter<String, String> composite = new CompositeValueWriter<>(
                    firstMatchingWriter,
                    secondMatchingWriter
            );

            // When
            String result = composite.write("input");

            // Then
            assertThat(result).isEqualTo("FIRST");
        }

        @Test
        @DisplayName("Should fall back to default writer behavior when payload type has no custom match")
        void shouldFallbackToDefaultWriterWhenNoCustomMatchFound() {
            // Given
            CompositeValueWriter<Object, Object> composite = new CompositeValueWriter<>(new DummyUUIDWriter());

            // When
            Object result = composite.write(Month.APRIL);

            // Then
            assertThat(result).isEqualTo("APRIL");
        }
    }
}