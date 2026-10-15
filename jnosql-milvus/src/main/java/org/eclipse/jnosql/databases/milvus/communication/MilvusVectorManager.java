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

import io.milvus.orm.iterator.QueryIterator;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DescribeCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.collection.request.GetLoadStateReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.QueryIteratorReq;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import com.google.gson.JsonObject;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The Milvus implementation of {@link VectorManager}.
 * Each entity name maps to a Milvus collection, created on the first insert when missing, in the Milvus database
 * that matches the name of this manager.
 * The {@link MilvusClientV2} is owned by the {@link MilvusVectorManagerFactory};
 * closing a {@link MilvusVectorManager} has no effect.
 */
final class MilvusVectorManager implements VectorManager {

    private static final int PAGE_SIZE = 256;

    private static final String COUNT = "count(*)";

    private final MilvusClientV2 client;

    private final String database;

    private final MilvusCollectionSettings settings;

    MilvusVectorManager(MilvusClientV2 client, String database, MilvusCollectionSettings settings) {
        this.client = client;
        this.database = database;
        this.settings = settings;
    }

    @Override
    public String name() {
        return database;
    }

    MilvusCollectionSettings collectionSettings() {
        return settings;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        upsert(entity.name(), List.of(entity));
        return entity;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(ttl, "ttl is required");
        throw new UnsupportedOperationException("Milvus does not support save with TTL");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        Map<String, List<CommunicationEntity>> byCollection = new LinkedHashMap<>();
        entities.forEach(entity -> byCollection.computeIfAbsent(entity.name(), key -> new ArrayList<>()).add(entity));
        byCollection.forEach(this::upsert);
        return entities;
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(ttl, "ttl is required");
        throw new UnsupportedOperationException("Milvus does not support save with TTL");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        Element idElement = entity.find(MilvusRowConverter.ID_FIELD).orElseThrow(() ->
                new UnsupportedOperationException("To update this CommunicationEntity the field `_id` is required"));
        Object id = MilvusValueWriteDecorator.convert(idElement.value());
        String collection = entity.name();
        Optional<MilvusCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return entity;
        }
        Map<String, Object> parameters = Map.of("id", id instanceof String ? id
                : MilvusFilterConverter.normalize(id));
        QueryResp found = client.query(QueryReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .filter(MilvusRowConverter.ID_FIELD + " == {id}")
                .filterTemplateValues(parameters)
                .outputFields(List.of(MilvusRowConverter.ID_FIELD))
                .limit(1)
                .consistencyLevel(settings.consistency())
                .build());
        if (found.getQueryResults().isEmpty()) {
            return entity;
        }
        upsert(collection, List.of(entity));
        return entity;
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        entities.forEach(this::update);
        return entities;
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");
        String collection = query.name();
        Optional<MilvusCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return;
        }
        ensureLoaded(collection);
        Expression expression = expression(query.condition().flatMap(c -> MilvusFilterConverter.convert(Optional.of(c))),
                schema.get());
        client.delete(DeleteReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .filter(expression.text())
                .filterTemplateValues(expression.parameters())
                .consistencyLevel(settings.consistency())
                .build());
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        if (!query.sorts().isEmpty()) {
            throw new UnsupportedOperationException("Milvus does not support sorting regular queries: "
                    + "the order of a query is not guaranteed");
        }
        String collection = query.name();
        Optional<MilvusCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return Stream.empty();
        }
        ensureLoaded(collection);
        Expression expression = expression(MilvusFilterConverter.convert(query.condition()), schema.get());
        long skip = query.skip();
        long limit = query.limit();
        QueryIteratorReq request = QueryIteratorReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .expr(expression.text())
                .filterTemplateValues(expression.parameters())
                .outputFields(outputFields(query.columns(), schema.get()))
                .batchSize(PAGE_SIZE)
                .offset(skip)
                .limit(limit > 0 ? limit : -1)
                .consistencyLevel(settings.consistency())
                .build();
        return iterate(collection, request, schema.get());
    }

    private Stream<CommunicationEntity> iterate(String collection, QueryIteratorReq request,
                                                MilvusCollectionSchema schema) {
        Spliterator<CommunicationEntity> spliterator = new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE,
                Spliterator.ORDERED) {

            private final Deque<CommunicationEntity> buffer = new ArrayDeque<>();
            private QueryIterator iterator;
            private boolean finished;

            @Override
            public boolean tryAdvance(Consumer<? super CommunicationEntity> action) {
                if (iterator == null) {
                    iterator = client.queryIterator(request);
                }
                while (buffer.isEmpty() && !finished) {
                    List<QueryResultsWrapper.RowRecord> page = iterator.next();
                    if (page.isEmpty()) {
                        finished = true;
                        iterator.close();
                    }
                    page.forEach(row -> buffer.add(MilvusRowConverter.toEntity(collection,
                            row.getFieldValues(), schema.vectorField())));
                }
                CommunicationEntity next = buffer.poll();
                if (next == null) {
                    return false;
                }
                action.accept(next);
                return true;
            }
        };
        return StreamSupport.stream(spliterator, false).onClose(() -> {
        });
    }

    @Override
    public long count(String entity) {
        Objects.requireNonNull(entity, "entity is required");
        return count(entity, Optional.empty());
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        if (query.skip() > 0 || query.limit() > 0) {
            return VectorManager.super.count(query);
        }
        return count(query.name(), MilvusFilterConverter.convert(query.condition()));
    }

    @Override
    public List<CommunicationEntity> search(VectorSelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        if (!(query.vector() instanceof DenseVector dense)) {
            throw new UnsupportedOperationException("Milvus only supports dense vectors but got: "
                    + query.vector().getClass().getName());
        }
        if (query.limit() <= 0) {
            throw new IllegalArgumentException("A vector search requires a positive limit");
        }
        String collection = query.name();
        Optional<MilvusCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return List.of();
        }
        MilvusCollectionSchema description = schema.get();
        if (description.dimension() != dense.dimensions()) {
            throw new IllegalArgumentException("The query vector has " + dense.dimensions()
                    + " dimensions but the collection " + collection + " expects " + description.dimension());
        }
        ensureLoaded(collection);
        Map<String, Object> searchParams = new HashMap<>();
        query.threshold().ifPresent(threshold -> MilvusThreshold.apply(settings.metric(), threshold, searchParams));
        SearchReq.SearchReqBuilder builder = SearchReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .annsField(description.vectorField())
                .data(List.of(new FloatVec(dense.values())))
                .limit((int) query.limit())
                .offset(query.skip())
                .outputFields(outputFields(query.columns(), description))
                .searchParams(searchParams)
                .consistencyLevel(settings.consistency());
        MilvusFilterConverter.convert(query.condition()).ifPresent(filter ->
                builder.filter(filter.expression()).filterTemplateValues(filter.parameters()));
        SearchResp response = client.search(builder.build());
        List<CommunicationEntity> entities = new ArrayList<>();
        if (!response.getSearchResults().isEmpty()) {
            for (SearchResp.SearchResult result : response.getSearchResults().get(0)) {
                Map<String, Object> row = new LinkedHashMap<>(result.getEntity());
                row.putIfAbsent(MilvusRowConverter.ID_FIELD, result.getId());
                entities.add(MilvusRowConverter.toEntity(collection, row, description.vectorField()));
            }
        }
        return entities;
    }

    @Override
    public void close() {
        // the client is owned and closed by the factory
    }

    @Override
    public String toString() {
        return "MilvusVectorManager{" + "database='" + database + '\'' + '}';
    }

    private void upsert(String collection, List<CommunicationEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        List<Element> vectors = new ArrayList<>();
        for (CommunicationEntity entity : entities) {
            Element vector = MilvusRowConverter.vectorElement(entity);
            MilvusRowConverter.values(vector);
            MilvusRowConverter.ensureId(entity);
            vectors.add(vector);
        }
        int dimension = MilvusRowConverter.values(vectors.get(0)).length;
        for (Element vector : vectors) {
            if (MilvusRowConverter.values(vector).length != dimension) {
                throw new IllegalArgumentException("All vectors of the collection " + collection
                        + " must have the same dimension");
            }
        }
        Optional<MilvusCollectionSchema> existing = schema(collection);
        if (existing.isEmpty()) {
            createCollection(collection, entities.get(0), vectors.get(0), dimension);
            existing = schema(collection);
        }
        MilvusCollectionSchema schema = existing.orElseThrow();
        List<JsonObject> rows = new ArrayList<>();
        for (int index = 0; index < entities.size(); index++) {
            schema.validate(collection, entities.get(index), vectors.get(index), dimension);
            rows.add(MilvusRowConverter.toRow(entities.get(index), vectors.get(index)));
        }
        client.upsert(UpsertReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .data(rows)
                .build());
    }

    private void createCollection(String collection, CommunicationEntity entity, Element vector, int dimension) {
        int size = settings.vectorSize() == null ? dimension : settings.vectorSize();
        if (size != dimension) {
            throw new IllegalArgumentException("The vector has " + dimension
                    + " dimensions but the configured size is " + size);
        }
        CreateCollectionReq.CollectionSchema schema = MilvusCollectionSchema.create(entity, vector, dimension,
                settings.maxLength(), client);
        IndexParam index = IndexParam.builder()
                .fieldName(vector.name())
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(settings.metric().toMetricType())
                .build();
        try {
            client.createCollection(CreateCollectionReq.builder()
                    .databaseName(database)
                    .collectionName(collection)
                    .collectionSchema(schema)
                    .indexParams(List.of(index))
                    .consistencyLevel(settings.consistency())
                    .build());
        } catch (RuntimeException exception) {
            if (!exists(collection)) {
                throw exception;
            }
        }
    }

    private boolean exists(String collection) {
        return Boolean.TRUE.equals(client.hasCollection(HasCollectionReq.builder()
                .databaseName(database).collectionName(collection).build()));
    }

    private Optional<MilvusCollectionSchema> schema(String collection) {
        if (!exists(collection)) {
            return Optional.empty();
        }
        return Optional.of(MilvusCollectionSchema.of(client.describeCollection(DescribeCollectionReq.builder()
                .databaseName(database).collectionName(collection).build())));
    }

    private void ensureLoaded(String collection) {
        Boolean loaded = client.getLoadState(GetLoadStateReq.builder()
                .databaseName(database).collectionName(collection).build());
        if (!Boolean.TRUE.equals(loaded)) {
            client.loadCollection(LoadCollectionReq.builder()
                    .databaseName(database).collectionName(collection).build());
        }
    }

    private long count(String collection, Optional<MilvusFilterConverter.Filter> filter) {
        Optional<MilvusCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return 0;
        }
        ensureLoaded(collection);
        Expression expression = expression(filter, schema.get());
        QueryResp response = client.query(QueryReq.builder()
                .databaseName(database)
                .collectionName(collection)
                .filter(expression.text())
                .filterTemplateValues(expression.parameters())
                .outputFields(List.of(COUNT))
                .consistencyLevel(settings.consistency())
                .build());
        return ((Number) response.getQueryResults().get(0).getEntity().get(COUNT)).longValue();
    }

    private static List<String> outputFields(List<String> columns, MilvusCollectionSchema schema) {
        if (columns.isEmpty()) {
            return List.of("*");
        }
        List<String> fields = new ArrayList<>(columns);
        fields.add(MilvusRowConverter.ID_FIELD);
        fields.add(schema.vectorField());
        return fields.stream().distinct().toList();
    }

    private record Expression(String text, Map<String, Object> parameters) {
    }

    private static Expression expression(Optional<MilvusFilterConverter.Filter> filter, MilvusCollectionSchema schema) {
        if (filter.isPresent()) {
            return new Expression(filter.get().expression(), filter.get().parameters());
        }
        Object lowest = schema.idType() == io.milvus.v2.common.DataType.Int64 ? (Object) Long.MIN_VALUE : "";
        return new Expression(MilvusRowConverter.ID_FIELD + " >= {all}", Map.of("all", lowest));
    }
}
