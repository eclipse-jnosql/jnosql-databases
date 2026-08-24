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
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class CouchbaseDocumentConfigurationTest {


    @Test
    public void shouldCreateDocumentManagerFactoryByFile() {
        CouchbaseDocumentConfiguration configuration = new CouchbaseDocumentConfiguration();
        CouchbaseDocumentManagerFactory managerFactory = configuration.apply(CouchbaseUtil.getSettings());
        assertNotNull(managerFactory);
    }

    @Test
    public void shouldGetConfiguration() {
        DatabaseConfiguration configuration = DatabaseConfiguration.getConfiguration();
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof CouchbaseDocumentConfiguration);
    }

    @Test
    public void shouldGetConfigurationFromQuery() {
        CouchbaseDocumentConfiguration configuration = DatabaseConfiguration
                .getConfiguration(CouchbaseDocumentConfiguration.class);
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof CouchbaseDocumentConfiguration);
    }

}