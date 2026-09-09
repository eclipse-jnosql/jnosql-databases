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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Map;

public enum InfluxDBDatabase {

    INSTANCE;

    public static final String DATABASE = "metrics";

    private static final String TOKEN = "jnosql-influxdb-test-token";

    private final GenericContainer<?> influxDB = new GenericContainer<>("influxdb:3.11.0-core")
            .withExposedPorts(8181)
            .withCommand("influxdb3", "serve",
                    "--node-id", "jnosql",
                    "--object-store", "memory",
                    "--without-auth")
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    {
        influxDB.start();
        try {
            Container.ExecResult result = influxDB.execInContainer(
                    "influxdb3", "create", "database",
                    "--host", "http://localhost:8181", DATABASE);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Could not create InfluxDB test database: " + result.getStderr());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Could not initialize InfluxDB test database", exception);
        }
    }

    public InfluxDBTimeSeriesManager manager() {
        Settings settings = Settings.of(Map.of(
                InfluxDBTimeSeriesConfigurations.URL.get(),
                url(),
                InfluxDBTimeSeriesConfigurations.TOKEN.get(), TOKEN));
        return new InfluxDBTimeSeriesConfiguration().apply(settings).apply(DATABASE);
    }

    public String url() {
        return "http://" + influxDB.getHost() + ":" + influxDB.getFirstMappedPort();
    }

    public String token() {
        return TOKEN;
    }
}
