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
package org.eclipse.jnosql.databases.scylladb.communication;

import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the ScyllaDB database.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum ScyllaDBConfigurations implements Supplier<String> {

    /**
     * The user's credential.
     */
    USER("jnosql.scylladb.user"),

    /**
     * The password's credential
     */
    PASSWORD("jnosql.scylladb.password"),
    /**
     * Database's host. It is a prefix to enumerate hosts. E.g.: jnosql.scylladb.host.1=localhost
     */
    HOST("jnosql.scylladb.host"),
    /**
     * The name of the application using the created session.
     */
    NAME("jnosql.scylladb.name"),
    /**
     * The scylladb's port
     */
    PORT("jnosql.scylladb.port"),
    /**
     * The ScyllaDB CQL to execute when the configuration starts. It uses as a prefix. E.g.: jnosql.scylladb.query.1=CQL
     */
    QUERY("jnosql.scylladb.query"),
    /**
     * The datacenter that is considered "local" by the load balancing policy.
     */
    DATA_CENTER("jnosql.scylladb.data.center");

    private final String configuration;

    ScyllaDBConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
