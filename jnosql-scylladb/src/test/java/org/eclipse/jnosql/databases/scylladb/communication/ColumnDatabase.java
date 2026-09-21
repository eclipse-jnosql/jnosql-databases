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
import org.eclipse.jnosql.communication.driver.ConfigurationReader;
import org.testcontainers.scylladb.ScyllaDBContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum ColumnDatabase implements Supplier<ScyllaDBColumnManagerFactory> {

    INSTANCE;

    private final ScyllaDBContainer scylladb =
            new ScyllaDBContainer("scylladb/scylla:2026.3.1");

    {
        scylladb.start();
    }

    @Override
    public ScyllaDBColumnManagerFactory get() {
        Settings settings = getSettings();
        ScyllaDBConfiguration scylladbConfiguration = new ScyllaDBConfiguration();
        return scylladbConfiguration.apply(settings);
    }

    public Settings getSettings() {
        Map<String, Object> configuration = new HashMap<>(ConfigurationReader.from("scylladb.properties"));
        configuration.put(ScyllaDBConfigurations.HOST.get()+".1", host());
        configuration.put(ScyllaDBConfigurations.PORT.get(), port());
        return Settings.of(configuration);
    }

    public String host() {
        return scylladb.getHost();
    }

    public int port() {
        return scylladb.getFirstMappedPort();
    }
}
