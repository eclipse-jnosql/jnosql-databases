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

    HOST("jnosql.iotdb.host"),
    PORT("jnosql.iotdb.port"),
    USERNAME("jnosql.iotdb.username"),
    PASSWORD("jnosql.iotdb.password"),
    POOL_SIZE("jnosql.iotdb.pool.size"),
    QUERY_TIMEOUT("jnosql.iotdb.query.timeout"),
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
