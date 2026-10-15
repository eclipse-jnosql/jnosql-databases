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
package org.eclipse.jnosql.databases.qdrant.communication;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.testcontainers.qdrant.QdrantContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Qdrant instances for the integration tests, started once per JVM.
 */
public enum VectorDatabase {

    INSTANCE;

    public static final String API_KEY = "test-key";

    private final QdrantContainer qdrant = new QdrantContainer("qdrant/qdrant:latest");

    private final QdrantContainer secured = new QdrantContainer("qdrant/qdrant:latest").withApiKey(API_KEY);

    {
        qdrant.start();
        secured.start();
    }

    public QdrantClient client() {
        return new QdrantClient(QdrantGrpcClient.newBuilder(qdrant.getHost(), qdrant.getGrpcPort(), false).build());
    }

    public Settings settings() {
        return settings(qdrant, null);
    }

    /** Settings of an instance that requires {@link #API_KEY}. */
    public Settings securedSettings(boolean withApiKey) {
        return settings(secured, withApiKey ? API_KEY : null);
    }

    public VectorManager get(String database) {
        return new QdrantConfiguration().apply(settings(qdrant, null)).apply(database);
    }

    public VectorManager get(String database, QdrantDistance distance) {
        Settings settings = Settings.builder()
                .putAll(settings().toMap())
                .put(QdrantConfigurations.DISTANCE, distance.name())
                .build();
        return new QdrantConfiguration().apply(settings).apply(database);
    }

    private static Settings settings(QdrantContainer container, String apiKey) {
        Map<String, Object> map = new HashMap<>();
        map.put(QdrantConfigurations.HOST.get(), container.getHost());
        map.put(QdrantConfigurations.PORT.get(), container.getGrpcPort());
        map.put(QdrantConfigurations.TIMEOUT.get(), 10_000L);
        if (apiKey != null) {
            map.put(QdrantConfigurations.API_KEY.get(), apiKey);
        }
        return Settings.of(map);
    }
}
