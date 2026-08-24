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

package org.eclipse.jnosql.databases.cassandra.communication;


import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class CassandraConfigurationTest {

    @Test
    public void shouldCreateDocumentEntityManagerFactoryFromSettings() {
        Settings settings = ColumnDatabase.INSTANCE.getSettings();
        CassandraConfiguration cassandraConfiguration = new CassandraConfiguration();
        var entityManagerFactory = cassandraConfiguration.apply(settings);
        assertNotNull(entityManagerFactory);
    }

    @Test
    public void shouldCreateDocumentEntityManagerFactoryFromFile() {
        Settings settings = ColumnDatabase.INSTANCE.getSettings();
        CassandraConfiguration cassandraConfiguration = new CassandraConfiguration();
        var entityManagerFactory = cassandraConfiguration.apply(settings);
        assertNotNull(entityManagerFactory);
    }

    @Test
    public void shouldCreateConfiguration() {
        var configuration = DatabaseConfiguration.getConfiguration();
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue( configuration instanceof DatabaseConfiguration);
    }

    @Test
    public void shouldCreateConfigurationQuery() {
        CassandraConfiguration configuration = DatabaseConfiguration.getConfiguration(CassandraConfiguration.class);
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue( configuration instanceof CassandraConfiguration);
    }

}
