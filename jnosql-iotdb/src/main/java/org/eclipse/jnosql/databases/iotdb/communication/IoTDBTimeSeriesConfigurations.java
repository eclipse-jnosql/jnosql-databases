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

import java.util.function.Supplier;

/**
 * Connection settings for Apache IoTDB's native Table Model client.
 */
public enum IoTDBTimeSeriesConfigurations implements Supplier<String> {

    /**
     * The IoTDB server host. Defaults to {@code localhost}.
     */
    HOST("jnosql.iotdb.host"),
    /**
     * The IoTDB RPC port, a positive integer. Defaults to {@code 6667}.
     */
    PORT("jnosql.iotdb.port"),
    /**
     * The user name. Defaults to {@code root}.
     */
    USERNAME("jnosql.iotdb.username"),
    /**
     * The user password. Defaults to {@code root}.
     */
    PASSWORD("jnosql.iotdb.password"),
    /**
     * The maximum number of sessions in each pool, a positive integer. Defaults to {@code 5}.
     */
    POOL_SIZE("jnosql.iotdb.pool.size"),
    /**
     * The query timeout in milliseconds, a positive number. Defaults to {@code 60000}.
     */
    QUERY_TIMEOUT("jnosql.iotdb.query.timeout"),
    /**
     * Whether the client redirects requests to the node that owns the data. Defaults to {@code true}.
     */
    ENABLE_REDIRECTION("jnosql.iotdb.enable.redirection");

    private final String configuration;

    IoTDBTimeSeriesConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
