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

import java.util.function.Supplier;

/**
 * Configuration options supported by the Milvus driver.
 *
 * <p>
 * The Milvus database used by the mapping layer is the one defined by
 * {@code jnosql.vector.database}; it is created when it does not exist yet.
 * </p>
 */
public enum MilvusConfigurations implements Supplier<String> {
    /**
     * The Milvus host. Defaults to {@code localhost}.
     */
    HOST("jnosql.milvus.host"),
    /**
     * The Milvus gRPC port. Defaults to {@code 19530}.
     */
    PORT("jnosql.milvus.port"),
    /**
     * Whether the connection uses TLS, which makes the driver connect through {@code https}.
     * Defaults to {@code false}.
     */
    TLS("jnosql.milvus.tls"),
    /**
     * The full Milvus URI, such as {@code https://my-cluster.example.com:443}.
     * When defined, it takes precedence over {@link #HOST}, {@link #PORT} and {@link #TLS}.
     */
    URI("jnosql.milvus.uri"),
    /**
     * The token used to authenticate against Milvus, either an API key or {@code user:password}.
     */
    TOKEN("jnosql.milvus.token"),
    /**
     * The timeout, in milliseconds, applied to each Milvus call. When absent, the client default is used.
     */
    TIMEOUT("jnosql.milvus.timeout"),
    /**
     * The metric used by the vector index when a collection is created:
     * see {@link MilvusMetric} (case-insensitive).
     * Defaults to {@link MilvusMetric#COSINE}.
     */
    METRIC("jnosql.milvus.collection.metric"),
    /**
     * The vector dimension used when a collection is created.
     * When absent, the dimension of the first inserted vector is used.
     */
    VECTOR_SIZE("jnosql.milvus.collection.vector.size"),
    /**
     * The consistency level applied to the collections and to every read:
     * {@code STRONG}, {@code BOUNDED}, {@code SESSION} or {@code EVENTUALLY} (case-insensitive).
     * Defaults to {@code STRONG}, so a write is visible to the next read.
     */
    CONSISTENCY("jnosql.milvus.consistency"),
    /**
     * The maximum length of the text attributes and of text ids, set when a collection is created.
     * Milvus accepts values from 1 to 65535. Defaults to {@code 65535}.
     */
    MAX_LENGTH("jnosql.milvus.collection.string.max.length");

    private final String configuration;

    MilvusConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
