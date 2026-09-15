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
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Connects Eclipse JNoSQL to Apache IoTDB's native Table Model API.
 */
public class IoTDBTimeSeriesConfiguration implements DatabaseConfiguration {

    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 6667;
    static final String DEFAULT_USERNAME = "root";
    static final String DEFAULT_PASSWORD = "root";
    static final int DEFAULT_POOL_SIZE = 5;
    static final long DEFAULT_QUERY_TIMEOUT = 60_000L;

    public IoTDBTimeSeriesConfiguration() {
    }

    public IoTDBTimeSeriesManagerFactory get(Map<String, String> configurations) {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    @Override
    public IoTDBTimeSeriesManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        return new IoTDBTimeSeriesManagerFactory(
                text(settings, IoTDBTimeSeriesConfigurations.HOST, DEFAULT_HOST),
                number(settings, IoTDBTimeSeriesConfigurations.PORT, DEFAULT_PORT),
                text(settings, IoTDBTimeSeriesConfigurations.USERNAME, DEFAULT_USERNAME),
                text(settings, IoTDBTimeSeriesConfigurations.PASSWORD, DEFAULT_PASSWORD),
                number(settings, IoTDBTimeSeriesConfigurations.POOL_SIZE, DEFAULT_POOL_SIZE),
                longNumber(settings, IoTDBTimeSeriesConfigurations.QUERY_TIMEOUT, DEFAULT_QUERY_TIMEOUT),
                bool(settings, IoTDBTimeSeriesConfigurations.ENABLE_REDIRECTION, true));
    }

    private String text(Settings settings, IoTDBTimeSeriesConfigurations configuration, String defaultValue) {
        return settings.get(configuration, String.class)
                .filter(value -> !value.isBlank())
                .orElse(defaultValue);
    }

    private int number(Settings settings, IoTDBTimeSeriesConfigurations configuration, int defaultValue) {
        int value = settings.get(configuration, String.class)
                .map(Integer::parseInt)
                .orElse(defaultValue);
        if (value <= 0) {
            throw new IllegalArgumentException(configuration.get() + " must be greater than zero");
        }
        return value;
    }

    private long longNumber(Settings settings, IoTDBTimeSeriesConfigurations configuration, long defaultValue) {
        long value = settings.get(configuration, String.class)
                .map(Long::parseLong)
                .orElse(defaultValue);
        if (value <= 0) {
            throw new IllegalArgumentException(configuration.get() + " must be greater than zero");
        }
        return value;
    }

    private boolean bool(Settings settings, IoTDBTimeSeriesConfigurations configuration, boolean defaultValue) {
        return settings.get(configuration, String.class)
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }
}
