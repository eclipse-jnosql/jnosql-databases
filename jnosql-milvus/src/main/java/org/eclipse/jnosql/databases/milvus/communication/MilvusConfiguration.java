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
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * The Milvus implementation of {@link DatabaseConfiguration}. It creates a {@link MilvusVectorManagerFactory}
 * from {@link Settings}, a configuration map or an existing {@link MilvusClientV2}.
 *
 * @see MilvusConfigurations
 */
public class MilvusConfiguration implements DatabaseConfiguration {

    /**
     * Creates a {@link MilvusVectorManagerFactory} from map configurations
     *
     * @param configurations the configurations map
     * @return a MilvusVectorManagerFactory instance
     * @throws NullPointerException when the configurations is null
     */
    public MilvusVectorManagerFactory get(Map<String, String> configurations) throws NullPointerException {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    /**
     * Creates a {@link MilvusVectorManagerFactory} from a {@link MilvusClientV2}, using the default collection
     * settings. The factory takes ownership of the client and closes it on
     * {@link MilvusVectorManagerFactory#close()}.
     *
     * @param client the Milvus client
     * @return a MilvusVectorManagerFactory instance
     * @throws NullPointerException when the client is null
     */
    public MilvusVectorManagerFactory get(MilvusClientV2 client) throws NullPointerException {
        requireNonNull(client, "client is required");
        return new MilvusVectorManagerFactory(client, MilvusCollectionSettings.DEFAULT);
    }

    @Override
    public MilvusVectorManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        MilvusCollectionSettings collectionSettings = MilvusCollectionSettings.from(settings);
        MilvusConnectionSettings connection = MilvusConnectionSettings.from(settings);

        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder().uri(connection.uri());
        connection.tokenOptional().ifPresent(builder::token);
        connection.timeoutOptional().ifPresent(timeout -> builder.rpcDeadlineMs(timeout.toMillis())
                .connectTimeoutMs(timeout.toMillis()));
        return new MilvusVectorManagerFactory(new MilvusClientV2(builder.build()), collectionSettings);
    }
}
