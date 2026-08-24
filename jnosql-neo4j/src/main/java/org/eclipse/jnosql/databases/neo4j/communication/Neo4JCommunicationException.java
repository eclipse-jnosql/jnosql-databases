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

import org.eclipse.jnosql.communication.CommunicationException;

/**
 * Exception representing general communication errors when interacting with a Neo4j database.
 * This exception is used as a base for more specific exceptions related to Neo4j operations,
 * such as Cypher syntax errors, transaction failures, or connectivity issues.
 *
 * <ul>
 *     <li>Invalid Cypher query syntax.</li>
 *     <li>Transaction failures during read or write operations.</li>
 *     <li>Connection timeouts or authentication failures.</li>
 *     <li>Inconsistent data constraints within the graph.</li>
 * </ul>
 */
public class Neo4JCommunicationException extends CommunicationException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public Neo4JCommunicationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param exception the cause
     */
    public Neo4JCommunicationException(String message, Throwable exception) {
        super(message, exception);
    }
}
