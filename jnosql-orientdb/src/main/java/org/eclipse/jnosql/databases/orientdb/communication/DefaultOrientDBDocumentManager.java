/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 *   Lucas Furlaneto
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.orientdb.communication;

import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.fetch.OFetchHelper;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.ID_FIELD;
import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.RID_FIELD;
import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.VERSION_FIELD;
import static org.eclipse.jnosql.databases.orientdb.communication.OrientDBConverter.toMap;

class DefaultOrientDBDocumentManager implements OrientDBDocumentManager {

    private final ODatabasePool pool;

    private final String database;

    DefaultOrientDBDocumentManager(ODatabasePool pool, String database) {
        this.pool = pool;
        this.database = database;
    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        requireNonNull(entity, "Entity is required");
        try (ODatabaseSession tx = pool.acquire()) {
            ODocument document = new ODocument(entity.name());
            toMap(entity).forEach(document::field);
            try {
                tx.save(document);
            } catch (ONeedRetryException e) {
                document = tx.reload(document, OFetchHelper.DEFAULT, false);
                Map<String, Object> entityValues = toMap(entity);
                entityValues.put(OrientDBConverter.VERSION_FIELD, document.getVersion());
                entityValues.forEach(document::field);
                tx.save(document);
            }
            updateEntity(entity, document);
            return entity;
        }
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("There is no support to ttl on OrientDB");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert)
                .collect(toList());
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> insert(e, ttl))
                .collect(toList());
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        requireNonNull(entity, "Entity is required");

        Optional<Element> rid = entity.find(RID_FIELD);
        Optional<Element> id = entity.find(ID_FIELD);
        ORecordId recordId = Stream.concat(rid.stream(), id.stream())
                .map(d -> d.get(String.class))
                .map(ORecordId::new)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("For updates at DocumentEntity"));
        try (ODatabaseSession tx = pool.acquire()) {
            ODocument record = tx.load(recordId);
            entity.remove(RID_FIELD);
            entity.remove(ID_FIELD);
            entity.remove(VERSION_FIELD);
            toMap(entity).forEach(record::field);
            tx.save(record);
            updateEntity(entity, record);
            return entity;
        }


    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update)
                .collect(toList());
    }

    @Override
    public void delete(DeleteQuery query) {
        requireNonNull(query, "query is required");
        var selectQuery = new OrientDBDocumentQuery(query);
        QueryOSQLFactory.QueryResult orientQuery = QueryOSQLFactory.to(selectQuery);

        try (ODatabaseSession tx = pool.acquire()) {

            if (orientQuery.isRunQuery()) {
                try (OResultSet resultSet = tx.command(orientQuery.getQuery(), orientQuery.getParams())) {
                    while (resultSet.hasNext()) {
                        OResult result = resultSet.next();
                        tx.delete(result.toElement().getIdentity());
                    }
                }
            }
            if (orientQuery.isLoad()) {
                orientQuery.getIds().forEach(tx::delete);
            }
        }

    }


    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        requireNonNull(query, "query is required");
        QueryOSQLFactory.QueryResult orientQuery = QueryOSQLFactory.to(query);

        try (ODatabaseSession tx = pool.acquire()) {
            List<CommunicationEntity> entities = new ArrayList<>();
            if (orientQuery.isRunQuery()) {
                try (OResultSet resultSet = tx.command(orientQuery.getQuery(), orientQuery.getParams())) {
                    entities.addAll(OrientDBConverter.convert(resultSet));
                }
            }
            if (orientQuery.isLoad()) {
                orientQuery.getIds().stream().map(tx::load)
                        .map(o -> OrientDBConverter.convert((ODocument) o))
                        .filter(Objects::nonNull)
                        .forEach(entities::add);
            }
            return entities.stream();
        }
    }

    @Override
    public long count(String documentCollection) {
        requireNonNull(documentCollection, "query is required");
        try (ODatabaseSession tx = pool.acquire()) {
            String query = "select count(*) from ".concat(documentCollection);
            OResultSet command = tx.command(query);
            OResult next = command.next();
            Object count = next.getProperty("count(*)");
            return Number.class.cast(count).longValue();

        }
    }

    @Override
    public long count(SelectQuery query) {
        requireNonNull(query, "query is required");
        QueryOSQLFactory.QueryResult orientQuery = QueryOSQLFactory.countTo(query);
        try (ODatabaseSession tx = pool.acquire()) {
            if (orientQuery.isRunQuery()) {
                try (OResultSet resultSet = tx.command(orientQuery.getQuery(), orientQuery.getParams())) {
                    if (resultSet.hasNext()) {
                        OResult next = resultSet.next();
                        Optional<String> propertyRef = next.getPropertyNames().stream().findFirst();
                        if (propertyRef.isPresent()) {
                            Object count = next.getProperty(propertyRef.get());
                            return Number.class.cast(count).longValue();
                        }
                    }
                }
            }
        }
        return 0L;
    }

    @Override
    public Stream<CommunicationEntity> sql(String query, Object... params) {
        requireNonNull(query, "query is required");
        try (ODatabaseSession tx = pool.acquire();
             OResultSet resultSet = tx.command(query, params)) {
            return OrientDBConverter.convert(resultSet).stream();
        }

    }

    @Override
    public Stream<CommunicationEntity> sql(String query, Map<String, Object> params) {
        requireNonNull(query, "query is required");
        requireNonNull(params, "params is required");

        try (ODatabaseSession tx = pool.acquire();
             OResultSet resultSet = tx.command(query, params)) {
            return OrientDBConverter.convert(resultSet).stream();
        }
    }

    @Override
    public void live(SelectQuery query, OrientDBLiveCallback<CommunicationEntity> callbacks) {
        requireNonNull(query, "query is required");
        requireNonNull(callbacks, "callbacks is required");
        ODatabaseSession tx = pool.acquire();
        QueryOSQLFactory.QueryResult queryResult = QueryOSQLFactory.toLive(query, callbacks);
        tx.live(queryResult.getQuery(), new LiveQueryListener(callbacks, tx));
    }

    @Override
    public void live(String query, OrientDBLiveCallback<CommunicationEntity> callbacks, Object... params) {
        requireNonNull(query, "query is required");
        requireNonNull(callbacks, "callbacks is required");
        ODatabaseSession tx = pool.acquire();
        tx.live(query, new LiveQueryListener(callbacks, tx));

    }

    @Override
    public void close() {
        pool.close();
    }

    private void updateEntity(CommunicationEntity entity, ODocument save) {
        ORecordId ridField = new ORecordId(save.getIdentity());
        entity.add(Element.of(RID_FIELD, ridField.toString()));
        entity.add(Element.of(VERSION_FIELD, save.getVersion()));
        entity.add(Element.of(ID_FIELD, ridField.toString()));
    }
}
