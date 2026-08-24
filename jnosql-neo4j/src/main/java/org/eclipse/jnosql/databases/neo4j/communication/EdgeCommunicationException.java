/*
 *
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 *
 */
package org.eclipse.jnosql.databases.neo4j.communication;

/**
 * Exception thrown when an issue occurs with edge (relationship) operations in Neo4j.
 * This exception is raised in cases where an edge cannot be created or removed
 * due to missing nodes, constraint violations, or other graph-related inconsistencies.
 *
 * <ul>
 *     <li>Attempting to create an edge where either the source or target entity does not exist.</li>
 *     <li>Removing an edge that is not found in the graph.</li>
 *     <li>Trying to enforce an invalid relationship constraint.</li>
 * </ul>
 */
public class EdgeCommunicationException extends Neo4JCommunicationException{

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public EdgeCommunicationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param exception the cause
     */
    public EdgeCommunicationException(String message, Throwable exception) {
        super(message, exception);
    }
}
