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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeaviateValueConverterTest {

    @Test
    @DisplayName("Should convert scalars to storable values")
    void shouldConvertScalars() {
        UUID uuid = UUID.randomUUID();

        assertThat(WeaviateValueConverter.toStorable(uuid)).isEqualTo(uuid.toString());
        assertThat(WeaviateValueConverter.toStorable(3)).isEqualTo(3L);
        assertThat(WeaviateValueConverter.toStorable(1.5f)).isEqualTo(1.5d);
        assertThat(WeaviateValueConverter.toStorable(true)).isEqualTo(true);
        assertThat(WeaviateValueConverter.toStorable("x")).isEqualTo("x");
    }

    @Test
    @DisplayName("Should convert collections to arrays")
    void shouldConvertCollections() {
        assertThat(WeaviateValueConverter.toStorable(List.of(1, 2))).isEqualTo(List.of(1L, 2L));
    }

    @Test
    @DisplayName("Should reject unsupported objects")
    void shouldRejectUnsupported() {
        assertThatThrownBy(() -> WeaviateValueConverter.toStorable(new Object()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should infer data types and compatibility")
    void shouldInferTypes() {
        assertThat(WeaviateValueConverter.dataType("x")).isEqualTo("text");
        assertThat(WeaviateValueConverter.dataType(1L)).isEqualTo("int");
        assertThat(WeaviateValueConverter.dataType(1d)).isEqualTo("number");
        assertThat(WeaviateValueConverter.dataType(true)).isEqualTo("boolean");
        assertThat(WeaviateValueConverter.dataType(List.of("a"))).isEqualTo("text[]");
        assertThat(WeaviateValueConverter.dataType(List.of())).isNull();
        assertThat(WeaviateValueConverter.isCompatible("number", 1L)).isTrue();
        assertThat(WeaviateValueConverter.isCompatible("text", 1L)).isFalse();
        assertThat(WeaviateValueConverter.isCompatible("int[]", List.of())).isTrue();
    }
}
