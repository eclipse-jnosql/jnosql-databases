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

package org.eclipse.jnosql.databases.solr.communication;

import jakarta.data.Sort;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.request.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * The default implementation of {@link SolrDocumentManager}.
 * <br/>
 * Closing a {@link DefaultSolrDocumentManager} has no effect.
 */
class DefaultSolrDocumentManager implements SolrDocumentManager {

    private final HttpJdkSolrClient solrClient;

    private final String database;

    private final boolean automaticCommit;

    DefaultSolrDocumentManager(HttpJdkSolrClient solrClient,
                               String database,
                               boolean automaticCommit) {

        this.solrClient = solrClient;
        this.database = database;
        this.automaticCommit = automaticCommit;
    }


    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");

        try {
            solrClient.add(database, SolrUtils.getDocument(entity));
            commit();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error to insert/update a information", e);
        }
        return entity;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("Apache Solr does not support save with TTL");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        final List<SolrInputDocument> documents = StreamSupport.stream(entities.spliterator(), false)
                .map(SolrUtils::getDocument).collect(toList());
        try {
            solrClient.add(database, documents);
            commit();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error to insert/update a information", e);
        }
        return entities;
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
        Objects.requireNonNull(entity, "entity is required");

        var id = entity.find("_id").orElseThrow(() ->
                new IllegalArgumentException("The _id field is required for update"));

        CriteriaCondition condition = CriteriaCondition.eq(id);
        var query = DeleteQuery.builder()
                .from(entity.name())
                .where(condition).build();
        delete(query);

        return insert(entity);
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
        Objects.requireNonNull(query, "query is required");
        try {
            solrClient.deleteByQuery(database, DocumentQueryConverter.convert(query));
            commit();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error to delete at Solr", e);
        }
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        try {
            SolrQuery solrQuery = buildSolrQuery(query);
            if (query.skip() > 0) {
                solrQuery.setStart((int) query.skip());
            }
            if (query.limit() > 0) {
                solrQuery.setRows((int) query.limit());
            }
            final List<SortClause> sorts = query.sorts().stream()
                            .map(convertSortToClause())
                            .collect(toList());
            solrQuery.setSorts(sorts);
            final QueryResponse response = solrClient.query(database, solrQuery);
            final SolrDocumentList documents = response.getResults();
            return SolrUtils.of(documents).stream();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error to query at Solr", e);
        }
    }

    private static Function<Sort<?>, SortClause> convertSortToClause() {
        return sort -> new SortClause(
                sort.property(),
                sort.isAscending()
                        ? SolrQuery.ORDER.asc
                        : SolrQuery.ORDER.desc);
    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.set("q", DocumentQueryConverter.entityCondition(documentCollection));
            solrQuery.setRows(0);
            final QueryResponse response = solrClient.query(database, solrQuery);
            return response.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error to execute count at Solr", e);
        }
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        try {
            SolrQuery solrQuery = buildSolrQuery(query);
            solrQuery.setRows(0);
            final QueryResponse response = solrClient.query(database, solrQuery);
            return response.getResults().getNumFound();
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error to execute count at Solr", e);
        }
    }

    private static SolrQuery buildSolrQuery(SelectQuery query) {
        SolrQuery solrQuery = new SolrQuery();
        final String queryExpression = DocumentQueryConverter.convert(query);
        solrQuery.set("q", queryExpression);
        return solrQuery;
    }

    /**
     * Closing a {@link DefaultSolrDocumentManager} has no effect.
     */
    @Override
    public void close() {

    }

    private void commit() {
        if (isAutomaticCommit()) {
            try {
                solrClient.commit(database);
            } catch (SolrServerException | IOException e) {
                throw new SolrException("Error to commit at Solr", e);
            }
        }
    }

    private Boolean isAutomaticCommit() {
        return automaticCommit;
    }


    @Override
    public List<CommunicationEntity> solr(String query) {
        Objects.requireNonNull(query, "query is required");

        try {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.set("q", query);
            final QueryResponse response = solrClient.query(database, solrQuery);
            final SolrDocumentList documents = response.getResults();
            return SolrUtils.of(documents);
        } catch (SolrServerException | IOException e) {
            throw new SolrException("Error to execute native query at Solr query: " + query, e);
        }
    }

    @Override
    public List<CommunicationEntity> solr(String query, Map<String, ?> params) {
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(params, "params is required");
        return solr(bindNativeQuery(query, params));
    }

    static String bindNativeQuery(String query, Map<String, ?> params) {
        String nativeQuery = query;
        var entries = params.entrySet().stream()
                .sorted(Comparator.comparingInt((Entry<String, ?> entry) -> entry.getKey().length()).reversed())
                .toList();
        for (Entry<String, ?> entry : entries) {
            String name = Objects.requireNonNull(entry.getKey(), "parameter name is required");
            Object value = Objects.requireNonNull(entry.getValue(), "parameter value is required");
            nativeQuery = nativeQuery.replace('@' + name, DocumentQueryConverter.escape(value));
        }
        return nativeQuery;
    }
}
