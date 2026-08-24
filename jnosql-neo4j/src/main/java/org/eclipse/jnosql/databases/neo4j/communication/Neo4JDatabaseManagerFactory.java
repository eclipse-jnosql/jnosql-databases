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
import org.eclipse.jnosql.communication.semistructured.DatabaseManagerFactory;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * This class provides a factory for creating and managing instances of {@link DefaultNeo4JDatabaseManager},
 * enabling communication with a Neo4j database. It implements the {@link DatabaseManagerFactory} interface
 * and handles the lifecycle of the Neo4j {@link Driver}.
 *
 * <p>Main Responsibilities:</p>
 * <ul>
 *     <li>Establishing a connection to a Neo4j database using the provided URI, username, and password.</li>
 *     <li>Creating new instances of {@link DefaultNeo4JDatabaseManager} for a specific database.</li>
 *     <li>Managing the lifecycle of the Neo4j driver, including proper resource cleanup.</li>
 * </ul>
 *>Thread Safety:
 * The factory is thread-safe as long as it is used properly. Ensure to close the factory
 * when it is no longer needed to release resources.
 * At the close method, the factory closes the Neo4j {@link Driver} and releases resources.
 */
public class Neo4JDatabaseManagerFactory implements DatabaseManagerFactory {

    private static final Logger LOGGER = Logger.getLogger(Neo4JDatabaseManagerFactory.class.getName());

    private final Driver driver;

    Neo4JDatabaseManagerFactory(Driver driver) {
        this.driver = Objects.requireNonNull(driver, "driver is required");
    }

    /**
     * Closes the Neo4j driver and releases resources.
     */
    @Override
    public void close() {
        LOGGER.info("Closing the Neo4J driver");
        this.driver.close();
    }

    @Override
    public DefaultNeo4JDatabaseManager apply(String database) {
        Objects.requireNonNull(database, "database is required");
        LOGGER.fine(() -> "Creating a new instance of Neo4JDatabaseManager with the database: " + database);
        boolean databaseExists = databaseExists(database);
        LOGGER.fine(() -> "Database " + database + " exists: " + databaseExists);
        if (!databaseExists) {
            LOGGER.fine(() -> "Database " + database + " does not exist. Creating it...");
            createDatabase(database);
        }
        var session = driver.session(SessionConfig.builder().withDatabase(database).build());
        return new DefaultNeo4JDatabaseManager(session, database);
    }

    static Neo4JDatabaseManagerFactory of(Neo4Property property) {
        Objects.requireNonNull(property, "property is required");
        LOGGER.fine(() -> "Creating a new instance of Neo4JDatabaseManagerFactory with the uri: " + property.uri());
        if(property.user() == null && property.password() == null) {
            LOGGER.fine("Creating a new instance of Neo4JDatabaseManagerFactory without authentication");
            return new Neo4JDatabaseManagerFactory(GraphDatabase.driver(property.uri()));
        }
        var basic = AuthTokens.basic(property.user(), property.password());
        return new Neo4JDatabaseManagerFactory(GraphDatabase.driver(property.uri(), basic));
    }

    private boolean databaseExists(String database) {
        try (Session session = driver.session()) {
            Result result = session.run("SHOW DATABASES");
            while (result.hasNext()) {
                if (database.equals(result.next().get("name").asString())) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Failed to check database existence: " + e.getMessage());
        }
        return false;
    }

    private void createDatabase(String database) {
        try (Session session = driver.session()) {
            session.run("CREATE DATABASE " + database);
            LOGGER.info("Database created: " + database);
        } catch (Exception e) {
            LOGGER.severe("Failed to create database: " + e.getMessage());
            throw new CommunicationException("Could not create database: " + database, e);
        }
    }
}
