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

import io.milvus.v2.client.MilvusClientV2;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.ServiceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MilvusConfigurationTest {

    private final MilvusConfiguration configuration = new MilvusConfiguration();

    @Test
    @DisplayName("Should be discoverable as a DatabaseConfiguration")
    void shouldBeDiscoverable() {
        assertThat(ServiceLoader.load(DatabaseConfiguration.class))
                .anyMatch(MilvusConfiguration.class::isInstance);
    }

    @Test
    @DisplayName("Should create the factory from a client with the default collection settings")
    void shouldCreateFromClient() {
        var client = Mockito.mock(MilvusClientV2.class);

        try (var factory = configuration.get(client)) {
            assertThat(factory.client()).isSameAs(client);
            assertThat(factory.collectionSettings()).isEqualTo(MilvusCollectionSettings.DEFAULT);
        }
    }

    @Test
    @DisplayName("Should reject null arguments")
    void shouldRejectNulls() {
        assertThatNullPointerException().isThrownBy(() -> configuration.get((MilvusClientV2) null));
        assertThatNullPointerException().isThrownBy(() -> configuration.get((Map<String, String>) null));
        assertThatNullPointerException().isThrownBy(() -> configuration.apply(null));
    }
}
