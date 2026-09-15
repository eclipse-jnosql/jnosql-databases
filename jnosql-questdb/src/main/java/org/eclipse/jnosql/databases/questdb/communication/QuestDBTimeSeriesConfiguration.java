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

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Connects Eclipse JNoSQL to a QuestDB server.
 */
public class QuestDBTimeSeriesConfiguration implements DatabaseConfiguration {

    /**
     * Creates a QuestDB configuration.
     */
    public QuestDBTimeSeriesConfiguration() {
    }

    /**
     * Opens a factory using the supplied QuestDB connection properties.
     *
     * @param configurations QuestDB properties
     * @return a QuestDB time-series manager factory
     */
    public QuestDBTimeSeriesManagerFactory get(Map<String, String> configurations) {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    @Override
    public QuestDBTimeSeriesManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        return new QuestDBTimeSeriesManagerFactory(
                required(settings, QuestDBTimeSeriesConfigurations.URL));
    }

    private String required(Settings settings, QuestDBTimeSeriesConfigurations configuration) {
        return settings.get(configuration, String.class)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(
                        "The configuration '" + configuration.get() + "' is required"));
    }
}
