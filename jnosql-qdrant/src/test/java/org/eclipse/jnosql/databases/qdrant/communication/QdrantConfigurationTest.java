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

import io.qdrant.client.QdrantClient;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class QdrantConfigurationTest {

    private final QdrantConfiguration configuration = new QdrantConfiguration();

    @Nested
    @DisplayName("When creating the factory with default settings")
    class WhenTheDefaultsAreApplied {

        @Test
        @DisplayName("Should create a factory without any setting")
        void shouldCreateFactory() {
            try (var factory = configuration.apply(Settings.builder().build())) {
                assertSoftly(softly -> {
                    softly.assertThat(factory).as("factory").isNotNull();
                    softly.assertThat(factory.client()).as("client").isNotNull();
                });
            }
        }

        @Test
        @DisplayName("Should leave the collection settings undefined")
        void shouldLeaveCollectionSettingsEmpty() {
            try (var factory = configuration.apply(Settings.builder().build())) {
                var collection = factory.collectionSettings();
                assertSoftly(softly -> {
                    softly.assertThat(collection.distanceOptional()).as("distance").isEmpty();
                    softly.assertThat(collection.vectorSizeOptional()).as("vector size").isEmpty();
                });
            }
        }
    }

    @Nested
    @DisplayName("When creating the factory with explicit settings")
    class WhenTheSettingsAreExplicit {

        @Test
        @DisplayName("Should apply host, port, TLS, API key and timeout")
        void shouldApplyConnectionSettings() {
            var settings = Settings.builder()
                    .put(QdrantConfigurations.HOST, "qdrant.example.com")
                    .put(QdrantConfigurations.PORT, 7334)
                    .put(QdrantConfigurations.TLS, true)
                    .put(QdrantConfigurations.API_KEY, "secret")
                    .put(QdrantConfigurations.TIMEOUT, 5000L)
                    .build();

            try (var factory = configuration.apply(settings)) {
                assertThat(factory.client().grpcClient().channel().authority())
                        .isEqualTo("qdrant.example.com:7334");
            }
        }

        @Test
        @DisplayName("Should use the generic host when the Qdrant one is absent")
        void shouldUseGenericHost() {
            var settings = Settings.builder().put(Configurations.HOST, "generic.example.com").build();

            try (var factory = configuration.apply(settings)) {
                assertThat(factory.client().grpcClient().channel().authority())
                        .isEqualTo("generic.example.com:6334");
            }
        }

        @Test
        @DisplayName("Should prefer the Qdrant host over the generic one")
        void shouldPreferQdrantHost() {
            var settings = Settings.builder()
                    .put(Configurations.HOST, "generic.example.com")
                    .put(QdrantConfigurations.HOST, "qdrant.example.com")
                    .build();

            try (var factory = configuration.apply(settings)) {
                assertThat(factory.client().grpcClient().channel().authority())
                        .isEqualTo("qdrant.example.com:6334");
            }
        }

        @Test
        @DisplayName("Should apply the collection distance and vector size")
        void shouldApplyCollectionSettings() {
            var settings = Settings.builder()
                    .put(QdrantConfigurations.DISTANCE, "euclid")
                    .put(QdrantConfigurations.VECTOR_SIZE, 384)
                    .build();

            try (var factory = configuration.apply(settings)) {
                var collection = factory.collectionSettings();
                assertSoftly(softly -> {
                    softly.assertThat(collection.distanceOptional()).as("distance")
                            .contains(QdrantDistance.EUCLID);
                    softly.assertThat(collection.vectorSizeOptional()).as("vector size").contains(384L);
                });
            }
        }

        @Test
        @DisplayName("Should accept every supported distance")
        void shouldAcceptEveryDistance() {
            for (QdrantDistance distance : QdrantDistance.values()) {
                var settings = Settings.builder().put(QdrantConfigurations.DISTANCE, distance.name()).build();
                try (var factory = configuration.apply(settings)) {
                    assertThat(factory.collectionSettings().distanceOptional()).contains(distance);
                }
            }
        }

        @Test
        @DisplayName("Should create the factory from a map")
        void shouldCreateFactoryFromMap() {
            Map<String, String> map = Map.of(
                    QdrantConfigurations.HOST.get(), "map.example.com",
                    QdrantConfigurations.DISTANCE.get(), "Dot");

            try (var factory = configuration.get(map)) {
                assertSoftly(softly -> {
                    softly.assertThat(factory.client().grpcClient().channel().authority())
                            .as("authority").isEqualTo("map.example.com:6334");
                    softly.assertThat(factory.collectionSettings().distanceOptional())
                            .as("distance").contains(QdrantDistance.DOT);
                });
            }
        }
    }

    @Nested
    @DisplayName("When creating the factory from an existing client")
    class WhenTheClientIsProvided {

        @Test
        @DisplayName("Should reuse the client")
        void shouldReuseClient() {
            var client = Mockito.mock(QdrantClient.class);

            var factory = configuration.get(client);

            assertSoftly(softly -> {
                softly.assertThat(factory.client()).as("client").isSameAs(client);
                softly.assertThat(factory.collectionSettings().distanceOptional()).as("distance").isEmpty();
            });
        }
    }

    @Nested
    @DisplayName("When validating the settings")
    class WhenTheValidation {

        @Test
        @DisplayName("Should reject an unsupported distance")
        void shouldRejectUnsupportedDistance() {
            var settings = Settings.builder().put(QdrantConfigurations.DISTANCE, "hamming").build();

            assertThatIllegalArgumentException().isThrownBy(() -> configuration.apply(settings))
                    .withMessageContaining("hamming");
        }

        @Test
        @DisplayName("Should reject a null settings")
        void shouldRejectNullSettings() {
            assertThatNullPointerException().isThrownBy(() -> configuration.apply(null));
        }

        @Test
        @DisplayName("Should reject a null map")
        void shouldRejectNullMap() {
            assertThatNullPointerException().isThrownBy(() -> configuration.get((Map<String, String>) null));
        }

        @Test
        @DisplayName("Should reject a null client")
        void shouldRejectNullClient() {
            assertThatNullPointerException().isThrownBy(() -> configuration.get((QdrantClient) null));
        }
    }

    @Nested
    @DisplayName("When discovering the configuration")
    class WhenTheDiscovery {

        @Test
        @DisplayName("Should be found by its type")
        void shouldFindByType() {
            assertThat(DatabaseConfiguration.getConfiguration(QdrantConfiguration.class)).isNotNull();
        }
    }
}
