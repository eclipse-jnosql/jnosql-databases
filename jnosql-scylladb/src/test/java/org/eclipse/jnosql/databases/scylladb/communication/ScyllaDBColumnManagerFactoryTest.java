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

package org.eclipse.jnosql.databases.scylladb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.SettingsBuilder;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class ScyllaDBColumnManagerFactoryTest {

    private ScyllaDBColumnManagerFactory subject;

    @BeforeEach
    public void setUp() {
        Settings settings = ColumnDatabase.INSTANCE.getSettings();
        SettingsBuilder builder = Settings.builder();
        builder.put(ScyllaDBConfigurations.HOST.get() + ".1", settings.get(ScyllaDBConfigurations.HOST.get() + ".1")
                .get().toString());

        builder.put(ScyllaDBConfigurations.PORT.get(), settings.get(ScyllaDBConfigurations.PORT.get()).get().toString());
        builder.put(ScyllaDBConfigurations.QUERY.get() + ".1", " CREATE KEYSPACE IF NOT EXISTS newKeySpace WITH replication = {'class': 'NetworkTopologyStrategy', 'replication_factor' : 1};");
        ScyllaDBConfiguration scylladbConfiguration = new ScyllaDBConfiguration();
        subject = scylladbConfiguration.apply(builder.build());
    }

    @Test
    public void shouldReturnErrorWhenSettingsIsNull() {
        ScyllaDBConfiguration scylladbConfiguration = new ScyllaDBConfiguration();
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> scylladbConfiguration.apply(null));

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> scylladbConfiguration.apply(null));
    }

    @Test
    public void shouldReturnEntityManager() {
        DatabaseManager columnEntityManager = subject.apply(Constants.KEY_SPACE);
        assertThat(columnEntityManager).isNotNull();
    }

}