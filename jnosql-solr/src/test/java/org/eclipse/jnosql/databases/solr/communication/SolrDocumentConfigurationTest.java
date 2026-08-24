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

package org.eclipse.jnosql.databases.solr.communication;

import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class SolrDocumentConfigurationTest {


    @Test
    public void shouldReturnErrorWhenSettingsIsNull() {
        var configuration = new SolrDocumentConfiguration();
        assertThrows(NullPointerException.class, () -> configuration.apply(null));
    }


    @Test
    public void shouldReturnFromConfiguration() {
        var configuration = DatabaseConfiguration.getConfiguration();
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof DatabaseConfiguration);
    }

    @Test
    public void shouldReturnFromConfigurationQuery() {
        SolrDocumentConfiguration configuration = DatabaseConfiguration
                .getConfiguration(SolrDocumentConfiguration.class);
        Assertions.assertNotNull(configuration);
        Assertions.assertTrue(configuration instanceof SolrDocumentConfiguration);
    }

}
