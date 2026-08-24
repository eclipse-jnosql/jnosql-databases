/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.cassandra.communication;

import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the Cassandra database.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum CassandraConfigurations implements Supplier<String> {

    /**
     * The user's credential.
     */
    USER("jnosql.cassandra.user"),

    /**
     * The password's credential
     */
    PASSWORD("jnosql.cassandra.password"),
    /**
     * Database's host. It is a prefix to enumerate hosts. E.g.: jnosql.cassandra.host.1=localhost
     */
    HOST("jnosql.cassandra.host"),
    /**
     * The name of the application using the created session.
     */
    NAME("jnosql.cassandra.name"),
    /**
     * The cassandra's port
     */
    PORT("jnosql.cassandra.port"),
    /**
     * The Cassandra CQL to execute when the configuration starts. It uses as a prefix. E.g.: jnosql.cassandra.query.1=CQL
     */
    QUERY("jnosql.cassandra.query"),
    /**
     * The datacenter that is considered "local" by the load balancing policy.
     */
    DATA_CENTER("jnosql.cassandra.data.center");

    private final String configuration;

    CassandraConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
