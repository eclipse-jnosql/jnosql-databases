/*
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
 */

package org.eclipse.jnosql.databases.ravendb.communication;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.queries.Query;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.IEnumerableQuery;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.exceptions.RavenException;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static net.ravendb.client.Constants.Documents.Metadata.COLLECTION;
import static net.ravendb.client.Constants.Documents.Metadata.EXPIRES;

/**
 * The RavenDB implementation to {@link DatabaseManager} that does not support TTL methods
 * {@link RavenDBDocumentManager#insert(CommunicationEntity, Duration)}
 */
public class RavenDBDocumentManager implements DatabaseManager {


    private final DocumentStore store;

    private final String database;


    RavenDBDocumentManager(DocumentStore store, String database) {
        this.store = store;
        this.store.initialize();
        this.database = database;

    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        try (IDocumentSession session = store.openSession()) {
            insert(entity, null, session);
        }
        return entity;
    }


    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(ttl, "ttl is required");

        LocalDateTime time = LocalDateTime.now(Clock.systemUTC()).plus(ttl);

        try (IDocumentSession session = store.openSession()) {
            insert(entity, time, session);
        }
        return entity;
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> insert(e, ttl))
                .collect(Collectors.toList());
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");

        try (IDocumentSession session = store.openSession()) {
            Element id = entity.find(EntityConverter.ID_FIELD)
                    .orElseThrow(() -> new RavenException("Id is required to Raven Update operation"));

            HashMap<String, Object> map = session.load(HashMap.class, id.get(String.class));
            map.putAll(EntityConverter.getMap(entity));
            session.saveChanges();
        }
        return entity;
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");

        try (IDocumentSession session = store.openSession()) {
            Stream<Map> entities = getQueryMaps(new RavenDeleteQuery(query), session);
            entities.map(EntityConverter::getId).forEach(session::delete);
            session.saveChanges();
        }

    }


    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");

        try (IDocumentSession session = store.openSession()) {
            Stream<Map> entities = getQueryMaps(query, session);
            return entities.filter(Objects::nonNull).map(EntityConverter::getEntity);
        }

    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        try (IDocumentSession session = store.openSession()) {
            IDocumentQuery<HashMap> ravenQuery = session.query(HashMap.class, Query.collection(documentCollection));
            return ravenQuery.count();
        }
    }

    @Override
    public void close() {
        store.close();
    }


    private void insert(CommunicationEntity entity, LocalDateTime time, IDocumentSession session) {
        String collection = entity.name();

        Map<String, Object> entityMap = EntityConverter.getMap(entity);
        String id = entity.find(EntityConverter.ID_FIELD)
                .map(d -> d.get(String.class))
                .orElse(collection + '/');
        session.store(entityMap, id);
        IMetadataDictionary metadata = session.advanced().getMetadataFor(entityMap);
        metadata.put(COLLECTION, collection);

        if(Objects.nonNull(time)) {
            metadata.put(EXPIRES, time.toString());
        }
        session.saveChanges();
        entity.add(EntityConverter.ID_FIELD, session.advanced().getDocumentId(entityMap));
    }


    private Stream<Map> getQueryMaps(SelectQuery query, IDocumentSession session) {
        DocumentQueryConverter.QueryResult queryResult = DocumentQueryConverter.createQuery(session, query);

        Stream<Map> idQueryStream = queryResult.getIds().stream()
                .map(i -> session.load(HashMap.class, i));

        final List<HashMap> hashMaps = queryResult.getRavenQuery().map(IEnumerableQuery::toList)
                .orElse(Collections.emptyList());
        final Stream<HashMap> queryStream = queryResult.getRavenQuery()
                .map(IEnumerableQuery::toList)
                .map(List::stream).orElseGet(Stream::empty);
        return Stream.concat(idQueryStream, queryStream);
    }

}
