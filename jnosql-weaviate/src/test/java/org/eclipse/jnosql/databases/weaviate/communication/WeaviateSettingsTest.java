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

import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WeaviateSettingsTest {

    @Test
    @DisplayName("Should use the defaults")
    void shouldUseDefaults() {
        var connection = WeaviateConnectionSettings.from(Settings.builder().build());

        assertThat(connection.host()).isEqualTo("localhost");
        assertThat(connection.httpPort()).isEqualTo(8080);
        assertThat(connection.grpcPort()).isEqualTo(50051);
        assertThat(connection.secure()).isFalse();
        assertThat(connection.apiKeyOptional()).isEmpty();
        assertThat(connection.timeoutOptional()).isEmpty();
        assertThat(WeaviateCollectionSettings.from(Settings.builder().build()))
                .isEqualTo(WeaviateCollectionSettings.DEFAULT);
    }

    @Test
    @DisplayName("Should read the configured values")
    void shouldReadValues() {
        var settings = Settings.builder()
                .put(WeaviateConfigurations.HOST, "db")
                .put(WeaviateConfigurations.HTTP_PORT, "9000")
                .put(WeaviateConfigurations.GRPC_PORT, "9001")
                .put(WeaviateConfigurations.SECURE, "true")
                .put(WeaviateConfigurations.API_KEY, "key")
                .put(WeaviateConfigurations.TIMEOUT, "5")
                .put(WeaviateConfigurations.DISTANCE, "dot")
                .build();

        var connection = WeaviateConnectionSettings.from(settings);

        assertThat(connection).isEqualTo(new WeaviateConnectionSettings("db", 9000, 9001, true, "key", 5));
        assertThat(WeaviateCollectionSettings.from(settings).distance()).isEqualTo(WeaviateDistance.DOT);
    }

    @Test
    @DisplayName("Should reject invalid ports and timeouts")
    void shouldRejectInvalid() {
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateConnectionSettings
                .from(Settings.builder().put(WeaviateConfigurations.HTTP_PORT, "0").build()));
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateConnectionSettings
                .from(Settings.builder().put(WeaviateConfigurations.GRPC_PORT, "70000").build()));
        assertThatIllegalArgumentException().isThrownBy(() -> WeaviateConnectionSettings
                .from(Settings.builder().put(WeaviateConfigurations.TIMEOUT, "0").build()));
    }
}
