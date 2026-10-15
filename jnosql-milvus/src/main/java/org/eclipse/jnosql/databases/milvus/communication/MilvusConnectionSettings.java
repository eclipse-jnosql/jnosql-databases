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

import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;

import java.time.Duration;
import java.util.Optional;

/**
 * The connection settings read from {@link Settings}.
 *
 * @param uri     the Milvus URI
 * @param token   the authentication token, if configured
 * @param timeout the timeout applied to each call, if configured
 */
record MilvusConnectionSettings(String uri, String token, Duration timeout) {

    static final String DEFAULT_HOST = "localhost";

    static final int DEFAULT_PORT = 19530;

    static MilvusConnectionSettings from(Settings settings) {
        String uri = settings.get(MilvusConfigurations.URI, String.class).orElseGet(() -> {
            String host = settings.get(MilvusConfigurations.HOST, String.class)
                    .or(() -> settings.get(Configurations.HOST, String.class))
                    .orElse(DEFAULT_HOST);
            int port = settings.get(MilvusConfigurations.PORT, Integer.class).orElse(DEFAULT_PORT);
            boolean tls = settings.get(MilvusConfigurations.TLS, Boolean.class).orElse(false);
            return (tls ? "https://" : "http://") + host + ":" + port;
        });
        String token = settings.get(MilvusConfigurations.TOKEN, String.class).orElse(null);
        Duration timeout = settings.get(MilvusConfigurations.TIMEOUT, Long.class)
                .map(Duration::ofMillis).orElse(null);
        return new MilvusConnectionSettings(uri, token, timeout);
    }

    Optional<String> tokenOptional() {
        return Optional.ofNullable(token);
    }

    Optional<Duration> timeoutOptional() {
        return Optional.ofNullable(timeout);
    }
}
