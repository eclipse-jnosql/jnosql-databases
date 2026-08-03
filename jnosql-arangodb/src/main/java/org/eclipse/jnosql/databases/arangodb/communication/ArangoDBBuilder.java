/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoDB;
import com.arangodb.Protocol;
import com.arangodb.entity.LoadBalancingStrategy;

/**
 * Wrapper around {@link ArangoDB.Builder} used by JNoSQL configuration.
 */
public class ArangoDBBuilder {

    private final ArangoDB.Builder arangoDB;

    ArangoDBBuilder(ArangoDB.Builder arangoDB) {
        this.arangoDB = arangoDB;
    }

    /**
     * Adds a host to the ArangoDB builder.
     *
     * @param host the host name
     * @param port the host port
     */
    public void host(String host, int port) {
        arangoDB.host(host, port);
    }

    /**
     * Sets the request timeout.
     *
     * @param timeout the timeout in milliseconds
     */
    public void timeout(int timeout) {
        arangoDB.timeout(timeout);
    }

    /**
     * Sets the user name.
     *
     * @param user the user name
     */
    public void user(String user) {
        arangoDB.user(user);
    }

    /**
     * Sets the password.
     *
     * @param password the password
     */
    public void password(String password) {
        arangoDB.password(password);
    }

    /**
     * Enables or disables SSL.
     *
     * @param useSsl whether SSL should be used
     */
    public void useSsl(boolean useSsl) {
        arangoDB.useSsl(useSsl);
    }

    /**
     * Sets the chunk size.
     *
     * @param chunkSize the chunk size
     */
    public void chunkSize(int chunkSize) {
        arangoDB.chunkSize(chunkSize);
    }

    /**
     * Sets the maximum number of connections.
     *
     * @param maxConnections the maximum number of connections
     */
    public void maxConnections(int maxConnections) {
        arangoDB.maxConnections(maxConnections);
    }

    /**
     * Sets the communication protocol.
     *
     * @param protocol the communication protocol
     */
    public void protocol(Protocol protocol) {
        arangoDB.protocol(protocol);
    }

    /**
     * Enables or disables acquiring the host list.
     *
     * @param acquireHostList whether the host list should be acquired
     */
    public void acquireHostList(boolean acquireHostList) {
        arangoDB.acquireHostList(acquireHostList);
    }

    /**
     * Sets the load balancing strategy.
     *
     * @param loadBalancingStrategy the load balancing strategy
     */
    public void loadBalancingStrategy(LoadBalancingStrategy loadBalancingStrategy) {
        arangoDB.loadBalancingStrategy(loadBalancingStrategy);
    }

    /**
     * Builds the ArangoDB driver instance.
     *
     * @return the configured ArangoDB driver
     */
    public ArangoDB build() {
        return arangoDB.build();
    }
}
