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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.DocumentCreateEntity;
import com.arangodb.entity.DocumentUpdateEntity;
import jakarta.json.JsonObject;
import org.eclipse.jnosql.communication.graph.CommunicationEdge;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.arangodb.internal.ArangoErrors.ERROR_ARANGO_DATA_SOURCE_NOT_FOUND;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

class DefaultArangoDBDocumentManager implements ArangoDBDocumentManager {

    private static final Logger LOGGER = Logger.getLogger(DefaultArangoDBDocumentManager.class.getName());

    public static final String KEY = "_key";
    public static final String ID = "_id";
    public static final String REV = "_rev";
    public static final String FROM = "_from";
    public static final String TO = "_to";

    private final ArangoDatabase db;

    DefaultArangoDBDocumentManager(String database, ArangoDB arangoDB) {
        db = arangoDB.db(database);
    }

    @Override
    public Optional<String> defaultIdFieldName() {
        return Optional.of(KEY);
    }

    @Override
    public String name() {
        return db.name();
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity) {
        requireNonNull(entity, "entity is required");
        String collectionName = entity.name();
        checkCollection(collectionName);
        JsonObject jsonObject = ArangoDBUtil.toJsonObject(entity);
        DocumentCreateEntity<Void> arangoDocument = db
                .collection(collectionName).insertDocument(jsonObject);
        updateEntity(entity, arangoDocument.getKey(), arangoDocument.getId(), arangoDocument.getRev());
        return entity;
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        requireNonNull(entity, "entity is required");
        String collectionName = entity.name();
        checkCollection(collectionName);
        JsonObject jsonObject = ArangoDBUtil.toJsonObject(entity);
        String key = extractKey(entity).orElseThrow(() ->
                new IllegalArgumentException("To update an entity is necessary to have either " + KEY + " or " + ID));
        DocumentUpdateEntity<Void> arangoDocument = db
                .collection(collectionName).updateDocument(key, jsonObject);
        updateEntity(entity, arangoDocument.getKey(), arangoDocument.getId(), arangoDocument.getRev());
        return entity;
    }


    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(DeleteQuery query) {
        requireNonNull(query, "query is required");
        try {
            checkCollection(query.name());
            if (query.condition().isEmpty()) {
                AQLQueryResult delete = QueryAQLConverter.delete(query);
                db.query(delete.query(), Void.class);
                return;
            }

            AQLQueryResult delete = QueryAQLConverter.delete(query);
            db.query(delete.query(), Void.class, delete.values(), null);
        } catch (com.arangodb.ArangoDBException exception) {
            if (ERROR_ARANGO_DATA_SOURCE_NOT_FOUND.equals(exception.getErrorNum())) {
                LOGGER.log(Level.FINEST, exception, () -> "An error to run query, that is related to delete " +
                        "a document collection that does not exist");
            } else {
                throw exception;
            }
        }
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) throws NullPointerException {
        requireNonNull(query, "query is required");
        checkCollection(query.name());
        AQLQueryResult result = QueryAQLConverter.select(query);
        LOGGER.finest("Executing AQL: " + result.query());
        ArangoCursor<JsonObject> documents = db.query(result.query(),
                JsonObject.class,
                result.values(), null);

        return StreamSupport.stream(documents.spliterator(), false)
                .map(ArangoDBUtil::toEntity);
    }

    @Override
    public long count(String documentCollection) {
        requireNonNull(documentCollection, "document collection is required");
        String aql = "RETURN LENGTH(" + documentCollection + ")";
        ArangoCursor<Object> query = db.query(aql, Object.class, emptyMap(), null);
        return StreamSupport.stream(query.spliterator(), false).findFirst().map(Number.class::cast)
                .map(Number::longValue).orElse(0L);
    }


    @Override
    public Stream<CommunicationEntity> aql(String query, Map<String, Object> params) throws NullPointerException {
        requireNonNull(query, "query is required");
        requireNonNull(params, "values is required");
        ArangoCursor<JsonObject> result = db.query(query, JsonObject.class, params, null);
        return StreamSupport.stream(result.spliterator(), false)
                .map(ArangoDBUtil::toEntity);
    }

    @Override
    public <T> Stream<T> aql(String query, Map<String, Object> params, Class<T> type) {
        requireNonNull(query, "query is required");
        requireNonNull(params, "values is required");
        requireNonNull(type, "typeClass is required");
        ArangoCursor<T> result = db.query(query, type, params, null);
        return StreamSupport.stream(result.spliterator(), false);
    }

    @Override
    public <T> Stream<T> aql(String query, Class<T> type) {
        requireNonNull(query, "query is required");
        requireNonNull(type, "typeClass is required");
        return db.query(query, type, emptyMap(), null).stream();
    }

    @Override
    public void close() {
        db.arango().shutdown();
    }


    private void checkCollection(String collectionName) {
        ArangoDBUtil.checkCollection(db.name(), db.arango(), collectionName);
    }

    private void checkEdgeCollection(String collectionName) {
        ArangoDBUtil.checkEdgeCollection(db.name(), db.arango(), collectionName);
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("TTL is not supported on ArangoDB implementation");
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
        requireNonNull(entities, "entities is required");
        requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> this.insert(e, ttl))
                .collect(Collectors.toList());
    }

    @Override
    public ArangoDB getArangoDB() {
        return db.arango();
    }

    private void updateEntity(CommunicationEntity entity, String key, String id, String rev) {
        entity.add(Element.of(KEY, key));
        entity.add(Element.of(ID, id));
        entity.add(Element.of(REV, rev));
    }

    @Override
    public CommunicationEdge edge(CommunicationEntity source, String label, CommunicationEntity target, Map<String, Object> properties) {
        requireNonNull(source, "Source entity is required");
        requireNonNull(target, "Target entity is required");
        requireNonNull(label, "Relationship type is required");
        requireNonNull(properties, "Properties map is required");

        checkCollection(source.name());
        checkCollection(target.name());
        checkEdgeCollection(label);

        source = ensureEntityExists(source);
        target = ensureEntityExists(target);

        CommunicationEntity entity = CommunicationEntity.of(label);
        entity.add(FROM, extractId(source).orElseThrow());
        entity.add(TO, extractId(target).orElseThrow());
        properties.forEach(entity::add);

        JsonObject jsonObject = ArangoDBUtil.toJsonObject(entity);
        String id = db.collection(label).insertDocument(jsonObject).getId();
        return new ArangoDBCommunicationEdge(id, source, target, label, properties);
    }

    private CommunicationEntity ensureEntityExists(CommunicationEntity entity) {
        return extractKey(entity)
                .filter(key -> db.collection(entity.name()).documentExists(key))
                .map(id -> entity)
                .orElseGet(() -> insert(entity));
    }

    @Override
    public void remove(CommunicationEntity source, String label, CommunicationEntity target) {
        Objects.requireNonNull(source, "Source entity is required");
        Objects.requireNonNull(target, "Target entity is required");
        Objects.requireNonNull(label, "Relationship type is required");

        String sourceId = extractId(source).orElseThrow();
        String targetId = extractId(target).orElseThrow();

        db.query("""
                FOR e IN @@collection
                FILTER e._from == @source AND e._to == @target
                REMOVE e IN @@collection
                """, Void.class, Map.of(
                "@collection", label,
                "source", sourceId,
                "target", targetId
        ));
    }

    @Override
    public <K> void deleteEdge(K id) {
        if (!(id instanceof String idString)) {
            throw new IllegalArgumentException("The id must be a String");
        }
        var elements = idString.split("/");
        if (elements.length != 2) {
            throw new IllegalArgumentException("The id must be in the format collection/key");
        }
        String collection = elements[0];
        String key = elements[1];
        String query = """
                FOR e IN @@collection
                FILTER e._key == @key
                REMOVE e IN @@collection
                """;
        var bindVars = Map.of(
                "@collection", collection,
                "key", key);
        db.query(query, Void.class, bindVars);
    }

    @Override
    public <K> Optional<CommunicationEdge> findEdgeById(K id) {
        if (!(id instanceof String idString)) {
            throw new IllegalArgumentException("The id must be a String");
        }
        var elements = idString.split("/");
        if (elements.length != 2) {
            throw new IllegalArgumentException("The id must be in the format collection/key");
        }
        String collection = elements[0];
        String key = elements[1];
        String query = """
                FOR e IN @@collection
                FILTER e._key == @key
                RETURN {
                  edge: e,
                  source: DOCUMENT(e._from),
                  target: DOCUMENT(e._to)
                }
                """;
        var bindVars = Map.of(
                "@collection", collection,
                "key", key);
        return db.query(query, JsonObject.class, bindVars)
                .stream()
                .findFirst()
                .map(it-> ArangoDBUtil.toEdge(
                        it.getJsonObject("edge"),
                        it.getJsonObject("source"),
                        it.getJsonObject("target")));
    }

    private Optional<String> extractId(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        Objects.requireNonNull(entity.name(), "entity name is required");
        if (entity.name().isEmpty()) {
            throw new IllegalArgumentException("entity name cannot be empty");
        }
        return extractKey(entity).map(key -> entity.name() + "/" + key);
    }

    private Optional<String> extractKey(CommunicationEntity entity) {
        return entity.find(KEY, String.class).or(() ->
                entity.find(ID, String.class)
                        .map(id -> {
                            var elements = id.split("/");
                            if (elements.length == 2) {
                                return elements[1];
                            } else {
                                return elements[0];
                            }
                        }));
    }

}
