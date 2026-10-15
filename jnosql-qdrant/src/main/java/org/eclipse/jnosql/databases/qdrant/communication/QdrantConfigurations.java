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

import java.util.function.Supplier;

/**
 * Configuration options supported by the Qdrant driver.
 */
public enum QdrantConfigurations implements Supplier<String> {
    /**
     * The Qdrant host. Defaults to {@code localhost}.
     */
    HOST("jnosql.qdrant.host"),
    /**
     * The Qdrant gRPC port. Defaults to {@code 6334}.
     */
    PORT("jnosql.qdrant.port"),
    /**
     * Whether the connection uses TLS. Defaults to {@code false}.
     */
    TLS("jnosql.qdrant.tls"),
    /**
     * The API key used to authenticate against Qdrant.
     */
    API_KEY("jnosql.qdrant.api.key"),
    /**
     * The timeout, in milliseconds, applied to each Qdrant call. When absent, the client default is used.
     */
    TIMEOUT("jnosql.qdrant.timeout"),
    /**
     * The distance metric used when a collection is created:
     * see {@link QdrantDistance} (case-insensitive).
     * Qdrant defines no default for it.
     */
    DISTANCE("jnosql.qdrant.collection.distance"),
    /**
     * The vector dimension used when a collection is created. Qdrant defines no default for it.
     */
    VECTOR_SIZE("jnosql.qdrant.collection.vector.size");

    private final String configuration;

    QdrantConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
