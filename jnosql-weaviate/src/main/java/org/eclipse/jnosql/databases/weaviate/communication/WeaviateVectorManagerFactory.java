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

import io.weaviate.client6.v1.api.WeaviateClient;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.eclipse.jnosql.mapping.vector.VectorManager;

import java.util.Objects;

/**
 * The Weaviate {@link DatabaseManagerFactory}. It owns the {@link WeaviateClient}
 * and closes it when the factory is closed.
 * Weaviate has no databases, so every {@link VectorManager} shares the collections of the Weaviate instance.
 */
public class WeaviateVectorManagerFactory implements DatabaseManagerFactory {

    private final WeaviateClient client;

    private final WeaviateCollectionSettings collectionSettings;

    WeaviateVectorManagerFactory(WeaviateClient client, WeaviateCollectionSettings collectionSettings) {
        this.client = client;
        this.collectionSettings = collectionSettings;
    }

    @Override
    public VectorManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        return new WeaviateVectorManager(client, database, collectionSettings);
    }

    WeaviateClient client() {
        return client;
    }

    WeaviateCollectionSettings collectionSettings() {
        return collectionSettings;
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (Exception exception) {
            throw new IllegalStateException("It cannot close the Weaviate client", exception);
        }
    }

    @Override
    public String toString() {
        return "WeaviateVectorManagerFactory{" + "client=" + client + '}';
    }
}
