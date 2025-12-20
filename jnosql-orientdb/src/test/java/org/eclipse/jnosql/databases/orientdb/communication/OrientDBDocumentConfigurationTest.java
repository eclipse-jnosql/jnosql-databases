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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class OrientDBDocumentConfigurationTest {

    @Test
    public void shouldCreateDocumentManagerFactoryForRemoteDB() {
        OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
        configuration.setHost("remote:172.17.0.2");
        configuration.setUser("root");
        configuration.setPassword("rootpwd");
        var managerFactory = configuration.apply(Settings.builder().build());
        assertNotNull(managerFactory);
    }

    @Test
    public void shouldCreateDocumentManagerFactoryForEmbeddedDB() {
        OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
        configuration.setHost("embedded:/tmp/db/");
        configuration.setUser("root");
        configuration.setPassword("rootpwd");
        var managerFactory = configuration.apply(Settings.builder().build());
        assertNotNull(managerFactory);
    }

    @Test
    public void shouldThrowExceptionWhenURLIsNotSupported() {

        assertThrows(IllegalArgumentException.class, ()-> {
            OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
            configuration.setHost("172.17.0.2");
            configuration.setUser("root");
            configuration.setPassword("rootpwd");
            configuration.apply(Settings.builder().build());
            fail("Should throw exception");
        });

        assertThrows(IllegalArgumentException.class, ()-> {
            OrientDBDocumentConfiguration configuration = new OrientDBDocumentConfiguration();
            configuration.setHost("/tmp/db/");
            configuration.setUser("root");
            configuration.setPassword("rootpwd");
            configuration.apply(Settings.builder().build());
            fail("Should throw exception");
        });
    }


    @Test
    public void shouldThrowExceptionWhenSettingsIsNull() {
        assertThrows(NullPointerException.class, () -> new OrientDBDocumentConfiguration().apply(null));
    }

    @Test
    public void shouldReturnFromConfiguration() {
        var configuration = DatabaseConfiguration.getConfiguration();
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof DatabaseConfiguration);
    }

    @Test
    public void shouldReturnFromConfigurationQuery() {
        OrientDBDocumentConfiguration configuration = DatabaseConfiguration
                .getConfiguration(OrientDBDocumentConfiguration.class);
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof OrientDBDocumentConfiguration);
    }
}
