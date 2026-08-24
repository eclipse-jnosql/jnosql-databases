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

import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MongoDBDocumentConfigurationTest {

    @Test
    void shouldCreateDocumentManagerFactoryByMap() {
        Map<String, String> map = new HashMap<>();
        map.put("mongodb-server-host-1", "172.17.0.2:27017");
        MongoDBDocumentConfiguration configuration = new MongoDBDocumentConfiguration();
        var managerFactory = configuration.get(map);
        assertNotNull(managerFactory);
    }


    @Test
    void shouldReturnErrorWhenSettingsIsNull() {
        var configuration = new MongoDBDocumentConfiguration();
        assertThrows(NullPointerException.class, () -> configuration.apply(null));
    }

    @Test
    void shouldReturnErrorWhenMapSettingsIsNull() {
        MongoDBDocumentConfiguration configuration = new MongoDBDocumentConfiguration();
        assertThrows(NullPointerException.class, () -> configuration.get((Map) null));
    }

    @Test
    void shouldReturnFromConfiguration() {
        var configuration = DatabaseConfiguration.getConfiguration();
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof DatabaseConfiguration);
    }

    @Test
    void shouldReturnFromConfigurationQuery() {
        MongoDBDocumentConfiguration configuration = DatabaseConfiguration
                .getConfiguration(MongoDBDocumentConfiguration.class);
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof MongoDBDocumentConfiguration);
    }

}
