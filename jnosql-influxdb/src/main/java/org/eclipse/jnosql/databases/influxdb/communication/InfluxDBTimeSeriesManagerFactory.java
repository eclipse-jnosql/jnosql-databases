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

import com.influxdb.v3.client.InfluxDBClient;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Creates time-series managers for databases on one InfluxDB 3 server.
 *
 * <p>InfluxDB 3 clients are database-bound, so each manager receives its own client.
 * Closing this factory closes every client it created.</p>
 */
public class InfluxDBTimeSeriesManagerFactory implements DatabaseManagerFactory {

    private final String url;

    private final char[] token;

    private final List<InfluxDBClient> clients = new ArrayList<>();

    private boolean closed;

    InfluxDBTimeSeriesManagerFactory(String url, char[] token) {
        this.url = url;
        this.token = token.clone();
    }

    /**
     * Targets an InfluxDB database for subsequent time-series operations.
     *
     * @param database database containing the measurements
     * @return a manager connected to the requested database
     * @throws NullPointerException if {@code database} is {@code null}
     * @throws IllegalArgumentException if {@code database} is blank
     * @throws IllegalStateException if this factory is closed
     */
    @Override
    public synchronized InfluxDBTimeSeriesManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        if (database.isBlank()) {
            throw new IllegalArgumentException("database is required");
        }
        if (closed) {
            throw new IllegalStateException("InfluxDB manager factory is closed");
        }
        InfluxDBClient client = InfluxDBClient.getInstance(url, token, database);
        clients.add(client);
        return new InfluxDBTimeSeriesManager(client, database);
    }

    /**
     * Closes all database clients and releases their HTTP and Arrow Flight resources.
     */
    @Override
    public synchronized void close() {
        closed = true;
        IllegalStateException failure = null;
        for (InfluxDBClient client : clients) {
            try {
                client.close();
            } catch (Exception exception) {
                if (failure == null) {
                    failure = new IllegalStateException("Could not close an InfluxDB client", exception);
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        clients.clear();
        Arrays.fill(token, '\0');
        if (failure != null) {
            throw failure;
        }
    }
}
