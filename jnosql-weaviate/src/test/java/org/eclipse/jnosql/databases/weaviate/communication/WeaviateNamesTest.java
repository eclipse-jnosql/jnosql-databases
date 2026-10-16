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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WeaviateNamesTest {

    @Test
    @DisplayName("Should capitalize the collection name")
    void shouldCapitalizeCollection() {
        assertThat(WeaviateNames.collection("book")).isEqualTo("Book");
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateNames.collection("my-book"));
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateNames.collection(""));
    }

    @Test
    @DisplayName("Should map the id field and reject reserved properties")
    void shouldMapProperties() {
        assertThat(WeaviateNames.property("_id")).isEqualTo("_jnosql_id");
        assertThat(WeaviateNames.field("_jnosql_id")).isEqualTo("_id");
        assertThat(WeaviateNames.property("title")).isEqualTo("title");
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateNames.property("id"));
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateNames.property("_additional"));
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateNames.property("a-b"));
    }

    @Test
    @DisplayName("Should keep UUID ids and derive a stable UUID for other ids")
    void shouldResolveObjectId() {
        UUID uuid = UUID.randomUUID();

        assertThat(WeaviateNames.objectId(uuid.toString().toUpperCase())).isEqualTo(uuid.toString());
        assertThat(WeaviateNames.objectId("book-1")).isEqualTo(WeaviateNames.objectId("book-1"))
                .isNotEqualTo(WeaviateNames.objectId("book-2"));
    }
}
