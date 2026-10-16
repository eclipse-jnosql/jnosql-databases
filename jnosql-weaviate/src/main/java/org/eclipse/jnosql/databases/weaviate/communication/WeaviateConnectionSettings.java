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

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;

import java.util.Optional;

/**
 * The connection settings read from {@link Settings}.
 *
 * @param host           the Weaviate host
 * @param httpPort       the HTTP port
 * @param grpcPort       the gRPC port
 * @param secure         whether the connection uses TLS
 * @param apiKey         the API key, if configured
 * @param timeoutSeconds the timeout in seconds, if configured
 */
record WeaviateConnectionSettings(String host, int httpPort, int grpcPort, boolean secure, String apiKey,
                                  Integer timeoutSeconds) {

    static final String DEFAULT_HOST = "localhost";

    static final int DEFAULT_HTTP_PORT = 8080;

    static final int DEFAULT_GRPC_PORT = 50051;

    static WeaviateConnectionSettings from(Settings settings) {
        String host = settings.get(WeaviateConfigurations.HOST, String.class)
                .or(() -> settings.get(Configurations.HOST, String.class))
                .orElse(DEFAULT_HOST);
        int httpPort = port(settings, WeaviateConfigurations.HTTP_PORT, DEFAULT_HTTP_PORT);
        int grpcPort = port(settings, WeaviateConfigurations.GRPC_PORT, DEFAULT_GRPC_PORT);
        boolean secure = settings.get(WeaviateConfigurations.SECURE, Boolean.class).orElse(false);
        String apiKey = settings.get(WeaviateConfigurations.API_KEY, String.class).orElse(null);
        Integer timeout = settings.get(WeaviateConfigurations.TIMEOUT, Integer.class).orElse(null);
        if (timeout != null && timeout < 1) {
            throw new IllegalArgumentException(WeaviateConfigurations.TIMEOUT.get() + " must be positive");
        }
        return new WeaviateConnectionSettings(host, httpPort, grpcPort, secure, apiKey, timeout);
    }

    private static int port(Settings settings, WeaviateConfigurations configuration, int defaultValue) {
        int port = settings.get(configuration, Integer.class).orElse(defaultValue);
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException(configuration.get() + " must be between 1 and 65535");
        }
        return port;
    }

    Optional<String> apiKeyOptional() {
        return Optional.ofNullable(apiKey);
    }

    Optional<Integer> timeoutOptional() {
        return Optional.ofNullable(timeoutSeconds);
    }
}
