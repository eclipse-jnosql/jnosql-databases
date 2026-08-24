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
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoDB;
import com.arangodb.Protocol;
import com.arangodb.entity.LoadBalancingStrategy;

public class ArangoDBBuilder {

    private final ArangoDB.Builder arangoDB;

    ArangoDBBuilder(ArangoDB.Builder arangoDB) {
        this.arangoDB = arangoDB;
    }

    public void host(String host, int port) {
        arangoDB.host(host, port);
    }

    public void timeout(int timeout) {
        arangoDB.timeout(timeout);
    }

    public void user(String user) {
        arangoDB.user(user);
    }

    public void password(String password) {
        arangoDB.password(password);
    }

    public void useSsl(boolean useSsl) {
        arangoDB.useSsl(useSsl);
    }

    public void chunkSize(int chunkSize) {
        arangoDB.chunkSize(chunkSize);
    }

    public void maxConnections(int maxConnections) {
        arangoDB.maxConnections(maxConnections);
    }

    public void protocol(Protocol protocol) {
        arangoDB.protocol(protocol);
    }

    public void acquireHostList(boolean acquireHostList) {
        arangoDB.acquireHostList(acquireHostList);
    }

    public void loadBalancingStrategy(LoadBalancingStrategy loadBalancingStrategy) {
        arangoDB.loadBalancingStrategy(loadBalancingStrategy);
    }

    public ArangoDB build() {
        return arangoDB.build();
    }
}
