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
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Connects Eclipse JNoSQL to an InfluxDB 3 server.
 *
 * <p>A connection requires the server URL and database token. The database name passed to
 * the resulting factory is used for both SQL queries and line-protocol writes.</p>
 */
public class InfluxDBTimeSeriesConfiguration implements DatabaseConfiguration {

    /**
     * Reads connection settings when a factory is requested; clients are opened only after
     * the application selects an InfluxDB database.
     */
    public InfluxDBTimeSeriesConfiguration() {
    }

    /**
     * Opens a client using the supplied InfluxDB connection properties.
     *
     * @param configurations properties keyed by {@link InfluxDBTimeSeriesConfigurations}
     * @return a factory that creates managers for InfluxDB databases
     * @throws NullPointerException if {@code configurations} is {@code null}
     * @throws IllegalArgumentException if a required connection property is absent or blank
     */
    public InfluxDBTimeSeriesManagerFactory get(Map<String, String> configurations) {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    @Override
    public InfluxDBTimeSeriesManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        String url = required(settings, InfluxDBTimeSeriesConfigurations.URL);
        String token = required(settings, InfluxDBTimeSeriesConfigurations.TOKEN);
        return new InfluxDBTimeSeriesManagerFactory(url, token.toCharArray());
    }

    private String required(Settings settings, InfluxDBTimeSeriesConfigurations configuration) {
        return settings.get(configuration, String.class)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(
                        "The configuration '" + configuration.get() + "' is required"));
    }
}
