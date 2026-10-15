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
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.time.Duration;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * The Qdrant implementation of {@link DatabaseConfiguration}. It creates a {@link QdrantVectorManagerFactory}
 * from {@link Settings}, a configuration map or an existing {@link QdrantClient}.
 */
public class QdrantConfiguration implements DatabaseConfiguration {

    static final String DEFAULT_HOST = "localhost";

    /**
     * Default Qdrant gRPC port.
     */
    static final int DEFAULT_PORT = 6334;

    /**
     * Creates a {@link QdrantVectorManagerFactory} from map configurations
     *
     * @param configurations the configurations map
     * @return a QdrantVectorManagerFactory instance
     * @throws NullPointerException when the configurations is null
     */
    public QdrantVectorManagerFactory get(Map<String, String> configurations) throws NullPointerException {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    /**
     * Creates a {@link QdrantVectorManagerFactory} from a {@link QdrantClient}.
     * The factory takes ownership of the client and closes it on {@link QdrantVectorManagerFactory#close()}.
     *
     * @param client the Qdrant client
     * @return a QdrantVectorManagerFactory instance
     * @throws NullPointerException when the client is null
     */
    public QdrantVectorManagerFactory get(QdrantClient client) throws NullPointerException {
        requireNonNull(client, "client is required");
        return new QdrantVectorManagerFactory(client, QdrantCollectionSettings.EMPTY);
    }

    @Override
    public QdrantVectorManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");

        String host = settings.get(QdrantConfigurations.HOST, String.class)
                .or(() -> settings.get(Configurations.HOST, String.class))
                .orElse(DEFAULT_HOST);
        int port = settings.get(QdrantConfigurations.PORT, Integer.class).orElse(DEFAULT_PORT);
        boolean tls = settings.get(QdrantConfigurations.TLS, Boolean.class).orElse(false);

        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, tls);
        settings.get(QdrantConfigurations.API_KEY, String.class).ifPresent(builder::withApiKey);
        settings.get(QdrantConfigurations.TIMEOUT, Long.class)
                .map(Duration::ofMillis)
                .ifPresent(builder::withTimeout);

        return new QdrantVectorManagerFactory(new QdrantClient(builder.build()), collectionSettings(settings));
    }

    private QdrantCollectionSettings collectionSettings(Settings settings) {
        QdrantDistance distance = settings.get(QdrantConfigurations.DISTANCE, String.class)
                .map(QdrantDistance::parse)
                .orElse(null);
        Long vectorSize = settings.get(QdrantConfigurations.VECTOR_SIZE, Long.class).orElse(null);
        return new QdrantCollectionSettings(distance, vectorSize);
    }
}
