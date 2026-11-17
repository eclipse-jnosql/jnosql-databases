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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.arangodb.communication;


import com.arangodb.ArangoDB;
import com.arangodb.entity.LoadBalancingStrategy;
import com.arangodb.serde.ArangoSerde;
import org.eclipse.jnosql.communication.Settings;

import static java.util.Objects.requireNonNull;

/**
 * The base to configuration both key-value and document on ArangoDB.
 * To each configuration set, it will change both builder
 * {@link ArangoDB.Builder}
 */
public abstract class ArangoDBConfiguration {

    /**
     * The underlying ArangoDB builder.
     */
    protected ArangoDB.Builder builder = new ArangoDB.Builder();

    /**
     * Adds an ArangoDB host. Hosts set in the {@link Settings} given to {@code apply} are added to these.
     *
     * @param host the host name
     * @param port the port, usually {@code 8529}
     * @throws NullPointerException when host is null
     */
    public void addHost(String host, int port) throws NullPointerException {
        requireNonNull(host, "host is required");
        builder.host(host, port);
    }

    /**
     * Set the {@link LoadBalancingStrategy}
     *
     * @param loadBalancingStrategy the LoadBalancingStrategy
     * @throws NullPointerException when the loadBalancingStrategy is null
     */
    public void setLoadBalancingStrategy(LoadBalancingStrategy loadBalancingStrategy) throws NullPointerException {
        requireNonNull(loadBalancingStrategy, "loadBalancingStrategy is required");
        builder.loadBalancingStrategy(loadBalancingStrategy);
    }

    /**
     * Sets the request timeout. A {@link ArangoDBConfigurations#TIMEOUT} setting overrides it.
     *
     * @param timeout the timeout in milliseconds
     */
    public void setTimeout(int timeout) {
        builder.timeout(timeout);
    }

    /**
     * Sets the user name. A user in the settings overrides it.
     *
     * @param user the user name
     */
    public void setUser(String user) {
        builder.user(user);
    }

    /**
     * Sets the password. A password in the settings overrides it.
     *
     * @param password the password
     */
    public void setPassword(String password) {
        builder.password(password);
    }

    /**
     * Enables or disables TLS for the connection.
     *
     * @param value {@code true} to use TLS
     */
    public void setUseSSL(boolean value) {
        builder.useSsl(value);
    }

    /**
     * Sets the serializer the ArangoDB driver uses to convert documents to and from the wire format.
     *
     * @param serde the {@link ArangoSerde} to use
     */
    public void setSerde(ArangoSerde serde) {
        builder.serde(serde);
    }

    /**
     * Creates an ArangoDB builder initialized with the provided settings.
     *
     * @param settings the configuration settings
     * @return the initialized ArangoDB builder
     */
    protected ArangoDBBuilder getArangoDBBuilder(Settings settings) {
        ArangoDBBuilder aragonDB = new ArangoDBBuilder(builder);
        ArangoDBBuilders.load(settings, aragonDB);
        return aragonDB;
    }

}
