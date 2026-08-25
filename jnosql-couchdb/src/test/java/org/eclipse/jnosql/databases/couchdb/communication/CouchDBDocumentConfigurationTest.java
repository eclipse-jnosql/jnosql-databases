/*
 *
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class CouchDBDocumentConfigurationTest {

    @Test
    public void shouldCreateDocumentManagerFactoryByMap() {
        CouchDBDocumentConfiguration configuration = new CouchDBDocumentConfiguration();
        var managerFactory = configuration.apply(Settings.settings());
        assertThat(managerFactory).isNotNull();
    }

    @Test
    public void shouldCreateDocumentManagerFactoryByFile() {
        CouchDBDocumentConfiguration configuration = new CouchDBDocumentConfiguration();
        var managerFactory = configuration.apply(Settings.settings());
        assertThat(managerFactory).isNotNull();
    }

    @Test
    public void shouldReturnFromConfiguration() {
        CouchDBDocumentConfiguration configuration = DatabaseConfiguration.getConfiguration();
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof CouchDBDocumentConfiguration).isTrue();
    }

    @Test
    public void shouldReturnFromConfigurationQuery() {
        CouchDBDocumentConfiguration configuration = DatabaseConfiguration
                .getConfiguration(CouchDBDocumentConfiguration.class);
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof CouchDBDocumentConfiguration).isTrue();
    }

}