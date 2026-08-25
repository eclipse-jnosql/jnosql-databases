/*
 *
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;


import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.time.Duration;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class DefaultCouchDBDocumentManager implements CouchDBDocumentManager {

    private final CouchDBHttpClient connector;

    private final String name;

    DefaultCouchDBDocumentManager(CouchDBHttpClient connector, String name) {
        this.connector = connector;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        return connector.insert(entity);
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("CouchDB does not support TTL operation");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert).collect(Collectors.toList());
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> insert(e, ttl)).collect(Collectors.toList());
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        return connector.update(entity);
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update).collect(Collectors.toList());
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");
        connector.delete(query);
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        return connector.select(query);
    }

    @Override
    public long count() {
        return connector.count();
    }

    @Override
    public long count(String documentCollection) {
       throw new UnsupportedOperationException("CouchDB does not have support to count by document Collection," +
               " to total of elments at database use CouchDBDocumentManager#count");
    }

    @Override
    public void close() {
        connector.close();
    }

}
