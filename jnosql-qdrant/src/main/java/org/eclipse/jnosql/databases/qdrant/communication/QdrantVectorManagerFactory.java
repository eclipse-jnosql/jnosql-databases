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
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;

import java.util.Objects;

/**
 * The Qdrant {@link DatabaseManagerFactory}. It owns the {@link QdrantClient}
 * and closes it when the factory is closed.
 */
public class QdrantVectorManagerFactory implements DatabaseManagerFactory {

    private final QdrantClient client;

    private final QdrantCollectionSettings collectionSettings;

    QdrantVectorManagerFactory(QdrantClient client, QdrantCollectionSettings collectionSettings) {
        this.client = client;
        this.collectionSettings = collectionSettings;
    }

    @Override
    public QdrantVectorManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        return new QdrantVectorManager(client, database, collectionSettings);
    }

    QdrantClient client() {
        return client;
    }

    QdrantCollectionSettings collectionSettings() {
        return collectionSettings;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public String toString() {
        return "QdrantVectorManagerFactory{" + "client=" + client + '}';
    }
}
