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
package org.eclipse.jnosql.databases.influxdb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class InfluxDBTimeSeriesConfigurationTest {

    @Test
    void shouldCreateFactoryFromSettings() {
        Settings settings = Settings.of(Map.of(
                InfluxDBTimeSeriesConfigurations.URL.get(), "http://localhost:8181",
                InfluxDBTimeSeriesConfigurations.TOKEN.get(), "token"));

        InfluxDBTimeSeriesManagerFactory factory = new InfluxDBTimeSeriesConfiguration().apply(settings);

        assertThat(factory.apply("metrics").name()).isEqualTo("metrics");
        factory.close();
    }

    @Test
    void shouldRejectBlankDatabase() {
        Settings settings = Settings.of(Map.of(
                InfluxDBTimeSeriesConfigurations.URL.get(), "http://localhost:8181",
                InfluxDBTimeSeriesConfigurations.TOKEN.get(), "token"));
        InfluxDBTimeSeriesManagerFactory factory = new InfluxDBTimeSeriesConfiguration().apply(settings);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> factory.apply(" "));
    }

    @Test
    void shouldRequireEveryConnectionSetting() {
        InfluxDBTimeSeriesConfiguration configuration = new InfluxDBTimeSeriesConfiguration();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> configuration.apply(Settings.builder().build()))
                .withMessageContaining(InfluxDBTimeSeriesConfigurations.URL.get());
    }

    @Test
    void shouldLoadFromServiceLoader() {
        InfluxDBTimeSeriesConfiguration configuration =
                DatabaseConfiguration.getConfiguration(InfluxDBTimeSeriesConfiguration.class);

        assertThat(configuration).isNotNull();
    }
}
