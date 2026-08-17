/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
 *   Maximillian Arruda
 */

package org.eclipse.jnosql.databases.orientdb.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class OrientDBDocumentConfigurationTest {

    @Test
    public void shouldCreateDocumentManagerFactoryForRemoteDB() {
        OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
        configuration.setHost("remote:172.17.0.2");
        configuration.setUser("root");
        configuration.setPassword("rootpwd");
        var managerFactory = configuration.apply(Settings.builder().build());
        assertThat(managerFactory).isNotNull();
    }

    @Test
    public void shouldCreateDocumentManagerFactoryForEmbeddedDB() {
        OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
        configuration.setHost("embedded:/tmp/db/");
        configuration.setUser("root");
        configuration.setPassword("rootpwd");
        var managerFactory = configuration.apply(Settings.builder().build());
        assertThat(managerFactory).isNotNull();
    }

    @Test
    public void shouldThrowExceptionWhenURLIsNotSupported() {

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(()-> {
            OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
            configuration.setHost("172.17.0.2");
            configuration.setUser("root");
            configuration.setPassword("rootpwd");
            configuration.apply(Settings.builder().build());
        });

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(()-> {
            OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
            configuration.setHost("/tmp/db/");
            configuration.setUser("root");
            configuration.setPassword("rootpwd");
            configuration.apply(Settings.builder().build());
        });
    }


    @Test
    public void shouldThrowExceptionWhenSettingsIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> new OrientDBDocumentConfiguration().apply(null));
    }

    @Test
    public void shouldReturnFromConfiguration() {
        var configuration = DatabaseConfiguration.getConfiguration();
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof DatabaseConfiguration).isTrue();
    }

    @Test
    public void shouldReturnFromConfigurationQuery() {
        OrientDBDocumentConfiguration configuration = DatabaseConfiguration
                .getConfiguration(OrientDBDocumentConfiguration.class);
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof OrientDBDocumentConfiguration).isTrue();
    }
}
