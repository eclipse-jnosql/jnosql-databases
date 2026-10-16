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

import java.util.function.Supplier;

/**
 * Configuration options supported by the Weaviate driver.
 *
 * <p>
 * The Weaviate Java client talks to the server through two endpoints: the HTTP (REST) endpoint, used to manage
 * collections and to write objects, and the gRPC endpoint, used to query. Both endpoints are served by the same
 * host, so the driver needs both ports. The default values match a local Weaviate server.
 * </p>
 * <p>
 * Weaviate has no databases: the database defined by {@code jnosql.vector.database} only names the manager,
 * and the collections are shared by the whole Weaviate instance.
 * </p>
 */
public enum WeaviateConfigurations implements Supplier<String> {
    /**
     * The Weaviate host. Defaults to {@code localhost}.
     */
    HOST("jnosql.weaviate.host"),
    /**
     * The Weaviate HTTP port. Defaults to {@code 8080}.
     */
    HTTP_PORT("jnosql.weaviate.http.port"),
    /**
     * The Weaviate gRPC port. Defaults to {@code 50051}.
     */
    GRPC_PORT("jnosql.weaviate.grpc.port"),
    /**
     * Whether the connection uses TLS, which makes the driver connect through {@code https}.
     * Defaults to {@code false}.
     */
    SECURE("jnosql.weaviate.secure"),
    /**
     * The API key used to authenticate against Weaviate. When absent, the connection is anonymous.
     */
    API_KEY("jnosql.weaviate.api.key"),
    /**
     * The timeout, in seconds, applied to the Weaviate calls. When absent, the client default is used.
     */
    TIMEOUT("jnosql.weaviate.timeout"),
    /**
     * The distance used by the vector index when a collection is created:
     * see {@link WeaviateDistance} (case-insensitive).
     * Defaults to {@link WeaviateDistance#COSINE}.
     */
    DISTANCE("jnosql.weaviate.collection.distance");

    private final String configuration;

    WeaviateConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
