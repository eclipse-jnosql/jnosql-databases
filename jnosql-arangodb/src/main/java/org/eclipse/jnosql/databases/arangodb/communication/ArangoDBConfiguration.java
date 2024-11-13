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

    protected ArangoDB.Builder builder = new ArangoDB.Builder()
            .serde(new JsonbSerde());

    /**
     * Adds a host in the arangodb builder
     *
     * @param host the host
     * @param port the port
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
     * set the setTimeout
     *
     * @param timeout the setTimeout
     */
    public void setTimeout(int timeout) {
        builder.timeout(timeout);
    }

    /**
     * set the setUser
     *
     * @param user the setUser
     */
    public void setUser(String user) {
        builder.user(user);
    }

    /**
     * set the setPassword
     *
     * @param password the setPassword
     */
    public void setPassword(String password) {
        builder.password(password);
    }

    /**
     * set if going to use ssl
     *
     * @param value the ssl
     */
    public void setUseSSL(boolean value) {
        builder.useSsl(value);
    }

    /**
     * Set the ArangoDB serde for the user data. Note that the provided
     * serde must support serializing and deserializing JsonP types,
     * i.e. {@link jakarta.json.JsonValue} and its children.
     * By default, the builder is configured to use {@link JsonbSerde};
     * this setter allows overriding it, i.e. providing an instance of
     * {@link JsonbSerde} that uses a specific {@link jakarta.json.bind.Jsonb}
     * instance.
     *
     * @param serde the serde
     */
    public void setSerde(ArangoSerde serde) {
        builder.serde(serde);
    }

    protected ArangoDBBuilder getArangoDBBuilder(Settings settings) {
        ArangoDBBuilder aragonDB = new ArangoDBBuilder(builder);
        ArangoDBBuilders.load(settings, aragonDB);
        return aragonDB;
    }

}
