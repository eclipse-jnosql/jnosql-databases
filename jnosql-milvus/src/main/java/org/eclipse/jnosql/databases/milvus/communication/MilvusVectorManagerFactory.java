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

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.database.request.CreateDatabaseReq;
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.eclipse.jnosql.mapping.vector.VectorManager;

import java.util.Objects;

/**
 * The Milvus {@link DatabaseManagerFactory}. It owns the {@link MilvusClientV2}
 * and closes it when the factory is closed.
 * The Milvus database of each {@link VectorManager} is created when it does not exist yet.
 */
public class MilvusVectorManagerFactory implements DatabaseManagerFactory {

    private final MilvusClientV2 client;

    private final MilvusCollectionSettings collectionSettings;

    MilvusVectorManagerFactory(MilvusClientV2 client, MilvusCollectionSettings collectionSettings) {
        this.client = client;
        this.collectionSettings = collectionSettings;
    }

    @Override
    public VectorManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        if (!client.listDatabases().getDatabaseNames().contains(database)) {
            client.createDatabase(CreateDatabaseReq.builder().databaseName(database).build());
        }
        return new MilvusVectorManager(client, database, collectionSettings);
    }

    MilvusClientV2 client() {
        return client;
    }

    MilvusCollectionSettings collectionSettings() {
        return collectionSettings;
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public String toString() {
        return "MilvusVectorManagerFactory{" + "client=" + client + '}';
    }
}
