/*
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
 */
package org.eclipse.jnosql.databases.neo4j.mapping;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.Interceptor;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.databases.neo4j.communication.Neo4JConfiguration;
import org.eclipse.jnosql.databases.neo4j.communication.Neo4JDatabaseManager;
import org.eclipse.jnosql.databases.neo4j.communication.Neo4JDatabaseManagerFactory;
import org.eclipse.jnosql.mapping.core.config.MicroProfileSettings;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@Alternative
@Priority(Interceptor.Priority.LIBRARY_BEFORE)
class GraphManagerSupplier implements Supplier<Neo4JDatabaseManager> {

    private static final String DATABASE_DEFAULT = "neo4j";

    private static final Logger LOGGER = Logger.getLogger(GraphManagerSupplier.class.getName());

    @Override
    @Produces
    @ApplicationScoped
    public Neo4JDatabaseManager get() {
        LOGGER.fine(() -> "Creating a Neo4JDatabaseManager bean");
        Settings settings = MicroProfileSettings.INSTANCE;
        var configuration = new Neo4JConfiguration();
        Neo4JDatabaseManagerFactory managerFactory = configuration.apply(settings);
        var database = settings.getOrDefault("database", DATABASE_DEFAULT);
        LOGGER.fine(() -> "Creating a Neo4JDatabaseManager bean with database: " + database);
        return managerFactory.apply(database);
    }

    public void close(@Disposes Neo4JDatabaseManager manager) {
        LOGGER.log(Level.FINEST, "Closing Neo4JDatabaseManager resource, database name: " + manager.name());
        manager.close();
    }
}
