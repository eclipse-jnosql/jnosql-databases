/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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

import oracle.nosql.driver.NoSQLHandleConfig;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

enum NoSQLHandleConfigConfiguration implements Function<Settings, NoSQLHandleConfiguration> {

    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(NoSQLHandleConfigConfiguration.class.getName());

    private static final String DEFAULT_HOST = "http://localhost:8080";
    private static final int DEFAULT_TABLE_READ_LIMITS = 25;
    private static final int DEFAULT_TABLE_WRITE_LIMITS = 25;
    private static final int DEFAULT_TABLE_STORAGE_GB = 25;
    private static final int DEFAULT_TABLE_WAIT_MILLIS = 120_000;
    private static final int DEFAULT_TABLE_DELAY_MILLIS = 500;
    @Override
    public NoSQLHandleConfiguration apply(Settings settings) {

        String host = settings.get(List.of(OracleNoSQLConfigurations.HOST.get(), Configurations.HOST.get()))
                .map(Object::toString).orElse(DEFAULT_HOST);


        DeploymentType deploymentType = settings.get(OracleNoSQLConfigurations.DEPLOYMENT.get())
                .map(Object::toString).map(DeploymentType::parse).orElse(DeploymentType.ON_PREMISES);

        LOGGER.info("Connecting to Oracle NoSQL database at " + host + " using " + deploymentType + " deployment type");

        NoSQLHandleConfig config = new NoSQLHandleConfig(host);

        deploymentType.apply(settings).ifPresent(config::setAuthorizationProvider);
        int readLimit = settings.getOrDefault(OracleNoSQLConfigurations.TABLE_READ_LIMITS, DEFAULT_TABLE_READ_LIMITS);
        int writeLimit = settings.getOrDefault(OracleNoSQLConfigurations.TABLE_WRITE_LIMITS, DEFAULT_TABLE_WRITE_LIMITS);
        int storageGB = settings.getOrDefault(OracleNoSQLConfigurations.TABLE_STORAGE_GB, DEFAULT_TABLE_STORAGE_GB);
        int waitMillis = settings.getOrDefault(OracleNoSQLConfigurations.TABLE_WAIT_MILLIS, DEFAULT_TABLE_WAIT_MILLIS);
        int delayMillis = settings.getOrDefault(OracleNoSQLConfigurations.TABLE_DELAY_MILLIS, DEFAULT_TABLE_DELAY_MILLIS);
        var tableLimits = new TableCreationConfiguration(readLimit, writeLimit, storageGB, waitMillis, delayMillis);
        settings.get(OracleNoSQLConfigurations.NAMESPACE.get())
                .map(Object::toString).ifPresent(config::setDefaultNamespace);
        settings.get(OracleNoSQLConfigurations.COMPARTMENT.get())
                .map(Object::toString).ifPresent(config::setDefaultCompartment);
        return new NoSQLHandleConfiguration(config, tableLimits);
    }
}
