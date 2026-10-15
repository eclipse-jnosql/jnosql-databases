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

import io.qdrant.client.QdrantGrpcClient;
import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class QdrantConnectionTest {

    private Settings settings(String apiKey) {
        return VectorDatabase.INSTANCE.securedSettings(apiKey != null);
    }

    @Nested
    @DisplayName("When connecting to a running Qdrant")
    class WhenTheConnectionIsOpened {

        @Test
        @DisplayName("Should reach the server using the configured API key")
        void shouldConnect() throws Exception {
            try (var factory = new QdrantConfiguration().apply(settings(VectorDatabase.API_KEY))) {
                var health = factory.client().healthCheckAsync().get(10, TimeUnit.SECONDS);

                assertThat(health.getTitle()).isNotBlank();
            }
        }

        @Test
        @DisplayName("Should reject the call when the API key is missing")
        void shouldRejectMissingApiKey() {
            try (var factory = new QdrantConfiguration().apply(settings(null))) {
                assertThatThrownBy(() -> factory.client().listCollectionsAsync().get(10, TimeUnit.SECONDS))
                        .hasMessageContaining("UNAUTHENTICATED");
            }
        }
    }

    @Nested
    @DisplayName("When closing the factory")
    class WhenTheClosing {

        @Test
        @DisplayName("Should shut down the connection")
        void shouldShutDownConnection() {
            var factory = new QdrantConfiguration().apply(settings(VectorDatabase.API_KEY));
            QdrantGrpcClient grpc = factory.client().grpcClient();

            factory.close();

            assertThat(grpc.channel().isShutdown()).isTrue();
        }

        @Test
        @DisplayName("Should keep a client provided by the caller usable until the factory closes")
        void shouldKeepProvidedClientOpenUntilClose() {
            var client = VectorDatabase.INSTANCE.client();
            var factory = new QdrantConfiguration().get(client);

            factory.apply("library").close();
            assertThat(client.grpcClient().channel().isShutdown()).isFalse();

            factory.close();
            assertThat(client.grpcClient().channel().isShutdown()).isTrue();
        }
    }
}
