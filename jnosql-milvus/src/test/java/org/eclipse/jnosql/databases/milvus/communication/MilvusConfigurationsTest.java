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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MilvusConfigurationsTest {

    @Test
    @DisplayName("Should expose the configuration keys")
    void shouldExposeKeys() {
        assertThat(Arrays.stream(MilvusConfigurations.values()).map(MilvusConfigurations::get))
                .containsExactly("jnosql.milvus.host", "jnosql.milvus.port", "jnosql.milvus.tls",
                        "jnosql.milvus.uri", "jnosql.milvus.token", "jnosql.milvus.timeout",
                        "jnosql.milvus.collection.metric", "jnosql.milvus.collection.vector.size",
                        "jnosql.milvus.consistency", "jnosql.milvus.collection.string.max.length");
    }
}
