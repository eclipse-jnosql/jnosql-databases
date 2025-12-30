/*
 *
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 *
 */
package org.eclipse.jnosql.databases.neo4j.communication;

import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.graph.CommunicationEdge;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.TypeSystem;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Stream;

class DefaultNeo4JDatabaseManager implements Neo4JDatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DefaultNeo4JDatabaseManager.class.getName());
    public static final String ID = "_id";

    private final Session session;
    private final String database;

    public DefaultNeo4JDatabaseManager(Session session, String database) {
        this.session = session;
        this.database = database;
    }

    @Override
    public String name() {
        return database;
    }

    public CommunicationEntity insert(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");
        return insertEntities(Collections.singletonList(entity)).iterator().next();
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        return insertEntities(entities);
    }

    @Override
    public Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl) {
       throw new UnsupportedOperationException("This operation is not supported in Neo4J");
    }

    @Override
    public CommunicationEntity insert(CommunicationEntity entity, Duration ttl) {
        throw new UnsupportedOperationException("This operation is not supported in Neo4J");
    }

    @Override
    public CommunicationEntity update(CommunicationEntity entity) {
        Objects.requireNonNull(entity, "entity is required");

        if (!entity.contains(ID)) {
            throw new Neo4JCommunicationException("Cannot update entity without an _id field, entity: " + entity);
        }

        Map<String, Object> entityMap = entity.toMap();
        StringBuilder cypher = new StringBuilder("MATCH (e) WHERE elementId(e) = $elementId SET ");

        entityMap.entrySet().stream()
                .filter(entry -> !ID.equals(entry.getKey()))
                .forEach(entry -> cypher.append("e.").append(entry.getKey()).append(" = $").append(entry.getKey()).append(", "));

        if (cypher.toString().endsWith(", ")) {
            cypher.setLength(cypher.length() - 2);
        }

        LOGGER.finest(() -> "Executing Cypher query to update entity: " + cypher);

        try (Transaction tx = session.beginTransaction()) {
            var elementId = entity.find(ID)
                    .orElseThrow(() -> new CommunicationException("Entity must have an ID"))
                    .get(String.class);

            Map<String, Object> params = new HashMap<>(entityMap);
            params.put("elementId", elementId);

            tx.run(cypher.toString(), Values.parameters(flattenMap(params)));
            tx.commit();
        }

        LOGGER.fine("Updated entity: " + entity.name() + " with elementId: " + entity.find(ID).orElseThrow().get(String.class));
        return entity;
    }


    @Override
    public Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities) {
        Objects.requireNonNull(entities, "entities is required");
        entities.forEach(this::update);
        return entities;
    }

    @Override
    public Iterable<CommunicationEntity> update(UpdateQuery query) {
        Objects.requireNonNull(query, "query is required");
        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        LOGGER.fine("Executing Cypher Query for select entities: " + cypher);
        try (Transaction tx = session.beginTransaction()) {
            return tx.run(cypher, Values.parameters(flattenMap(parameters)))
                    .list(record -> extractEntity(query.name(), record, true))
                    .stream().toList();
        }
    }

    @Override
    public void delete(DeleteQuery query) {
        Objects.requireNonNull(query, "query is required");
        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        LOGGER.fine("Executing Delete Cypher Query: " + cypher);
        try (Transaction tx = session.beginTransaction()) {
            tx.run(cypher, Values.parameters(flattenMap(parameters)));
            tx.commit();
        }
    }

    @Override
    public Stream<CommunicationEntity> select(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildQuery(query, parameters);

        LOGGER.fine("Executing Cypher Query for select entities: " + cypher);
        try (Transaction tx = session.beginTransaction()) {
            return tx.run(cypher, Values.parameters(flattenMap(parameters)))
                    .list(record -> extractEntity(query.name(), record, query.columns().isEmpty()))
                    .stream();
        }
    }

    public long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        Map<String, Object> parameters = new HashMap<>();
        String cypher = Neo4JQueryBuilder.INSTANCE.buildCountQuery(query, parameters);

        LOGGER.fine("Executing Cypher Query for counting entities: " + cypher);
        try (Transaction tx = session.beginTransaction()) {
            long count = tx.run(cypher, Values.parameters(flattenMap(parameters)))
                    .single()
                    .get("count")
                    .asLong();
            tx.commit();
            return count;
        } catch (Exception e) {
            LOGGER.severe("Error executing count query: " + e.getMessage());
            throw new CommunicationException("Error executing count query", e);
        }
    }

    @Override
    public long count(String entity) {
        Objects.requireNonNull(entity, "entity is required");
        try (Transaction tx = session.beginTransaction()) {
            String cypher = "MATCH (e:" + entity + ") RETURN count(e) AS count";
            LOGGER.fine("Executing Cypher Query for counting: " + cypher);
            long count = tx.run(cypher).single().get("count").asLong();
            tx.commit();
            return count;
        } catch (Exception e) {
            LOGGER.severe("Error executing count query: " + e.getMessage());
           throw new CommunicationException("Error executing count query", e);
        }
    }

    @Override
    public Stream<CommunicationEntity> cypher(String cypher, Map<String, Object> parameters) {
        Objects.requireNonNull(cypher, "Cypher query is required");
        Objects.requireNonNull(parameters, "Parameters map is required");

        try (Transaction tx = session.beginTransaction()) {
            var result = tx.run(cypher, Values.parameters(flattenMap(parameters)));

            List<CommunicationEntity> entities = result
                    .stream()
                    .map(record -> record.keys().stream()
                            .map(key -> {
                                var value = record.get(key);
                                if (value.hasType(TypeSystem.getDefault().NODE())) {
                                    return extractEntity(key, record, false);
                                } else if (value.hasType(TypeSystem.getDefault().RELATIONSHIP())) {
                                    var rel = value.asRelationship();
                                    List<Element> elements = new ArrayList<>();
                                    rel.asMap().forEach((k, v) -> elements.add(Element.of(k, v)));
                                    elements.add(Element.of(ID, rel.elementId()));
                                    elements.add(Element.of("start", rel.startNodeElementId()));
                                    elements.add(Element.of("end", rel.endNodeElementId()));
                                    return CommunicationEntity.of(key, elements);
                                }
                                return null;
                            })
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null)
                    )
                    .filter(Objects::nonNull)
                    .toList();
            LOGGER.fine("Executed Cypher query: " + cypher);
            tx.commit();
            return entities.stream();
        } catch (Exception e) {
            throw new CommunicationException("Error executing Cypher query", e);
        }
    }

    @Override
    public Stream<CommunicationEntity> cypher(String cypher) {
        return cypher(cypher, Collections.emptyMap());
    }

    @Override
    public Stream<CommunicationEntity> traverse(String startNodeId, String label, int depth) {
        Objects.requireNonNull(startNodeId, "Start node ID is required");
        Objects.requireNonNull(label, "Relationship type is required");

        String cypher = "MATCH (startNode) WHERE elementId(startNode) = $elementId " +
                "MATCH (startNode)-[r:" + label + "*1.." + depth + "]-(endNode) " +
                "RETURN endNode";

        try (Transaction tx = session.beginTransaction()) {
            Stream<CommunicationEntity> result = tx.run(cypher, Values.parameters("elementId", startNodeId))
                    .list(record -> extractEntity("TraversalResult", record, false))
                    .stream();
            LOGGER.fine("Executed traversal query: " + cypher);
            tx.commit();
            return result;
        }
    }

    @Override
    public void edge(CommunicationEntity source, String label, CommunicationEntity target) {
        Objects.requireNonNull(source, "Source entity is required");
        Objects.requireNonNull(target, "Target entity is required");
        Objects.requireNonNull(label, "Relationship type is required");

        String cypher = "MATCH (s) WHERE elementId(s) = $sourceElementId " +
                        "MATCH (t) WHERE elementId(t) = $targetElementId " +
                        "WITH s, t " +
                        "WHERE NOT EXISTS { MATCH (s)-[r:" + label + "]->(t) } " +
                        "CREATE (s)-[r:" + label + "]->(t)";

        try (Transaction tx = session.beginTransaction()) {
            var sourceId = source.find(ID).orElseThrow(() ->
                    new EdgeCommunicationException("The source entity should have the " + ID + " property")).get();
            var targetId = target.find(ID).orElseThrow(() ->
                    new EdgeCommunicationException("The target entity should have the " + ID + " property")).get();

            tx.run(cypher, Values.parameters(
                    "sourceElementId", sourceId,
                    "targetElementId", targetId
            ));

            LOGGER.fine("Created edge: " + cypher);
            tx.commit();
        }
    }

    @Override
    public void remove(CommunicationEntity source, String label, CommunicationEntity target) {
        Objects.requireNonNull(source, "Source entity is required");
        Objects.requireNonNull(target, "Target entity is required");
        Objects.requireNonNull(label, "Relationship type is required");

        String cypher = "MATCH (s) WHERE elementId(s) = $sourceElementId " +
                "MATCH (t) WHERE elementId(t) = $targetElementId " +
                "MATCH (s)-[r:" + label + "]-(t) DELETE r";

        var sourceId = source.find(ID).orElseThrow(() ->
                new EdgeCommunicationException("The source entity should have the " + ID + " property")).get();
        var targetId = target.find(ID).orElseThrow(() ->
                new EdgeCommunicationException("The target entity should have the " + ID + " property")).get();

        try (Transaction tx = session.beginTransaction()) {
            tx.run(cypher, Values.parameters(
                    "sourceElementId", sourceId,
                    "targetElementId", targetId
            ));
            LOGGER.fine("Removed edge: " + cypher);
            tx.commit();
        }
    }

    @Override
    public <K> void deleteEdge(K id) {
        Objects.requireNonNull(id, "The id is required");
        LOGGER.fine(() -> "Deleting edge with ID: " + id);
        String cypher = "MATCH ()-[r]-() WHERE elementId(r) = $elementId DELETE r";

        try (Transaction tx = session.beginTransaction()) {
            tx.run(cypher, Values.parameters("elementId", id));
            LOGGER.fine(() -> "Deleted edge with ID: " + id);
            tx.commit();
        }
    }

    @Override
    public <K> Optional<CommunicationEdge> findEdgeById(K id) {
        Objects.requireNonNull(id, "The edge ID is required");

        String cypher = "MATCH (s)-[r]->(t) WHERE elementId(r) = $edgeId RETURN r, s, t";
        LOGGER.fine(() -> "Find edge with ID: " + id);
        try (Transaction tx = session.beginTransaction()) {
            var result = tx.run(cypher, Values.parameters("edgeId", id));
            if (result.hasNext()) {
                LOGGER.fine(() -> "Found edge with ID: " + id);
                var record = result.next();
                var relationship = record.get("r").asRelationship();

                var sourceNode = record.get("s").asNode();
                var targetNode = record.get("t").asNode();
                var sourceEntity = extractEntity(sourceNode.labels().iterator().next(), record, true);
                var targetEntity = extractEntity(targetNode.labels().iterator().next(), record, true);
                Map<String, Object> properties = relationship.asMap();
                return Optional.of(new Neo4jCommunicationEdge(relationship.elementId(), sourceEntity, targetEntity, relationship.type(), properties));
            }
        }
        return Optional.empty();
    }

    @Override
    public CommunicationEdge edge(CommunicationEntity source, String label, CommunicationEntity target, Map<String, Object> properties) {
        Objects.requireNonNull(source, "Source entity is required");
        Objects.requireNonNull(target, "Target entity is required");
        Objects.requireNonNull(label, "Relationship type is required");
        Objects.requireNonNull(properties, "Properties map is required");

        source = ensureEntityExists(source);
        target = ensureEntityExists(target);

        var sourceId = source.find(ID).orElseThrow(() ->
                new EdgeCommunicationException("The source entity should have the " + ID + " property")).get();
        var targetId = target.find(ID).orElseThrow(() ->
                new EdgeCommunicationException("The target entity should have the " + ID + " property")).get();

        try (Transaction tx = session.beginTransaction()) {

            String findEdge = "MATCH (s) WHERE elementId(s) = $sourceElementId " +
                    "MATCH (t) WHERE elementId(t) = $targetElementId " +
                    "MATCH (s)-[r:" + label + "]->(t) RETURN r";

            LOGGER.fine(() -> "Finding existing edge with ID: " + sourceId + " to " + targetId);
            LOGGER.fine(() -> "Cypher Query: " + findEdge);
            var result = tx.run(findEdge, Values.parameters(
                    "sourceElementId", sourceId,
                    "targetElementId", targetId
            ));

            org.neo4j.driver.types.Relationship relationship;

            if (result.hasNext()) {
                String updateQuery = "MATCH (s)-[r:" + label + "]->(t) " +
                        "WHERE elementId(s) = $sourceElementId AND elementId(t) = $targetElementId " +
                        "SET r += $props " +
                        "RETURN r";
                LOGGER.fine(() -> "Updating existing edge with ID: " + sourceId + " to " + targetId);
                LOGGER.fine(() -> "Cypher Query: " + updateQuery);
                var updateResult = tx.run(updateQuery, Values.parameters(
                        "sourceElementId", sourceId,
                        "targetElementId", targetId,
                        "props", properties
                ));

                relationship = updateResult.single().get("r").asRelationship();
                LOGGER.fine(() -> "Found existing edge with ID: " + relationship.elementId());
            } else {
                String createEdge = "MATCH (s) WHERE elementId(s) = $sourceElementId " +
                        "MATCH (t) WHERE elementId(t) = $targetElementId " +
                        "CREATE (s)-[r:" + label + " $props]->(t) RETURN r";

                LOGGER.fine(() -> "Creating new edge with ID: " + sourceId + " to " + targetId);
                LOGGER.fine(() -> "Cypher Query: " + createEdge);
                var createResult = tx.run(createEdge, Values.parameters(
                        "sourceElementId", sourceId,
                        "targetElementId", targetId,
                        "props", properties
                ));
                relationship = createResult.single().get("r").asRelationship();
                LOGGER.fine(() -> "Created new edge with ID: " + relationship.elementId());
            }
            tx.commit();
            return new Neo4jCommunicationEdge(relationship.elementId(), source, target, label, properties);
        }
    }

    private CommunicationEntity ensureEntityExists(CommunicationEntity entity) {
        return entity.find(ID).filter(this::entityExists).map(id -> entity).orElseGet(() -> insert(entity));
    }

    private boolean entityExists(Element id) {
        String cypher = "MATCH (e) WHERE elementId(e) = $id RETURN count(e) > 0 AS exists";
        try (Transaction tx = session.beginTransaction()) {
            var result = tx.run(cypher, Values.parameters("id", id.get()));
            var exists =  result.single().get("exists").asBoolean();
            LOGGER.fine(() -> "Checking if entity exists with ID: " + id + " result: " + exists);
            return exists;
        }
    }

    @Override
    public void close() {
        LOGGER.fine("Closing the Neo4J session, the database name is: " + database);
        this.session.close();
    }

    private Object[] flattenMap(Map<String, Object> map) {
        return map.entrySet().stream()
                .flatMap(entry -> java.util.stream.Stream.of(entry.getKey(), entry.getValue()))
                .toArray();
    }

    private Iterable<CommunicationEntity> insertEntities(Iterable<CommunicationEntity> entities) {
        List<CommunicationEntity> entitiesResult = new ArrayList<>();
        try (Transaction tx = session.beginTransaction()) {
            for (CommunicationEntity entity : entities) {

                Map<String, Object> properties = entity.toMap();
                StringBuilder cypher = new StringBuilder("CREATE (e:");
                cypher.append(entity.name()).append(" {");

                properties.keySet().forEach(key -> cypher.append(key).append(": $").append(key).append(", "));

                if (!properties.isEmpty()) {
                    cypher.setLength(cypher.length() - 2);
                }
                cypher.append("}) RETURN e");
                LOGGER.fine("Executing Cypher Query to insert entities: " + cypher);

                var result = tx.run(cypher.toString(), Values.parameters(flattenMap(properties)));
                var record = result.hasNext() ? result.next() : null;
                var insertedNode = record.get("e").asNode();
                entity.add(ID, insertedNode.elementId());
                entitiesResult.add(entity);
            }
            tx.commit();
        }
        LOGGER.fine("Inserted entities: " + entitiesResult.size());
        return entitiesResult;
    }

    private CommunicationEntity extractEntity(String alias, org.neo4j.driver.Record record, boolean isFullNode) {
        List<Element> elements = new ArrayList<>();

        for (String key : record.keys()) {
            var value = record.get(key);

            if (value.hasType(TypeSystem.getDefault().NODE())) {
                var node = value.asNode();

                node.asMap().forEach((k, v) -> elements.add(Element.of(k, v)));

                elements.add(Element.of(ID, node.elementId()));
                elements.add(Element.of("_alias", key));

                var label = node.labels().iterator().hasNext()
                        ? node.labels().iterator().next()
                        : key;

                return CommunicationEntity.of(label, elements);
            }

            if (value.hasType(TypeSystem.getDefault().RELATIONSHIP())) {
                var rel = value.asRelationship();

                rel.asMap().forEach((k, v) -> elements.add(Element.of(k, v)));

                elements.add(Element.of(ID, rel.elementId()));
                elements.add(Element.of("start", rel.startNodeElementId()));
                elements.add(Element.of("end", rel.endNodeElementId()));
                elements.add(Element.of("_alias", key));

                return CommunicationEntity.of(rel.type(), elements);
            }

            String fieldName = key.contains(".") ? key.substring(key.indexOf('.') + 1) : key;
            elements.add(Element.of(fieldName, value.asObject()));
        }

        // No node or relationship found: use alias as fallback
        return CommunicationEntity.of(alias, elements);
    }
}
