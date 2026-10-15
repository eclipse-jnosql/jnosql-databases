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
package org.eclipse.jnosql.databases.milvus.communication;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.testcontainers.milvus.MilvusContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared Milvus instance for the integration tests, started once per JVM.
 */
public enum VectorDatabase {

    INSTANCE;

    private final MilvusContainer milvus = new MilvusContainer("milvusdb/milvus:v2.6.25")
            .withEnv("DEPLOY_MODE", "STANDALONE");

    {
        milvus.start();
    }

    public MilvusClientV2 client() {
        return new MilvusClientV2(ConnectConfig.builder().uri(milvus.getEndpoint()).build());
    }

    public Settings settings() {
        Map<String, Object> map = new HashMap<>();
        map.put(MilvusConfigurations.HOST.get(), milvus.getHost());
        map.put(MilvusConfigurations.PORT.get(), milvus.getMappedPort(19530));
        map.put(MilvusConfigurations.TIMEOUT.get(), 60_000L);
        return Settings.of(map);
    }

    public VectorManager get(String database) {
        return new MilvusConfiguration().apply(settings()).apply(database);
    }

    public VectorManager get(String database, MilvusMetric metric) {
        Settings settings = Settings.builder()
                .putAll(settings().toMap())
                .put(MilvusConfigurations.METRIC, metric.name())
                .build();
        return new MilvusConfiguration().apply(settings).apply(database);
    }
}
