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
package org.eclipse.jnosql.databases.iotdb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class IoTDBTimeSeriesConfigurationTest {

    @Test
    void shouldCreateFactoryWithDefaultsAndConfiguredValues() {
        IoTDBTimeSeriesConfiguration configuration = new IoTDBTimeSeriesConfiguration();

        assertThat(configuration.apply(Settings.of(Map.of()))).isNotNull();
        assertThat(configuration.get(Map.of(
                IoTDBTimeSeriesConfigurations.HOST.get(), "iotdb",
                IoTDBTimeSeriesConfigurations.PORT.get(), "16667",
                IoTDBTimeSeriesConfigurations.POOL_SIZE.get(), "2"))).isNotNull();
    }

    @Test
    void shouldRejectInvalidNumericSettings() {
        IoTDBTimeSeriesConfiguration configuration = new IoTDBTimeSeriesConfiguration();
        Settings settings = Settings.of(Map.of(IoTDBTimeSeriesConfigurations.PORT.get(), "0"));

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> configuration.apply(settings))
                .withMessageContaining(IoTDBTimeSeriesConfigurations.PORT.get());
    }

    @Test
    void shouldLoadThroughServiceLoader() {
        IoTDBTimeSeriesConfiguration configuration =
                DatabaseConfiguration.getConfiguration(IoTDBTimeSeriesConfiguration.class);

        assertThat(configuration).isNotNull();
    }
}
