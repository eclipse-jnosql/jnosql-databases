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

package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;
import jakarta.data.Sort;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.Elements;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.StreamSupport.stream;
import static org.eclipse.jnosql.databases.mongodb.communication.MongoDBUtils.ID_FIELD;
import static org.eclipse.jnosql.databases.mongodb.communication.MongoDBUtils.getDocument;

/**
 * The mongodb implementation to {@link DatabaseManager} that does not support TTL methods
 * {@link MongoDBDocumentManager#insert(CommunicationEntity, Duration)}
 * Closing a {@link MongoDBDocumentManager} has no effect.
 */
public class MongoDBDocumentManager implements DatabaseManager {

    private static final BsonDocument EMPTY = new BsonDocument();

    private final MongoDatabase mongoDatabase;

    private final String database;

    MongoDBDocumentManager(MongoDatabase mongoDatabase, String database) {
        this.mongoDatabase = mongoDatabase;
        this.database = database;
    }


    @Override
    public String name() {
        return database;
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        String collectionName = entity.name();
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        var document = getDocument(entity);
        if (document.get(ID_FIELD) == null) {
            document.remove(ID_FIELD);
            entity.remove(ID_FIELD);
        }

        collection.insertOne(document);
        boolean hasNotId = entity.elements().stream()
                .map(Element::name).noneMatch(k -> k.equals(ID_FIELD));
        if (hasNotId) {
            entity.add(Elements.of(ID_FIELD, document.get(ID_FIELD)));
        }
        return entity;
    }


    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("MongoDB does not support save with TTL");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert)
                .toList();
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> insert(e, ttl))
                .toList();
    }


    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");

        CommunicationEntity copy = entity.copy();
        String collectionName = entity.name();
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Document id = copy.find(ID_FIELD)
                .map(d -> new Document(d.name(), d.value().get()))
                .orElseThrow(() -> new UnsupportedOperationException("To update this DocumentEntity " +
                        "the field `id` is required"));
        copy.remove(ID_FIELD);
        collection.findOneAndReplace(id, getDocument(entity));
        return entity;
    }

    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update)
                .toList();
    }


    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");

        String collectionName = query.name();
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Bson mongoDBQuery = query.condition().map(DocumentQueryConversor::convert).orElse(EMPTY);
        collection.deleteMany(mongoDBQuery);
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        String collectionName = query.name();
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        Bson mongoDBQuery = query.condition().map(DocumentQueryConversor::convert).orElse(EMPTY);
        FindIterable<Document> documents = collection.find(mongoDBQuery);
        documents.projection(Projections.include(query.columns()));

        if (!query.sorts().isEmpty()) {
            documents.sort(sort(query.sorts()));
        }

        if (query.skip() > 0) {
            documents.skip((int) query.skip());
        }

        if (query.limit() > 0) {
            documents.limit((int) query.limit());
        }

        return stream(documents.spliterator(), false).map(MongoDBUtils::of)
                .map(ds -> CommunicationEntity.of(collectionName, ds));

    }

    @Override
    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        String collectionName = query.name();
        Bson mongoDBQuery = query.condition().map(DocumentQueryConversor::convert).orElse(EMPTY);
        return count(collectionName, mongoDBQuery);
    }

    @Override
    public long count(String documentCollection) {
        Objects.requireNonNull(documentCollection, "documentCollection is required");
        MongoCollection<Document> collection = mongoDatabase.getCollection(documentCollection);
        return collection.countDocuments();
    }

    /**
     * Closing a {@link MongoDBDocumentManager} has no effect.
     */
    @Override
    public void close() {

    }

    /**
     * Removes all documents from the collection that match the given query filter.
     * If no documents match, the collection is not modified.
     *
     * @param collectionName the collection name
     * @param filter         the delete filter
     * @return the number of documents deleted.
     * @throws NullPointerException when filter or collectionName is null
     */
    public long delete(String collectionName, Bson filter) {
        Objects.requireNonNull(filter, "filter is required");
        Objects.requireNonNull(collectionName, "collectionName is required");

        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        DeleteResult result = collection.deleteMany(filter);
        return result.getDeletedCount();
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param collectionName the collection name
     * @param pipeline       the aggregation pipeline
     * @return the stream of BSON Documents
     * @throws NullPointerException when filter or collectionName is null
     */
    public Stream<Map<String, BsonValue>> aggregate(String collectionName, Bson... pipeline) {
        Objects.requireNonNull(pipeline, "filter is required");
        Objects.requireNonNull(collectionName, "collectionName is required");
        MongoCollection<BsonDocument> collection = mongoDatabase.getCollection(collectionName, BsonDocument.class);
        AggregateIterable aggregate = collection.aggregate(Arrays.asList(pipeline));
        return stream(aggregate.spliterator(), false);
    }

    /**
     * Aggregates documents according to the specified aggregation pipeline.
     *
     * @param collectionName the collection name
     * @param pipeline       the aggregation pipeline
     * @return the stream result
     * @throws NullPointerException when pipeline or collectionName is null
     */
    public Stream<CommunicationEntity> aggregate(String collectionName, List<Bson> pipeline) {
        Objects.requireNonNull(pipeline, "pipeline is required");
        Objects.requireNonNull(collectionName, "collectionName is required");
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        AggregateIterable<Document> aggregate = collection.aggregate(pipeline);
        return stream(aggregate.spliterator(), false).map(MongoDBUtils::of)
                .map(ds -> CommunicationEntity.of(collectionName, ds));
    }

    /**
     * Finds all documents in the collection.
     *
     * @param collectionName the collection name
     * @param filter         the query filter
     * @return the stream result
     * @throws NullPointerException when filter or collectionName is null
     */
    public Stream<CommunicationEntity> select(String collectionName, Bson filter) {
        Objects.requireNonNull(filter, "filter is required");
        Objects.requireNonNull(collectionName, "collectionName is required");
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        FindIterable<Document> documents = collection.find(filter);
        return stream(documents.spliterator(), false).map(MongoDBUtils::of)
                .map(ds -> CommunicationEntity.of(collectionName, ds));
    }

    private Bson sort(Sort<?> sort) {
        return sort.isAscending() ? Sorts.ascending(sort.property()) : Sorts.descending(sort.property());
    }

    private Bson sort(List<Sort<?>> sorts) {
        List<Bson> bsonSorts = sorts.stream().map(this::sort).toList();
        return Sorts.orderBy(bsonSorts);
    }

    /**
     * Returns the number of documents in the collection that match the given query filter.
     *
     * @param collectionName the collection name
     * @param filter         the query filter
     * @return the number of documents founded.
     * @throws NullPointerException when filter or collectionName is null
     */
    public long count(String collectionName, Bson filter) {
        Objects.requireNonNull(filter, "filter is required");
        Objects.requireNonNull(collectionName, "collectionName is required");
        MongoCollection<Document> collection = mongoDatabase.getCollection(collectionName);
        return collection.countDocuments(filter);
    }

}
