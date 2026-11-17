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

import static java.util.Objects.requireNonNull;

/**
 * The Oracle NoSQL implementation of {@link KeyValueConfiguration}. It builds the connection from
 * {@link OracleNoSQLConfigurations} and returns a factory whose buckets are Oracle NoSQL tables.
 *
 * <p>Example:</p>
 * {@snippet lang="java" :
 * Settings settings = Settings.builder()
 *         .put(OracleNoSQLConfigurations.HOST, "http://localhost:8080")
 *         .put(OracleNoSQLConfigurations.DEPLOYMENT, "ON_PREMISES")
 *         .build();
 * BucketManagerFactory factory = new OracleNoSQLKeyValueConfiguration().apply(settings);
 * }
 *
 * @see OracleNoSQLConfigurations
 */
public final class OracleNoSQLKeyValueConfiguration implements KeyValueConfiguration {
    @Override
    public BucketManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");

        var nosql =  NoSQLHandleConfigConfiguration.INSTANCE.apply(settings);
        return new OracleNoSQLBucketManagerFactory(nosql);
    }
}
