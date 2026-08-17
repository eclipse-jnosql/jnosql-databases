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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UUIDValueWriterTest {
    private final UUIDValueWriter writer = new UUIDValueWriter();

    @Nested
    @DisplayName("When validating supported UUID types")
    class WhenTheValidation {

        @Test
        @DisplayName("Should return true when the class type is exactly UUID")
        void shouldReturnTrueWhenClassIsUUID() {
            // When
            boolean result = writer.test(UUID.class);

            // Then
            assertThat(result)
                    .isTrue();
        }

        @Test
        @DisplayName("Should return false when the class type is not UUID")
        void shouldReturnFalseWhenClassIsNotUUID() {
            // When
            boolean resultWithString = writer.test(String.class);
            boolean resultWithInteger = writer.test(Integer.class);

            // Then
            assertThat(resultWithString).isFalse();

            assertThat(resultWithInteger).isFalse();
        }

        @Test
        @DisplayName("Should return false when the class type is null")
        void shouldReturnFalseWhenClassIsNull() {
            // When
            boolean result = writer.test(null);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("When converting UUID values")
    class WhenTheConversion {

        @Test
        @DisplayName("Should return a valid canonical String representation when UUID is provided")
        void shouldReturnStringRepresentationWhenUUIDIsNotNull() {
            // Given
            UUID sampleUuid = UUID.randomUUID();
            String expectedString = sampleUuid.toString();

            // When
            String actualString = writer.write(sampleUuid);

            // Then
            assertThat(actualString).isNotNull().isEqualTo(expectedString);
        }

        @Test
        @DisplayName("Should return null when the provided UUID parameter is null")
        void shouldReturnNullWhenUUIDIsNull() {
            // When
            String actualString = writer.write(null);

            // Then
            assertThat(actualString).isNull();
        }
    }
}