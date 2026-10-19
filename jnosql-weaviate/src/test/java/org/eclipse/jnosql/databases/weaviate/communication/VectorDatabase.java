/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.weaviate.communication;

import io.weaviate.client6.v1.api.WeaviateClient;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.weaviate.WeaviateContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Weaviate instance for the integration tests, started once per JVM.
 */
public enum VectorDatabase {

    INSTANCE;

    private final WeaviateContainer weaviate = new WeaviateContainer(
            DockerImageName.parse("semitechnologies/weaviate:1.38.21"));

    {
        weaviate.start();
    }

    public WeaviateClient client() {
        return WeaviateClient.connectToCustom(custom -> custom.scheme("http")
                .httpHost(weaviate.getHost()).httpPort(weaviate.getHttpPort())
                .grpcHost(weaviate.getHost()).grpcPort(weaviate.getGrpcPort()));
    }

    public Settings settings() {
        Map<String, Object> map = new HashMap<>();
        map.put(WeaviateConfigurations.HOST.get(), weaviate.getHost());
        map.put(WeaviateConfigurations.HTTP_PORT.get(), weaviate.getHttpPort());
        map.put(WeaviateConfigurations.GRPC_PORT.get(), weaviate.getGrpcPort());
        map.put(WeaviateConfigurations.TIMEOUT.get(), 60);
        return Settings.of(map);
    }

    public VectorManager get(String database) {
        return new WeaviateConfiguration().apply(settings()).apply(database);
    }

    public VectorManager get(String database, WeaviateDistance distance) {
        Settings settings = Settings.builder()
                .putAll(settings().toMap())
                .put(WeaviateConfigurations.DISTANCE, distance.name())
                .build();
        return new WeaviateConfiguration().apply(settings).apply(database);
    }
}
