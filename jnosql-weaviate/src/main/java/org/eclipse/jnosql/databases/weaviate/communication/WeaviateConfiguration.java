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

import io.weaviate.client6.v1.api.Authentication;
import io.weaviate.client6.v1.api.WeaviateClient;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * The Weaviate implementation of {@link DatabaseConfiguration}. It creates a {@link WeaviateVectorManagerFactory}
 * from {@link Settings}, a configuration map or an existing {@link WeaviateClient}.
 *
 * @see WeaviateConfigurations
 */
public class WeaviateConfiguration implements DatabaseConfiguration {

    /**
     * Creates a {@link WeaviateVectorManagerFactory} from map configurations
     *
     * @param configurations the configurations map
     * @return a WeaviateVectorManagerFactory instance
     * @throws NullPointerException when the configurations is null
     */
    public WeaviateVectorManagerFactory get(Map<String, String> configurations) throws NullPointerException {
        requireNonNull(configurations, "configurations is required");
        SettingsBuilder builder = Settings.builder();
        configurations.forEach(builder::put);
        return apply(builder.build());
    }

    /**
     * Creates a {@link WeaviateVectorManagerFactory} from a {@link WeaviateClient}, using the default collection
     * settings. The factory takes ownership of the client and closes it on
     * {@link WeaviateVectorManagerFactory#close()}.
     *
     * @param client the Weaviate client
     * @return a WeaviateVectorManagerFactory instance
     * @throws NullPointerException when the client is null
     */
    public WeaviateVectorManagerFactory get(WeaviateClient client) throws NullPointerException {
        requireNonNull(client, "client is required");
        return new WeaviateVectorManagerFactory(client, WeaviateCollectionSettings.DEFAULT);
    }

    @Override
    public WeaviateVectorManagerFactory apply(Settings settings) {
        requireNonNull(settings, "settings is required");
        WeaviateCollectionSettings collectionSettings = WeaviateCollectionSettings.from(settings);
        WeaviateConnectionSettings connection = WeaviateConnectionSettings.from(settings);
        WeaviateClient client = WeaviateClient.connectToCustom(custom -> {
            custom.scheme(connection.secure() ? "https" : "http")
                    .httpHost(connection.host())
                    .httpPort(connection.httpPort())
                    .grpcHost(connection.host())
                    .grpcPort(connection.grpcPort());
            connection.apiKeyOptional().ifPresent(key -> custom.authentication(Authentication.apiKey(key)));
            connection.timeoutOptional().ifPresent(custom::timeout);
            return custom;
        });
        return new WeaviateVectorManagerFactory(client, collectionSettings);
    }
}
