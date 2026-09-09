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

import java.util.function.Supplier;

/**
 * Connection settings used to authenticate with an InfluxDB 3 server.
 *
 * <p>The database is supplied separately when creating a manager. Applications using
 * Eclipse JNoSQL Mapping set it with {@code jnosql.timeseries.database}.</p>
 */
public enum InfluxDBTimeSeriesConfigurations implements Supplier<String> {

    /**
     * Base URL of the InfluxDB 3 server, for example {@code http://localhost:8181}.
     */
    URL("jnosql.influxdb.url"),

    /**
     * Database token authorized to read and write the configured database.
     */
    TOKEN("jnosql.influxdb.token");

    private final String configuration;

    InfluxDBTimeSeriesConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
