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

import io.milvus.v2.common.ConsistencyLevel;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class MilvusSettingsTest {

    @Nested
    @DisplayName("When reading the connection settings")
    class WhenTheConnectionIsRead {

        @Test
        @DisplayName("Should use the local defaults")
        void shouldUseDefaults() {
            var connection = MilvusConnectionSettings.from(Settings.builder().build());

            assertSoftly(soft -> {
                soft.assertThat(connection.uri()).isEqualTo("http://localhost:19530");
                soft.assertThat(connection.tokenOptional()).isEmpty();
                soft.assertThat(connection.timeoutOptional()).isEmpty();
            });
        }

        @Test
        @DisplayName("Should apply host, port, TLS, token and timeout")
        void shouldApplyExplicit() {
            var connection = MilvusConnectionSettings.from(Settings.builder()
                    .put(MilvusConfigurations.HOST, "milvus.example.com")
                    .put(MilvusConfigurations.PORT, 443)
                    .put(MilvusConfigurations.TLS, true)
                    .put(MilvusConfigurations.TOKEN, "user:pwd")
                    .put(MilvusConfigurations.TIMEOUT, 5000L)
                    .build());

            assertSoftly(soft -> {
                soft.assertThat(connection.uri()).isEqualTo("https://milvus.example.com:443");
                soft.assertThat(connection.tokenOptional()).contains("user:pwd");
                soft.assertThat(connection.timeoutOptional()).contains(Duration.ofSeconds(5));
            });
        }

        @Test
        @DisplayName("Should use the generic host when the Milvus one is absent")
        void shouldUseGenericHost() {
            var connection = MilvusConnectionSettings.from(Settings.builder()
                    .put(Configurations.HOST, "generic.example.com").build());

            assertThat(connection.uri()).isEqualTo("http://generic.example.com:19530");
        }

        @Test
        @DisplayName("Should prefer the URI over host, port and TLS")
        void shouldPreferUri() {
            var connection = MilvusConnectionSettings.from(Settings.builder()
                    .put(MilvusConfigurations.URI, "https://cluster.example.com:8443")
                    .put(MilvusConfigurations.HOST, "ignored").build());

            assertThat(connection.uri()).isEqualTo("https://cluster.example.com:8443");
        }
    }

    @Nested
    @DisplayName("When reading the collection settings")
    class WhenTheCollectionIsRead {

        @Test
        @DisplayName("Should use cosine, strong consistency and the maximum text length by default")
        void shouldUseDefaults() {
            var collection = MilvusCollectionSettings.from(Settings.builder().build());

            assertThat(collection).isEqualTo(MilvusCollectionSettings.DEFAULT);
            assertSoftly(soft -> {
                soft.assertThat(collection.metric()).isEqualTo(MilvusMetric.COSINE);
                soft.assertThat(collection.vectorSize()).isNull();
                soft.assertThat(collection.consistency()).isEqualTo(ConsistencyLevel.STRONG);
                soft.assertThat(collection.maxLength()).isEqualTo(65_535);
            });
        }

        @Test
        @DisplayName("Should apply metric, vector size, consistency and text length")
        void shouldApplyExplicit() {
            var collection = MilvusCollectionSettings.from(Settings.builder()
                    .put(MilvusConfigurations.METRIC, "l2")
                    .put(MilvusConfigurations.VECTOR_SIZE, 128)
                    .put(MilvusConfigurations.CONSISTENCY, "bounded")
                    .put(MilvusConfigurations.MAX_LENGTH, 256)
                    .build());

            assertThat(collection).isEqualTo(new MilvusCollectionSettings(MilvusMetric.EUCLIDEAN, 128,
                    ConsistencyLevel.BOUNDED, 256));
        }

        @Test
        @DisplayName("Should reject invalid values")
        void shouldRejectInvalid() {
            assertSoftly(soft -> {
                soft.assertThatThrownBy(() -> MilvusCollectionSettings.from(Settings.builder()
                        .put(MilvusConfigurations.METRIC, "hamming").build()))
                        .isInstanceOf(IllegalArgumentException.class);
                soft.assertThatThrownBy(() -> MilvusCollectionSettings.from(Settings.builder()
                        .put(MilvusConfigurations.CONSISTENCY, "never").build()))
                        .isInstanceOf(IllegalArgumentException.class);
                soft.assertThatThrownBy(() -> MilvusCollectionSettings.from(Settings.builder()
                        .put(MilvusConfigurations.VECTOR_SIZE, 0).build()))
                        .isInstanceOf(IllegalArgumentException.class);
                soft.assertThatThrownBy(() -> MilvusCollectionSettings.from(Settings.builder()
                        .put(MilvusConfigurations.MAX_LENGTH, 70_000).build()))
                        .isInstanceOf(IllegalArgumentException.class);
            });
            assertThatIllegalArgumentException().isThrownBy(() -> MilvusCollectionSettings.from(
                    Settings.builder().put(MilvusConfigurations.MAX_LENGTH, 0).build()));
        }
    }
}
