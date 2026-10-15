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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class QdrantConfigurationsTest {

    @Nested
    @DisplayName("When reading the configuration keys")
    class WhenTheKeysAreRead {

        @Test
        @DisplayName("Should expose the documented keys")
        void shouldExposeKeys() {
            assertThat(QdrantConfigurations.values()).extracting(QdrantConfigurations::get)
                    .containsExactly(
                            "jnosql.qdrant.host",
                            "jnosql.qdrant.port",
                            "jnosql.qdrant.tls",
                            "jnosql.qdrant.api.key",
                            "jnosql.qdrant.timeout",
                            "jnosql.qdrant.collection.distance",
                            "jnosql.qdrant.collection.vector.size");
        }

        @Test
        @DisplayName("Should not repeat keys")
        void shouldHaveUniqueKeys() {
            assertThat(Arrays.stream(QdrantConfigurations.values()).map(QdrantConfigurations::get))
                    .doesNotHaveDuplicates();
        }
    }
}
