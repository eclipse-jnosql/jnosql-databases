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
package org.eclipse.jnosql.databases.neo4j.mapping;

import org.eclipse.jnosql.mapping.graph.GraphTemplate;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A template interface for executing queries, traversing relationships,
 * and managing edges in a Neo4J database.
 * This interface provides methods for executing Cypher queries,
 * traversing relationships, and handling edges between entities.
 *
 */
public interface Neo4JTemplate extends GraphTemplate {
    /**
     * Executes a Cypher query and returns a stream of results mapped to the given entity type.
     *
     * @param cypher The Cypher query string.
     * @param parameters The query parameters.
     * @param <T> The entity type representing nodes or relationships within the graph database.
     * @return A stream of entities representing the query result.
     * @throws NullPointerException if {@code cypher} or {@code parameters} is null.
     */
    <T> Stream<T> cypher(String cypher, Map<String, Object> parameters);

    /**
     * Traverses relationships from a given start node up to a specified depth.
     *
     * @param startNodeId The unique identifier of the starting node.
     * @param relationship The relationship type to traverse.
     * @param depth The depth of traversal.
     * @param <T> The entity type representing nodes or relationships within the graph database.
     * @return A stream of entities resulting from the traversal.
     * @throws NullPointerException if {@code startNodeId} or {@code relationship} is null.
     */
    <T> Stream<T> traverse(String startNodeId, String relationship, int depth);

    /**
     * Traverses relationships dynamically using a relationship supplier.
     *
     * @param startNodeId The unique identifier of the starting node.
     * @param relationship A supplier providing the relationship type dynamically.
     * @param depth The depth of traversal.
     * @param <T> The entity type representing nodes or relationships within the graph database.
     * @return A stream of entities resulting from the traversal.
     * @throws NullPointerException if {@code startNodeId} or {@code relationship} is null.
     */
    <T> Stream<T> traverse(String startNodeId, Supplier<String> relationship, int depth);

}
