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

import org.eclipse.jnosql.communication.graph.GraphDatabaseManager;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;

import java.util.Map;
import java.util.stream.Stream;

/**
 * This specialization of {@link DatabaseManager} is designed for Neo4j databases.
 *
 * <p>Neo4j does not natively support Time-To-Live (TTL) operations, but TTL can be managed
 * using the APOC library. Implementations of this interface should handle TTL-related methods
 * accordingly—either by integrating APOC's TTL features or throwing {@link UnsupportedOperationException}
 * if TTL is not supported.</p>
 * <p>
 * All write operations, including {@code insert} and {@code update}, will be executed within a transaction.
 * When performing batch inserts using an iterable, the entire operation will be executed as a single transaction
 * to ensure consistency.
 *
 * <pre>
 * Neo4JDatabaseManager manager = ...; // Obtain an instance
 * CommunicationEntity entity = CommunicationEntity.of("User");
 * entity.add("name", "Alice");
 * entity.add("age", 30);
 *
 * manager.insert(entity); // Insert into Neo4j
 * </pre>
 * <p>
 * Ensure proper transaction and session management when implementing this interface.
 * Unsupported TTL operations should result in an {@link UnsupportedOperationException}.
 *
 * @see DatabaseManager
 */
public interface Neo4JDatabaseManager extends GraphDatabaseManager {

    /**
     * Executes a custom Cypher query with parameters and returns a stream of {@link CommunicationEntity}.
     *
     * @param cypher     the Cypher query to execute.
     * @param parameters the parameters to bind to the query.
     * @return a stream of {@link CommunicationEntity} representing the query result.
     * @throws NullPointerException if {@code cypher} or {@code parameters} is null.
     */
    Stream<CommunicationEntity> executeQuery(String cypher, Map<String, Object> parameters);

    /**
     * Traverses the graph starting from a node and follows the specified label type up to a given depth.
     *
     * @param startNodeId  the ID of the starting node.
     * @param label the type of label to traverse.
     * @param depth        the traversal depth limit.
     * @return a stream of {@link CommunicationEntity} representing related nodes.
     * @throws NullPointerException if {@code startNodeId}, {@code label}, or {@code depth} is null.
     */
    Stream<CommunicationEntity> traverse(String startNodeId, String label, int depth);

    /**
     * Creates a relationship (edge) between two {@link CommunicationEntity} nodes.
     *
     * @param source           the source entity.
     * @param target           the target entity.
     * @param label the type of relationship to create.
     * @throws EdgeCommunicationException if either the source or target entity does not exist in the database.
     * @throws NullPointerException       if {@code source}, {@code target}, or {@code label} is null.
     */
    void edge(CommunicationEntity source, String label, CommunicationEntity target);

    /**
     * Removes an existing relationship (edge) between two {@link CommunicationEntity} nodes.
     *
     * @param source           the source entity, which must already exist in the database.
     * @param target           the target entity, which must already exist in the database.
     * @param label the type of relationship to remove.
     * @throws EdgeCommunicationException if either the source or target entity does not exist in the database.
     * @throws NullPointerException       if {@code source}, {@code target}, or {@code label} is null.
     */
    void remove(CommunicationEntity source, String label, CommunicationEntity target);
}
