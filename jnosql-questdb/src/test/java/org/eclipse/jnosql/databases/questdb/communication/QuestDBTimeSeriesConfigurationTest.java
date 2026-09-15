/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.questdb.communication;

import io.questdb.client.QuestDB;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class QuestDBTimeSeriesConfigurationTest {

    @Test
    void shouldCreateFactoryFromSettings() {
        Settings settings = Settings.of(Map.of(
                QuestDBTimeSeriesConfigurations.URL.get(), "ws::addr=localhost:9000;"));

        QuestDBTimeSeriesManagerFactory factory = new QuestDBTimeSeriesConfiguration().apply(settings);

        assertThat(factory).isNotNull();
        factory.close();
    }

    @Test
    void shouldRejectManagerCreationAfterClose() {
        QuestDBTimeSeriesManagerFactory factory =
                new QuestDBTimeSeriesManagerFactory(mock(QuestDB.class));
        factory.close();

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> factory.apply("qdb"))
                .withMessageContaining("closed");
    }

    @Test
    void shouldRequireEveryConnectionSetting() {
        QuestDBTimeSeriesConfiguration configuration = new QuestDBTimeSeriesConfiguration();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> configuration.apply(Settings.builder().build()))
                .withMessageContaining(QuestDBTimeSeriesConfigurations.URL.get());
    }

    @Test
    void shouldLoadFromServiceLoader() {
        QuestDBTimeSeriesConfiguration configuration =
                DatabaseConfiguration.getConfiguration(QuestDBTimeSeriesConfiguration.class);

        assertThat(configuration).isNotNull();
    }
}
