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

import org.apache.iotdb.isession.ITableSession;
import org.apache.iotdb.isession.pool.ITableSessionPool;
import org.apache.iotdb.session.pool.TableSessionPoolBuilder;
import org.eclipse.jnosql.communication.Settings;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

public enum IoTDBDatabase {

    INSTANCE;

    public static final String DATABASE = "jnosql";
    private static final int RPC_PORT = 6667;

    private final GenericContainer<?> iotDB = new GenericContainer<>(
            DockerImageName.parse("apache/iotdb:2.0.11-standalone"))
            .withExposedPorts(RPC_PORT)
            .withEnv("dn_rpc_address", "0.0.0.0")
            .waitingFor(Wait.forLogMessage(".*DataNode started.*\\n", 1));

    {
        iotDB.start();
        try (ITableSessionPool pool = new TableSessionPoolBuilder()
                .nodeUrls(List.of(nodeUrl()))
                .user("root")
                .password("root")
                .enableRedirection(false)
                .maxSize(1)
                .build();
             ITableSession session = pool.getSession()) {
            session.executeNonQueryStatement("CREATE DATABASE IF NOT EXISTS " + DATABASE);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not initialize the IoTDB test database", exception);
        }
    }

    public IoTDBTimeSeriesManager manager() {
        Settings settings = Settings.of(Map.of(
                IoTDBTimeSeriesConfigurations.HOST.get(), iotDB.getHost(),
                IoTDBTimeSeriesConfigurations.PORT.get(), Integer.toString(iotDB.getMappedPort(RPC_PORT)),
                IoTDBTimeSeriesConfigurations.ENABLE_REDIRECTION.get(), "false"));
        return new IoTDBTimeSeriesConfiguration().apply(settings).apply(DATABASE);
    }

    public String host() {
        return iotDB.getHost();
    }

    public int port() {
        return iotDB.getMappedPort(RPC_PORT);
    }

    private String nodeUrl() {
        return host() + ":" + port();
    }
}
