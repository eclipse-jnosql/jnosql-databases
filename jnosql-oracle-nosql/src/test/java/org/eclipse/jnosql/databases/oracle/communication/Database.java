/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Supplier;

public enum Database implements Supplier<BucketManagerFactory> {

    INSTANCE;
    private final GenericContainer<?> container = new GenericContainer<>
            (DockerImageName.parse("ghcr.io/oracle/nosql:latest-ce"))
            .withExposedPorts(8080);

    {
        container.start();
    }


    @Override
    public BucketManagerFactory get() {
        KeyValueConfiguration configuration = new OracleNoSQLKeyValueConfiguration();
        Settings settings = Settings.builder()
                .put(OracleNoSQLConfigurations.HOST, host())
                .build();
        return configuration.apply(settings);
    }

    public String host() {
        return "http://" + container.getHost() + ":" + container.getFirstMappedPort();
    }

    public DatabaseManagerFactory managerFactory() {
        DatabaseConfiguration configuration = DatabaseConfiguration.getConfiguration();
        Settings settings = Settings.builder()
                .put(OracleNoSQLConfigurations.HOST, host())
                .build();
        return configuration.apply(settings);
    }
}
