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
package org.eclipse.jnosql.databases.hazelcast.communication;

import java.util.function.Supplier;

/**
 * An enumeration to show the available options to connect to the Hazelcast database.
 * It implements {@link Supplier}, where its it returns the property name that might be
 * overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum HazelcastConfigurations implements Supplier<String> {

    /**
     * The instance name uniquely identifying the hazelcast instance created by this configuration.
     * This name is used in different scenarios, such as identifying the hazelcast
     * instance when running multiple instances in the same JVM.
     */
    INSTANCE("jnosql.hazelcast.instance.name"),
    /**
     * Database's host. It is a prefix to enumerate hosts. E.g.: jnosql.hazelcast.host.1=localhost
     */
    HOST("jnosql.hazelcast.host"),
    /**
     * The database port
     */
    PORT("jnosql.hazelcast.port"),
    /**
     * The maximum number of ports allowed to use.
     */
    PORT_COUNT("jnosql.hazelcast.port.count"),
    /**
     * Sets if a Hazelcast member is allowed to find a free port by incrementing the port number when it encounters
     * an occupied port.
     */
    PORT_AUTO_INCREMENT("jnosql.hazelcast.port.auto.increment"),
    /**
     * Enables or disables the multicast discovery mechanism
     */
    MULTICAST_ENABLE("jnosql.hazelcast.multicast.enable"),
    /**
     * Enables or disables the Tcp/Ip join mechanism.
     */
    TCP_IP_JOIN("jnosql.hazelcast.tcp.ip.join");

    private final String configuration;

    HazelcastConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
