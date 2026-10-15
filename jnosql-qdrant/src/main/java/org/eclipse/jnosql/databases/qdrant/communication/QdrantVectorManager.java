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
import io.qdrant.client.QueryFactory;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.WithVectorsSelectorFactory;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.PointVectors;
import io.qdrant.client.grpc.Points.QueryPoints;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.ScrollPoints;
import io.qdrant.client.grpc.Points.ScrollResponse;
import io.qdrant.client.grpc.Points.UpsertPoints;
import jakarta.data.Sort;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.vector.DenseVector;
import org.eclipse.jnosql.mapping.vector.VectorManager;
import org.eclipse.jnosql.mapping.vector.VectorSelectQuery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;
import java.util.function.Consumer;
import java.util.Spliterators;
import java.util.Spliterator;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The Qdrant implementation of {@link VectorManager}.
 * Each entity name maps to a Qdrant collection, which is created on the first insert when missing.
 * The {@link QdrantClient} is owned by the {@link QdrantVectorManagerFactory};
 * closing a {@link QdrantVectorManager} has no effect.
 */
final class QdrantVectorManager implements VectorManager {

    private static final int PAGE_SIZE = 256;

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
        Objects.requireNonNull(entity, "entity is required");
        upsert(entity.name(), List.of(entity));
        return entity;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(ttl, "ttl is required");
        throw new UnsupportedOperationException("Qdrant does not support save with TTL");
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
        throw new UnsupportedOperationException("Qdrant does not support save with TTL");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        Element idElement = entity.find(QdrantPointIds.ID_FIELD).orElseThrow(() ->
                new UnsupportedOperationException("To update this CommunicationEntity the field `_id` is required"));
        PointId id = QdrantPointIds.of(QdrantValueWriteDecorator.convert(idElement.value()));
        String collection = entity.name();
        Filter byId = Filter.newBuilder().addMust(ConditionFactory.hasId(id)).build();
        if (!exists(collection, byId)) {
            return entity;
        }
        List<Element> vectors = entity.elements().stream()
                .filter(element -> QdrantValueWriteDecorator.convert(element.value()) instanceof org.eclipse.jnosql.mapping.vector.Vector)
                .toList();
        if (vectors.size() > 1) {
            throw new IllegalArgumentException("The entity " + collection + " must have at most one vector element");
        }
        Map<String, io.qdrant.client.grpc.JsonWithInt.Value> payload = new LinkedHashMap<>(
                QdrantPayloadConverter.toPayload(entity.elements().stream()
                        .filter(element -> !vectors.contains(element)).toList()));
        payload.put(QdrantPointIds.ID_FIELD, QdrantPayloadConverter.toValue(
                QdrantPointIds.storedValue(QdrantValueWriteDecorator.convert(idElement.value()))));
        if (!vectors.isEmpty()) {
            float[] values = QdrantPointConverter.values(vectors.get(0));
            validateDimension(collection, values.length);
            payload.put(QdrantPointConverter.VECTOR_FIELD, ValueFactory.value(vectors.get(0).name()));
            QdrantCalls.await(client.updateVectorsAsync(collection, List.of(PointVectors.newBuilder()
                    .setId(id).setVectors(VectorsFactory.vectors(values)).build()), true, null, null));
        }
        QdrantCalls.await(client.setPayloadAsync(collection, payload, id, true, null, null));
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
        Filter filter = QdrantFilterConverter.convert(query.condition()).orElseGet(Filter::getDefaultInstance);
        try {
            QdrantCalls.await(client.deleteAsync(query.name(), filter));
        } catch (RuntimeException exception) {
            if (!QdrantCalls.isNotFound(exception)) {
                throw exception;
            }
        }
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        String collection = query.name();
        Filter filter = QdrantFilterConverter.convert(query.condition()).orElseGet(Filter::getDefaultInstance);
        boolean sorted = !query.sorts().isEmpty();
        long skip = query.skip();
        long limit = query.limit();

        Stream<CommunicationEntity> stream;
        if (sorted) {
            stream = scroll(collection, filter, List.of(), Long.MAX_VALUE).sorted(comparator(query.sorts()));
        } else {
            stream = scroll(collection, filter, query.columns(), limit <= 0 ? Long.MAX_VALUE : skip + limit);
        }
        stream = stream.map(entity -> project(entity, query.columns()));
        if (skip > 0) {
            stream = stream.skip(skip);
        }
        if (limit > 0) {
            stream = stream.limit(limit);
        }
        return stream;
    }

    private Stream<CommunicationEntity> scroll(String collection, Filter filter, List<String> columns, long needed) {
        int pageSize = (int) Math.min(PAGE_SIZE, Math.max(1, needed));
        Spliterator<CommunicationEntity> spliterator = new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE,
                Spliterator.ORDERED) {

            private final Deque<CommunicationEntity> buffer = new ArrayDeque<>();
            private PointId offset;
            private boolean finished;

            @Override
            public boolean tryAdvance(Consumer<? super CommunicationEntity> action) {
                while (buffer.isEmpty() && !finished) {
                    fetchPage();
                }
                CommunicationEntity next = buffer.poll();
                if (next == null) {
                    return false;
                }
                action.accept(next);
                return true;
            }

            private void fetchPage() {
                ScrollPoints.Builder builder = ScrollPoints.newBuilder()
                        .setCollectionName(collection)
                        .setFilter(filter)
                        .setLimit(pageSize)
                        .setWithPayload(payloadSelector(columns))
                        .setWithVectors(WithVectorsSelectorFactory.enable(true));
                if (offset != null) {
                    builder.setOffset(offset);
                }
                try {
                    ScrollResponse response = QdrantCalls.await(client.scrollAsync(builder.build()));
                    for (RetrievedPoint point : response.getResultList()) {
                        buffer.add(QdrantPointConverter.toEntity(collection, point.getId(),
                                point.getPayloadMap(), point.getVectors()));
                    }
                    offset = response.hasNextPageOffset() ? response.getNextPageOffset() : null;
                    finished = offset == null;
                } catch (RuntimeException exception) {
                    if (!QdrantCalls.isNotFound(exception)) {
                        throw exception;
                    }
                    finished = true;
                }
            }
        };
        return StreamSupport.stream(spliterator, false);
    }

    @Override
    public long count(String entity) {
        Objects.requireNonNull(entity, "entity is required");
        return count(entity, Filter.getDefaultInstance());
    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        if (query.skip() > 0 || query.limit() > 0) {
            return VectorManager.super.count(query);
        }
        return count(query.name(), QdrantFilterConverter.convert(query.condition())
                .orElseGet(Filter::getDefaultInstance));
    }

    @Override
    public List<CommunicationEntity> search(VectorSelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        if (!(query.vector() instanceof DenseVector dense)) {
            throw new UnsupportedOperationException("Qdrant only supports dense vectors but got: "
                    + query.vector().getClass().getName());
        }
        String collection = query.name();
        Optional<Integer> size = collectionSize(collection);
        if (size.isEmpty()) {
            return List.of();
        }
        if (size.get() != dense.dimensions()) {
            throw new IllegalArgumentException("The query vector has " + dense.dimensions()
                    + " dimensions but the collection " + collection + " expects " + size.get());
        }
        QueryPoints.Builder builder = QueryPoints.newBuilder()
                .setCollectionName(collection)
                .setQuery(QueryFactory.nearest(dense.values()))
                .setLimit(query.limit())
                .setOffset(query.skip())
                .setWithPayload(payloadSelector(query.columns()))
                .setWithVectors(WithVectorsSelectorFactory.enable(true));
        QdrantFilterConverter.convert(query.condition()).ifPresent(builder::setFilter);
        query.threshold().ifPresent(builder::setScoreThreshold);
        List<ScoredPoint> points = QdrantCalls.await(client.queryAsync(builder.build()));
        return points.stream()
                .map(point -> project(QdrantPointConverter.toEntity(collection, point.getId(),
                        point.getPayloadMap(), point.getVectors()), query.columns()))
                .toList();
    }

    @Override
    public void close() {
        // the client is owned and closed by the factory
    }

    @Override
    public String toString() {
        return "QdrantVectorManager{" + "database='" + database + '\'' + '}';
    }

    private void upsert(String collection, List<CommunicationEntity> entities) {
        List<PointStruct> points = entities.stream().map(QdrantPointConverter::toPoint).toList();
        if (points.isEmpty()) {
            return;
        }
        List<Integer> dimensions = entities.stream()
                .map(entity -> QdrantPointConverter.values(QdrantPointConverter.vectorElement(entity)).length)
                .toList();
        int dimension = dimensions.get(0);
        dimensions.forEach(size -> {
            if (size != dimension) {
                throw new IllegalArgumentException("All vectors of the collection " + collection
                        + " must have the same dimension");
            }
        });
        ensureCollection(collection, dimension);
        QdrantCalls.await(client.upsertAsync(UpsertPoints.newBuilder()
                .setCollectionName(collection).setWait(true).addAllPoints(points).build()));
    }

    private void ensureCollection(String collection, int dimension) {
        if (Boolean.TRUE.equals(QdrantCalls.await(client.collectionExistsAsync(collection)))) {
            validateDimension(collection, dimension);
            return;
        }
        QdrantDistance distance = collectionSettings.distanceOptional().orElseThrow(() ->
                new IllegalStateException("The collection " + collection + " does not exist and "
                        + QdrantConfigurations.DISTANCE.get()
                        + " is not configured to create it"));
        long size = collectionSettings.vectorSizeOptional().orElse((long) dimension);
        if (size != dimension) {
            throw new IllegalArgumentException("The vector has " + dimension
                    + " dimensions but the configured size is " + size);
        }
        try {
            QdrantCalls.await(client.createCollectionAsync(collection, VectorParams.newBuilder()
                    .setSize(size).setDistance(distance.toDistance()).build()));
        } catch (RuntimeException exception) {
            if (!QdrantCalls.isAlreadyExists(exception)) {
                throw exception;
            }
        }
    }

    private void validateDimension(String collection, int dimension) {
        collectionSize(collection).filter(size -> size != dimension).ifPresent(size -> {
            throw new IllegalArgumentException("The vector has " + dimension + " dimensions but the collection "
                    + collection + " expects " + size);
        });
    }

    private Optional<Integer> collectionSize(String collection) {
        try {
            var info = QdrantCalls.await(client.getCollectionInfoAsync(collection));
            return Optional.of((int) info.getConfig().getParams().getVectorsConfig().getParams().getSize());
        } catch (RuntimeException exception) {
            if (QdrantCalls.isNotFound(exception)) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private boolean exists(String collection, Filter filter) {
        return count(collection, filter) > 0;
    }

    private long count(String collection, Filter filter) {
        try {
            return QdrantCalls.await(client.countAsync(collection, filter, true));
        } catch (RuntimeException exception) {
            if (QdrantCalls.isNotFound(exception)) {
                return 0;
            }
            throw exception;
        }
    }

    private static io.qdrant.client.grpc.Points.WithPayloadSelector payloadSelector(List<String> columns) {
        if (columns.isEmpty()) {
            return WithPayloadSelectorFactory.enable(true);
        }
        List<String> fields = new ArrayList<>(columns);
        fields.add(QdrantPointIds.ID_FIELD);
        fields.add(QdrantPointConverter.VECTOR_FIELD);
        return WithPayloadSelectorFactory.include(fields);
    }

    private static CommunicationEntity project(CommunicationEntity entity, List<String> columns) {
        if (columns.isEmpty()) {
            return entity;
        }
        List<Element> kept = entity.elements().stream()
                .filter(element -> columns.contains(element.name())
                        || QdrantPointIds.ID_FIELD.equals(element.name()))
                .toList();
        return CommunicationEntity.of(entity.name(), kept);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparator<CommunicationEntity> comparator(List<Sort<?>> sorts) {
        Comparator<CommunicationEntity> result = null;
        for (Sort<?> sort : sorts) {
            Comparator<CommunicationEntity> current = Comparator.comparing(
                    entity -> (Comparable) entity.find(sort.property())
                            .map(element -> QdrantValueWriteDecorator.convert(element.value())).orElse(null),
                    Comparator.nullsFirst(Comparator.naturalOrder()));
            if (!sort.isAscending()) {
                current = current.reversed();
            }
            result = result == null ? current : result.thenComparing(current);
        }
        return result;
    }
}
