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
 */
package org.eclipse.jnosql.databases.couchbase.communication;


import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.InsertOptions;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * The default implementation of {@link CouchbaseDocumentManager}.
 * <br/>
 * Closing a {@link DefaultCouchbaseDocumentManager} has no effect.
 */
class DefaultCouchbaseDocumentManager implements CouchbaseDocumentManager {

    private static final Logger LOGGER = Logger.getLogger(DefaultCouchbaseDocumentManager.class.getName());

    private final Bucket bucket;
    private final String database;

    private final Cluster cluster;

    DefaultCouchbaseDocumentManager(Cluster cluster, String database) {
        this.bucket = cluster.bucket(database);
        this.database = database;
        this.cluster = cluster;
    }

    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) throws NullPointerException {
        requireNonNull(entity, "entity is required");
        return waitBucketBeReadyAndGet(() -> {
            entity.add(EntityConverter.COLLECTION_FIELD, entity.name());
            JsonObject json = EntityConverter.convert(entity);
            Element id = entity.find(EntityConverter.ID_FIELD)
                    .orElseThrow(() -> new CouchbaseNoKeyFoundException(entity.toString()));
            Collection collection = bucket.collection(entity.name());
            collection.insert(id.get(String.class), json);
            return entity;
        });
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        requireNonNull(entity, "entity is required");
        requireNonNull(ttl, "ttl is required");
        return waitBucketBeReadyAndGet(() -> {
            JsonObject json = EntityConverter.convert(entity);
            Element id = entity.find(EntityConverter.ID_FIELD)
                    .orElseThrow(() -> new CouchbaseNoKeyFoundException(entity.toString()));
            Collection collection = bucket.collection(entity.name());
            collection.insert(id.get(String.class), json, InsertOptions.insertOptions().expiry(ttl));
            return entity;
        });
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
        requireNonNull(entity, "entity is required");
        return waitBucketBeReadyAndGet(() -> {
            entity.add(EntityConverter.COLLECTION_FIELD, entity.name());
            JsonObject json = EntityConverter.convert(entity);
            Element id = entity.find(EntityConverter.ID_FIELD)
                    .orElseThrow(() -> new CouchbaseNoKeyFoundException(entity.toString()));
            Collection collection = bucket.collection(entity.name());
            collection.upsert(id.get(String.class), json);
            return entity;
        });
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update).collect(Collectors.toList());
    }

    @Override
    public Iterable<CommunicationEntity> update(UpdateQuery query) {
        Objects.requireNonNull(query, "query is required");
        return waitBucketBeReadyAndGet(() -> {
            N1QLQuery n1QLQuery = N1QLBuilder.of(query, database, bucket.defaultScope().name()).get();
            QueryResult result = cluster.query(n1QLQuery.query(), QueryOptions
                    .queryOptions().parameters(n1QLQuery.params()));
            List<JsonObject> jsons = result.rowsAsObject();
            return EntityConverter.convert(jsons, database).toList();
        });
    }

    @Override
    public void delete(DeleteQuery query) {
        waitBucketBeReadyAndDo(() -> {
            Objects.requireNonNull(query, "query is required");
            Collection collection = bucket.collection(query.name());
            SelectQuery delete = DeleteQueryWrapper.of(query);
            Stream<CommunicationEntity> entities = select(delete);
            entities.flatMap(d -> d.find(EntityConverter.ID_FIELD).stream())
                    .filter(Objects::nonNull)
                    .map(d -> d.get(String.class))
                    .forEach(collection::remove);
        });
    }

    private void waitBucketBeReadyAndDo(Runnable runnable) {
        bucket.waitUntilReady(bucket.environment().timeoutConfig().kvDurableTimeout());
        runnable.run();
    }


    private <T> T waitBucketBeReadyAndGet(Supplier<T> supplier) {
        bucket.waitUntilReady(bucket.environment().timeoutConfig().kvDurableTimeout());
        return supplier.get();
    }

    @Override
    public Stream<CommunicationEntity> select(final SelectQuery query) throws NullPointerException {
        Objects.requireNonNull(query, "query is required");
        return waitBucketBeReadyAndGet(() -> {
            N1QLQuery n1QLQuery = N1QLBuilder.of(query, database, bucket.defaultScope().name()).get();
            List<JsonObject> jsons = new ArrayList<>();
            if (n1QLQuery.hasIds()) {
                Collection collection = bucket.collection(query.name());
                for (String id : n1QLQuery.ids()) {
                    try {
                        GetResult result = collection.get(id);
                        jsons.add(result.contentAsObject());
                    } catch (DocumentNotFoundException exp) {
                        LOGGER.log(Level.FINEST, "The id was not found: " + id);
                    }
                }
            }

            if (!n1QLQuery.hasOnlyIds()) {
                QueryResult result;
                if (n1QLQuery.isParameterEmpty()) {
                    result = cluster.query(n1QLQuery.query());
                } else {
                    result = cluster.query(n1QLQuery.query(), QueryOptions
                            .queryOptions().parameters(n1QLQuery.params()));
                }
                jsons.addAll(result.rowsAsObject());
            }
            return EntityConverter.convert(jsons, database);
        });
    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        return waitBucketBeReadyAndGet(() -> {
            SelectQuery countQuery = SelectQuery
                    .select("COUNT(*)").from(documentCollection).build();
            N1QLQuery n1QLQuery = N1QLBuilder
                    .of(countQuery, database, bucket.defaultScope().name()).get();
            QueryResult query = cluster.query(n1QLQuery.query());
            List<JsonObject> result = query.rowsAsObject();
            var count = result.stream().findFirst()
                    .map(data -> data.getNumber("$1"))
                    .orElse(0L);
            return count.longValue();
        });
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        return waitBucketBeReadyAndGet(() -> {
            N1QLQuery n1QLQuery = N1QLBuilder.countOf(query, database, bucket.defaultScope().name()).get();
            QueryResult result;
            if (n1QLQuery.isParameterEmpty()) {
                result = cluster.query(n1QLQuery.query());
            } else {
                result = cluster.query(n1QLQuery.query(), QueryOptions
                        .queryOptions().parameters(n1QLQuery.params()));
            }
            return result.rowsAsObject()
                    .stream()
                    .findFirst()
                    .map(json -> json.getLong("$1"))
                    .orElse(0L);
        });
    }

    @Override
    public Stream<CommunicationEntity> n1qlQuery(final String n1ql, final JsonObject params) throws NullPointerException {
        requireNonNull(n1ql, "n1qlQuery is required");
        requireNonNull(params, "params is required");
        return waitBucketBeReadyAndGet(() -> {
            QueryResult query = cluster.query(n1ql, QueryOptions
                    .queryOptions().parameters(params));
            return EntityConverter.convert(query.rowsAsObject(), database);
        });
    }


    @Override
    public Stream<CommunicationEntity> n1qlQuery(String n1ql) throws NullPointerException {
        requireNonNull(n1ql, "n1qlQuery is required");
        return waitBucketBeReadyAndGet(() -> {
            QueryResult query = cluster.query(n1ql);
            return EntityConverter.convert(query.rowsAsObject(), database);
        });
    }


    /**
     * Closing a {@link DefaultCouchbaseDocumentManager} has no effect.
     */
    @Override
    public void close() {
    }
}
