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

import com.mongodb.client.MongoClient;
import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MongoDBDocumentManagerFactoryTest {

    private static MongoDBDocumentConfiguration configuration;

    @BeforeAll
    public static void setUp() {
        configuration = new MongoDBDocumentConfiguration();
    }

    @Test
    void shouldCreateEntityManager() {
        MongoDBDocumentManagerFactory mongoDBFactory = configuration.apply(Settings.builder().build());
        assertNotNull(mongoDBFactory.apply("database"));
    }

    @Test
    void shouldReturnNPEWhenSettingsIsNull() {
        assertThrows(NullPointerException.class, () -> configuration.apply((Settings) null));
    }

    @Test
    void shouldReturnNPEWhenMapSettingsIsNull() {
        assertThrows(NullPointerException.class, () -> configuration.get((Map<String, String>) null));
    }

    @Test
    void shouldReturnNPEWhenMongoClientIsNull() {
        assertThrows(NullPointerException.class, () -> configuration.get((MongoClient) null));
    }

}