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

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;

import java.util.logging.Logger;

/**
 * Class Neo4JConfiguration
 * This class provides the configuration for the Neo4j database using the Eclipse jNoSQL framework.
 * It implements the {@link DatabaseConfiguration} interface to set up a connection to a Neo4j database.
 *
 * <p>The configuration retrieves the following settings from the provided {@link Settings}:
 * <ul>
 *     <li>{@link Neo4JConfigurations#URI}: The connection URI for the Neo4j database (e.g., "bolt://localhost:7687").</li>
 *     <li>{@link Neo4JConfigurations#USERNAME}: The username for authentication (optional).</li>
 *     <li>{@link Neo4JConfigurations#PASSWORD}: The password for authentication (optional).</li>
 * </ul>
 *
 * <p>If no URI is provided, a default URI ("bolt://localhost:7687") is used.
 *
 * <p>Usage example:
 * <pre>
 * Settings settings = Settings.builder()
 *         .put(Neo4JConfigurations.URI, "bolt://custom-host:7687")
 *         .put(Neo4JConfigurations.USERNAME, "neo4j")
 *         .put(Neo4JConfigurations.PASSWORD, "password123")
 *         .build();
 *
 * Neo4JConfiguration configuration = new Neo4JConfiguration();
 * Neo4JDatabaseManagerFactory factory = configuration.apply(settings);
 * </pre>
 */
public final class Neo4JConfiguration implements DatabaseConfiguration {

    private static final Logger LOGGER = Logger.getLogger(Neo4JConfiguration.class.getName());

    private static final String DEFAULT_BOLT = "bolt://localhost:7687";

    /**
     * Applies the provided settings to the Neo4j database configuration.
     *
     * @param settings the settings to apply
     * @return a new {@link Neo4JDatabaseManagerFactory} instance
     */
    @Override
    public Neo4JDatabaseManagerFactory apply(Settings settings) {
        var uri = settings.getOrDefault(Neo4JConfigurations.URI, DEFAULT_BOLT);
        var user = settings.get(Neo4JConfigurations.USERNAME, String.class).orElse(null);
        var password = settings.get(Neo4JConfigurations.PASSWORD, String.class).orElse(null);
        LOGGER.info("Starting configuration to Neo4J database, the uri: " + uri);
        var neo4Property = new Neo4Property(uri, user, password);
        return Neo4JDatabaseManagerFactory.of(neo4Property);
    }
}
