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

import org.apache.iotdb.isession.pool.ITableSessionPool;
import org.apache.iotdb.session.pool.TableSessionPoolBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Creates database-bound managers backed by IoTDB native session pools.
 */
public class IoTDBTimeSeriesManagerFactory implements DatabaseManagerFactory {

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final int poolSize;
    private final long queryTimeout;
    private final boolean redirection;
    private final List<ITableSessionPool> pools = new ArrayList<>();
    private boolean closed;

    IoTDBTimeSeriesManagerFactory(String host, int port, String username, String password,
                                  int poolSize, long queryTimeout, boolean redirection) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.poolSize = poolSize;
        this.queryTimeout = queryTimeout;
        this.redirection = redirection;
    }

    @Override
    public synchronized IoTDBTimeSeriesManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        IoTDBEntityConverter.identifier(database);
        if (closed) {
            throw new IllegalStateException("IoTDB manager factory is closed");
        }
        ITableSessionPool pool = new TableSessionPoolBuilder()
                .nodeUrls(List.of(host + ":" + port))
                .user(username)
                .password(password)
                .database(database)
                .maxSize(poolSize)
                .queryTimeoutInMs(queryTimeout)
                .enableRedirection(redirection)
                .build();
        pools.add(pool);
        return new IoTDBTimeSeriesManager(pool, database);
    }

    @Override
    public synchronized void close() {
        closed = true;
        pools.forEach(ITableSessionPool::close);
        pools.clear();
    }
}
