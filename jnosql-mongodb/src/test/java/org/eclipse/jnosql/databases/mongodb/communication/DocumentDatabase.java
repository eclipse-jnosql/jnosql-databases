/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.mongodb.communication;


import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.eclipse.jnosql.communication.Settings;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.HashMap;
import java.util.Map;


public enum DocumentDatabase {

    INSTANCE;

    private final GenericContainer<?> mongodb =
            new GenericContainer<>("mongo:latest")
                    .withExposedPorts(27017)
                    .waitingFor(Wait.defaultWaitStrategy());

    {
        mongodb.start();
    }

    public MongoClient mongoClient() {
        return MongoClients.create(new ConnectionString("mongodb://" + host()));
    }

    public MongoDBDocumentManager get(String database) {
        Settings settings = getSettings();
        MongoDBDocumentConfiguration configuration = new MongoDBDocumentConfiguration();
        MongoDBDocumentManagerFactory factory = configuration.apply(settings);
        return factory.apply(database);
    }


    private Settings getSettings() {
        Map<String,Object> settings = new HashMap<>();
        settings.put(MongoDBDocumentConfigurations.HOST.get()+".1", host());
        return Settings.of(settings);
    }

    public String host() {
        return mongodb.getHost() + ":" + mongodb.getFirstMappedPort();
    }

}
