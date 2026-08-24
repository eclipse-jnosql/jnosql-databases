/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
 */

package org.eclipse.jnosql.databases.neo4j.communication;

import org.eclipse.jnosql.communication.Settings;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

public enum DatabaseContainer {

    INSTANCE;

    private final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>(DockerImageName.parse("neo4j:5.26.3"))
            .withoutAuthentication();

    {
        neo4jContainer.start();
    }


    public String host() {
        return neo4jContainer.getBoltUrl();
    }
    public Neo4JDatabaseManager get(String database) {
        Objects.requireNonNull(database, "database is required");
        Settings settings = Settings.builder().put(Neo4JConfigurations.URI, neo4jContainer.getBoltUrl()).build();
        var configuration = new Neo4JConfiguration();
        var managerFactory = configuration.apply(settings);
        return managerFactory.apply(database);
    }

}
