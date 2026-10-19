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
package org.eclipse.jnosql.databases.weaviate.communication;

import io.weaviate.client6.v1.api.WeaviateClient;
import io.weaviate.client6.v1.api.collections.CollectionHandle;
import io.weaviate.client6.v1.api.collections.Vectors;
import io.weaviate.client6.v1.api.collections.WeaviateObject;
import io.weaviate.client6.v1.api.collections.data.DeleteManyResponse;
import io.weaviate.client6.v1.api.collections.data.InsertManyResponse;
import io.weaviate.client6.v1.api.collections.query.Filter;
import io.weaviate.client6.v1.api.collections.query.QueryResponse;
import io.weaviate.client6.v1.api.WeaviateApiException;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.vector.Vector;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The Weaviate implementation of {@link VectorManager}.
 * Each entity name maps to a Weaviate collection, created on the first insert when missing.
 * Weaviate has no databases, so the name of this manager does not scope the collections.
 * The {@link WeaviateClient} is owned by the {@link WeaviateVectorManagerFactory};
 * closing a {@link WeaviateVectorManager} has no effect.
 */
final class WeaviateVectorManager implements VectorManager {

    private static final int PAGE_SIZE = 256;

    private final WeaviateClient client;

    private final String database;

    private final WeaviateCollectionSettings settings;

    WeaviateVectorManager(WeaviateClient client, String database, WeaviateCollectionSettings settings) {
        this.client = client;
        this.database = database;
        this.settings = settings;
    }

    @Override
    public String name() {
        return database;
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
        throw new UnsupportedOperationException("Weaviate does not support save with TTL");
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
        throw new UnsupportedOperationException("Weaviate does not support save with TTL");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        Element idElement = entity.find(WeaviateNames.ID_FIELD).orElseThrow(() ->
                new UnsupportedOperationException("To update this CommunicationEntity the field `_id` is required"));
        Object id = WeaviateValueWriteDecorator.convert(idElement.value());
        String collection = WeaviateNames.collection(entity.name());
        Optional<WeaviateCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return entity;
        }
        CollectionHandle<Map<String, Object>> handle = client.collections.use(collection);
        if (!handle.data.exists(WeaviateNames.objectId(id.toString()))) {
            return entity;
        }
        upsert(entity.name(), List.of(entity));
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
        String collection = WeaviateNames.collection(query.name());
        Optional<WeaviateCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return;
        }
        CollectionHandle<Map<String, Object>> handle = client.collections.use(collection);
        Optional<Filter> filter = WeaviateFilterConverter.convert(query.condition(), schema.get());
        if (filter.isPresent()) {
            deleteMatching(handle, filter.get(), collection);
        } else {
            deleteAll(handle);
        }
    }

    private void deleteMatching(CollectionHandle<Map<String, Object>> handle, Filter filter, String collection) {
        DeleteManyResponse response;
        do {
            response = handle.data.deleteMany(filter);
            if (response.failed() > 0) {
                throw new IllegalStateException("Weaviate failed to delete " + response.failed()
                        + " objects of the collection " + collection);
            }
        } while (response.matches() > 0);
    }

    private void deleteAll(CollectionHandle<Map<String, Object>> handle) {
        List<WeaviateObject<Map<String, Object>>> page;
        do {
            page = handle.query.fetchObjects(fetch -> fetch.limit(PAGE_SIZE).returnProperties(List.of())).objects();
            if (!page.isEmpty()) {
                handle.data.deleteMany(page.stream().map(WeaviateObject::uuid).toArray(String[]::new));
            }
        } while (page.size() == PAGE_SIZE);
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        Optional<Request> request = request(query);
        if (request.isEmpty()) {
            return Stream.empty();
        }
        Request current = request.get();
        CursorSpliterator spliterator = new CursorSpliterator(current);
        Stream<CommunicationEntity> stream = StreamSupport.stream(spliterator, false);
        if (query.skip() > 0) {
            stream = stream.skip(query.skip());
        }
        if (query.limit() > 0) {
            stream = stream.limit(query.limit());
        }
        return stream;
    }

    @Override
    public long count(String entity) {
        Objects.requireNonNull(entity, "entity is required");
        String collection = WeaviateNames.collection(entity);
        if (schema(collection).isEmpty()) {
            return 0;
        }
        return client.collections.use(collection).size();
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        if (query.skip() > 0 || query.limit() > 0) {
            return VectorManager.super.count(query);
        }
        if (query.condition().isEmpty()) {
            return count(query.name());
        }
        Optional<Request> request = request(query);
        if (request.isEmpty()) {
            return 0;
        }
        Request current = request.get();
        Long total = client.collections.use(current.collection).aggregate
                .overAll(aggregation -> aggregation.includeTotalCount(true).filters(current.filterOption.orElseThrow())).totalCount();
        return total == null ? 0 : total;
    }

    @Override
    public List<CommunicationEntity> search(VectorSelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        Vector vector = query.vector();
        WeaviateVectors.Kind kind = WeaviateVectors.kind(vector);
        if (query.limit() <= 0) {
            throw new IllegalArgumentException("A vector search requires a positive limit");
        }
        Optional<Request> request = request(query);
        if (request.isEmpty()) {
            return List.of();
        }
        Request current = request.get();
        WeaviateCollectionSchema schema = current.schema;
        if (schema.kind() != kind) {
            throw new IllegalArgumentException("The collection " + query.name() + " stores " + schema.kind()
                    + " vectors but the query vector is " + kind);
        }
        Float maxDistance = query.threshold()
                .map(threshold -> WeaviateThreshold.maxDistance(schema.distance(), threshold))
                
                .orElse(null);
        CollectionHandle<Map<String, Object>> handle = client.collections.use(current.collection);
        QueryResponse<Map<String, Object>> response;
        try {
            response = nearVector(handle, query, current, vector, maxDistance);
        } catch (WeaviateApiException exception) {
            checkDimension(handle, schema, vector, exception);
            throw exception;
        }
        return response.objects().stream()
                .map(object -> WeaviateObjectConverter.toEntity(query.name(), object, schema))
                .toList();
    }

    private void checkDimension(CollectionHandle<Map<String, Object>> handle, WeaviateCollectionSchema schema,
                                Vector vector, WeaviateApiException cause) {
        handle.query.fetchObjects(fetch -> fetch.limit(1).returnProperties(List.of())
                        .includeVector(schema.vectorName())).objects().stream().findFirst()
                .map(WeaviateObject::vectors)
                .filter(vectors -> vectors.contains(schema.vectorName()))
                .map(vectors -> WeaviateVectors.dimension(WeaviateVectors.fromVectors(schema.kind(), vectors,
                        schema.vectorName())))
                .filter(dimension -> dimension != WeaviateVectors.dimension(vector))
                .ifPresent(dimension -> {
                    throw new IllegalArgumentException("The query vector has " + WeaviateVectors.dimension(vector)
                            + " dimensions but the collection expects " + dimension, cause);
                });
    }

    private QueryResponse<Map<String, Object>> nearVector(CollectionHandle<Map<String, Object>> handle,
                                                          VectorSelectQuery query, Request current,
                                                          Vector vector, Float maxDistance) {
        WeaviateCollectionSchema schema = current.schema;
        return handle.query
                .nearVector(WeaviateVectors.toTarget(schema.vectorName(), vector), near -> {
                    near.limit((int) query.limit())
                            .offset((int) query.skip())
                            .returnProperties(current.properties)
                            .includeVector(current.vectors);
                    current.filter().ifPresent(near::filters);
                    if (maxDistance != null) {
                        near.distance(maxDistance);
                    }
                    return near;
                });
    }

    @Override
    public void close() {
        // the client is owned and closed by the factory
    }

    @Override
    public String toString() {
        return "WeaviateVectorManager{" + "database='" + database + '\'' + '}';
    }

    /**
     * The parts of a query that regular selects and vector searches have in common:
     * the collection, its schema, the filter and the projection.
     */
    private record Request(String collection, WeaviateCollectionSchema schema, Optional<Filter> filterOption,
                           List<String> properties, List<String> vectors) {

        Optional<Filter> filter() {
            return filterOption;
        }
    }

    private Optional<Request> request(SelectQuery query) {
        if (!query.sorts().isEmpty()) {
            throw new UnsupportedOperationException("Weaviate does not support sorting regular queries: "
                    + "the order of a query is not guaranteed");
        }
        String collection = WeaviateNames.collection(query.name());
        Optional<WeaviateCollectionSchema> schema = schema(collection);
        if (schema.isEmpty()) {
            return Optional.empty();
        }
        WeaviateCollectionSchema description = schema.get();
        Optional<Filter> filter = WeaviateFilterConverter.convert(query.condition(), description);
        Set<String> properties = new LinkedHashSet<>();
        boolean vector = query.columns().isEmpty();
        if (query.columns().isEmpty()) {
            properties.addAll(description.propertyNames());
        } else {
            properties.add(WeaviateNames.ID_PROPERTY);
            for (String column : query.columns()) {
                String property = WeaviateNames.property(column);
                if (description.hasProperty(property)) {
                    properties.add(property);
                } else if (property.equals(description.vectorName())) {
                    vector = true;
                }
            }
        }
        List<String> vectors = vector ? List.of(description.vectorName()) : List.of();
        return Optional.of(new Request(collection, description, filter, List.copyOf(properties), vectors));
    }

    private final class CursorSpliterator extends Spliterators.AbstractSpliterator<CommunicationEntity> {

        private final Request request;
        private final CollectionHandle<Map<String, Object>> handle;
        private final Deque<CommunicationEntity> buffer = new ArrayDeque<>();
        private String cursor;
        private boolean finished;

        private CursorSpliterator(Request request) {
            super(Long.MAX_VALUE, Spliterator.ORDERED);
            this.request = request;
            this.handle = client.collections.use(request.collection);
        }

        @Override
        public boolean tryAdvance(Consumer<? super CommunicationEntity> action) {
            while (buffer.isEmpty() && !finished) {
                List<WeaviateObject<Map<String, Object>>> page = handle.query.fetchObjects(fetch -> {
                    fetch.limit(PAGE_SIZE)
                            .returnProperties(request.properties)
                            .includeVector(request.vectors);
                    request.filter().ifPresent(fetch::filters);
                    if (cursor != null) {
                        fetch.after(cursor);
                    }
                    return fetch;
                }).objects();
                if (page.size() < PAGE_SIZE) {
                    finished = true;
                }
                for (WeaviateObject<Map<String, Object>> object : page) {
                    cursor = object.uuid();
                    buffer.add(WeaviateObjectConverter.toEntity(request.collection, object, request.schema));
                }
            }
            CommunicationEntity next = buffer.poll();
            if (next == null) {
                return false;
            }
            action.accept(next);
            return true;
        }
    }

    private Optional<WeaviateCollectionSchema> schema(String collection) {
        try {
            return client.collections.getConfig(collection)
                    .map(config -> WeaviateCollectionSchema.of(config, settings));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void upsert(String name, List<CommunicationEntity> entities) {
        if (entities.isEmpty()) {
            return;
        }
        String collection = WeaviateNames.collection(name);
        List<Element> vectors = new ArrayList<>();
        for (CommunicationEntity entity : entities) {
            Element vector = WeaviateObjectConverter.vectorElement(entity);
            WeaviateVectors.kind(WeaviateObjectConverter.vector(vector));
            WeaviateObjectConverter.ensureId(entity);
            vectors.add(vector);
        }
        Vector first = WeaviateObjectConverter.vector(vectors.get(0));
        WeaviateVectors.Kind kind = WeaviateVectors.kind(first);
        int dimension = WeaviateVectors.dimension(first);
        for (Element element : vectors) {
            Vector vector = WeaviateObjectConverter.vector(element);
            if (WeaviateVectors.kind(vector) != kind || WeaviateVectors.dimension(vector) != dimension) {
                throw new IllegalArgumentException("All vectors of the collection " + collection
                        + " must have the same type and dimension");
            }
        }
        Optional<WeaviateCollectionSchema> existing = schema(collection);
        if (existing.isEmpty()) {
            createCollection(collection, entities.get(0), vectors.get(0));
            existing = schema(collection);
        }
        WeaviateCollectionSchema schema = existing.orElseThrow();
        CollectionHandle<Map<String, Object>> handle = client.collections.use(collection);
        List<WeaviateObject<Map<String, Object>>> inserts = new ArrayList<>();
        for (int index = 0; index < entities.size(); index++) {
            CommunicationEntity entity = entities.get(index);
            schema.validate(entity, vectors.get(index));
            WeaviateObject<Map<String, Object>> object = WeaviateObjectConverter.toObject(entity, vectors.get(index));
            if (handle.data.exists(object.uuid())) {
                replace(handle, object);
            } else {
                inserts.add(object);
            }
        }
        insertMany(handle, inserts);
    }

    private void insertMany(CollectionHandle<Map<String, Object>> handle,
                            List<WeaviateObject<Map<String, Object>>> inserts) {
        if (inserts.isEmpty()) {
            return;
        }
        InsertManyResponse response = handle.data.insertMany(inserts);
        if (!response.errors().isEmpty()) {
            throw new IllegalArgumentException("Weaviate rejected the objects: " + response.errors());
        }
    }

    private void replace(CollectionHandle<Map<String, Object>> handle, WeaviateObject<Map<String, Object>> object) {
        try {
            handle.data.replace(object.uuid(), replace -> replace
                    .properties(object.properties())
                    .vectors(object.vectors() == null ? new Vectors[0] : new Vectors[]{object.vectors()}));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void createCollection(String collection, CommunicationEntity entity, Element vector) {
        try {
            client.collections.create(WeaviateCollectionSchema.create(collection, entity, vector, settings));
        } catch (WeaviateApiException | IOException exception) {
            if (!exists(collection)) {
                if (exception instanceof IOException io) {
                    throw new UncheckedIOException(io);
                }
                throw (WeaviateApiException) exception;
            }
        }
    }

    private boolean exists(String collection) {
        try {
            return client.collections.exists(collection);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
