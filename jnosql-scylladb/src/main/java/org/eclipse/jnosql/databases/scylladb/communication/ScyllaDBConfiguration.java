/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.scylladb.communication;


import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A ScyllaDB-specific implementation of {@link DatabaseConfiguration}, which provides
 * {@link ScyllaDBColumnManagerFactory} instances.
 *
 * @see ScyllaDBConfigurations
 */
public final class ScyllaDBConfiguration implements DatabaseConfiguration {

    /**
     * Retrieves the {@link ScyllaDBColumnManagerFactory} based on the provided configurations.
     *
     * @param configurations the configurations for ScyllaDB
     * @return a {@link ScyllaDBColumnManagerFactory} instance
     * @throws NullPointerException if configurations are null
     */
    private ScyllaDBColumnManagerFactory getManagerFactory(Map<String, String> configurations) {
        Objects.requireNonNull(configurations);
        ScyllaDBProperties properties = ScyllaDBProperties.of(configurations);
        return new ScyllaDBColumnManagerFactory(properties.createCluster(), properties.getQueries());
    }

    /**
     * Applies the settings to create a {@link ScyllaDBColumnManagerFactory}.
     *
     * @param settings the settings to apply
     * @return a {@link ScyllaDBColumnManagerFactory} instance
     * @throws NullPointerException if settings are null
     */
    @Override
    public ScyllaDBColumnManagerFactory apply(Settings settings) throws NullPointerException {
        Objects.requireNonNull(settings, "Settings is required");
        Map<String, String> configurations = new HashMap<>();

        List<String> keys = settings.keySet()
                .stream()
                .filter(k -> k.startsWith("jnosql."))
                .toList();

        for (String key : keys) {
            settings.get(key, String.class).ifPresent(v -> configurations.put(key, v));
        }
        return getManagerFactory(configurations);
    }

}
