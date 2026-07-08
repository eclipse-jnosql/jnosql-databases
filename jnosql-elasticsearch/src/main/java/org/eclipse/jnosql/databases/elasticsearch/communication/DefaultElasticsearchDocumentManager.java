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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * Default Elasticsearch implementation of {@link ElasticsearchDocumentManager}.
 * <p>
 * This manager delegates document operations to the Elasticsearch Java client.
 * Standard JNoSQL operations such as insert, update, delete, select, and count
 * are translated into Elasticsearch requests executed against the configured
 * index. Native Elasticsearch searches are supported through
 * {@link #search(SearchRequest)}.
 * </p>
 */
class DefaultElasticsearchDocumentManager implements ElasticsearchDocumentManager {


    private final ElasticsearchClient elasticsearchClient;

    private final String index;

    DefaultElasticsearchDocumentManager(ElasticsearchClient elasticsearchClient, String index) {
        this.elasticsearchClient = elasticsearchClient;
        this.index = index;
    }

    @Override
    public String name() {
        return index;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        requireNonNull(entity, "entity is required");
        var id = entity.find(EntityConverter.ID_FIELD)
                .orElseThrow(() -> new ElasticsearchKeyFoundException(entity.toString()));
        Map<String, Object> jsonObject = EntityConverter.getMap(entity);
        try {
            var indexRequest = IndexRequest.of(b ->
                    b.index(index)
                            .id(id.get(String.class)).document(jsonObject)
            );
            elasticsearchClient.index(indexRequest);
        } catch (IOException e) {
            throw new ElasticsearchException("An error to insert in Elastic search", e);
        }
        return entity;
    }


    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("The insert with TTL does not support");
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
    public CommunicationEntity update(CommunicationEntity entity) throws NullPointerException {
        return insert(entity);
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update).collect(Collectors.toList());
    }

    @Override
    public void delete(DeleteQuery query) throws NullPointerException {
        requireNonNull(query, "query is required");

        var select = new ElasticsearchDocumentQuery(query);

        List<CommunicationEntity> entities = select(select).toList();

        if (entities.isEmpty()) {
            return;
        }

        BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

        entities.stream()
                .map(entity -> entity.find(EntityConverter.ID_FIELD).orElseThrow().get(String.class))
                .forEach(id ->
                        bulkRequest.operations(op -> op
                                .delete(DeleteOperation.of(dp -> dp
                                        .index(index)
                                        .id(id)))
                        )
                );
        try {
            elasticsearchClient.bulk(bulkRequest.build());
        } catch (IOException e) {
            throw new ElasticsearchException("An error to delete entities on elasticsearch", e);
        }
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) throws NullPointerException {
        requireNonNull(query, "query is required");
        return EntityConverter.query(query, elasticsearchClient, index);
    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        try {
            return elasticsearchClient.count(CountRequest.of(s -> s.index(index)
                    .query(MatchQuery.of(m -> m
                            .field(EntityConverter.ENTITY)
                            .query(documentCollection))._toQuery()))).count();
        } catch (IOException e) {
            throw new CommunicationException("Error on ES when try to execute count to document collection:" + documentCollection, e);
        }
    }

    @Override
    public Stream<CommunicationEntity> search(SearchRequest query) {
        Objects.requireNonNull(query, "query is required");
        try {
            var responses = elasticsearchClient.search(query, Map.class);
            return EntityConverter.getDocumentEntityStream(elasticsearchClient, responses);
        } catch (IOException e) {
            throw new ElasticsearchException("An error when do search from QueryBuilder on elasticsearch", e);
        }
    }

    @Override
    public void close() {
        try {
            elasticsearchClient._transport().close();
        } catch (IOException e) {
            throw new ElasticsearchException("An error when close the client", e);
        }
    }
}
