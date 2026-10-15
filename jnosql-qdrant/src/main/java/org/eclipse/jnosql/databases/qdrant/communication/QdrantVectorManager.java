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
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

/**
 * The Qdrant implementation of {@link VectorManager}.
 * The {@link QdrantClient} is owned by the {@link QdrantVectorManagerFactory};
 * closing a {@link QdrantVectorManager} has no effect.
 * Database operations are not supported yet.
 */
public class QdrantVectorManager implements VectorManager {

    private final QdrantClient client;

    private final String database;

    private final QdrantCollectionSettings collectionSettings;

    QdrantVectorManager(QdrantClient client, String database, QdrantCollectionSettings collectionSettings) {
        this.client = client;
        this.database = database;
        this.collectionSettings = collectionSettings;
    }

    @Override
    public String name() {
        return database;
    }

    QdrantCollectionSettings collectionSettings() {
        return collectionSettings;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public void delete(DeleteQuery query) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public long count(String entity) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public List<CommunicationEntity> search(VectorSelectQuery query) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public void close() {
        // the client is owned and closed by the factory
    }

    @Override
    public String toString() {
        return "QdrantVectorManager{" + "database='" + database + '\'' + '}';
    }
}
