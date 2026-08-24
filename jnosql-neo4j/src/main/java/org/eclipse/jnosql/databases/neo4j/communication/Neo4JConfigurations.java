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

import java.util.function.Supplier;

/**
 * Enum Neo4JConfigurations
 * This enum defines the configuration keys used in the Eclipse jNoSQL driver for Neo4j.
 * Each configuration key is associated with a specific property required to connect to a Neo4j database.
 *
 * <ul>
 *   <li>URI: The connection URI for the Neo4j database (e.g., bolt://localhost:7687).</li>
 *   <li>USERNAME: The username used for authentication (e.g., "neo4j").</li>
 *   <li>PASSWORD: The password used for authentication (e.g., "password123").</li>
 * </ul>
 * <p>
 * This enum implements the Supplier interface, providing a method to retrieve the configuration key as a string.
 * Usage example:
 * <pre>
 * String uri = Neo4JConfigurations.URI.get(); // Returns "jnosql.neo4j.uri"
 * String username = Neo4JConfigurations.USERNAME.get(); // Returns "jnosql.neo4j.username"
 * String password = Neo4JConfigurations.PASSWORD.get(); // Returns "jnosql.neo4j.password"
 *
 * // Example of configuring a connection:
 * Settings settings = Settings.builder().put(Neo4JConfigurations.URI, "bolt://localhost:7687")
 * .put(Neo4JConfigurations.USERNAME, "neo4j")
 * .put(Neo4JConfigurations.PASSWORD, "password123")
 * .build();
 * </pre>
 */
public enum Neo4JConfigurations implements Supplier<String> {

    /**
     * The URI of the Neo4j database.
     * Example: bolt://localhost:7687
     */
    URI("jnosql.neo4j.uri"),

    /**
     * The username for authentication.
     * Example: "neo4j"
     */
    USERNAME("jnosql.neo4j.username"),

    /**
     * The password for authentication.
     * Example: "password123"
     */
    PASSWORD("jnosql.neo4j.password"),

    /**
     * The database name.
     * Example: "library"
     */
    DATABASE("jnosql.neo4j.database");

    private final String value;

    Neo4JConfigurations(String value) {
        this.value = value;
    }

    @Override
    public String get() {
        return value;
    }
}
