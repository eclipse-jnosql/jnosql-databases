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
 */

package org.eclipse.jnosql.databases.solr.communication;

import org.eclipse.jnosql.communication.semistructured.DatabaseConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class SolrDocumentConfigurationTest {


    @Test
    public void shouldReturnErrorWhenSettingsIsNull() {
        var configuration = new SolrDocumentConfiguration();
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> configuration.apply(null));
    }


    @Test
    public void shouldReturnFromConfiguration() {
        var configuration = DatabaseConfiguration.getConfiguration();
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof DatabaseConfiguration).isTrue();
    }

    @Test
    public void shouldReturnFromConfigurationQuery() {
        SolrDocumentConfiguration configuration = DatabaseConfiguration
                .getConfiguration(SolrDocumentConfiguration.class);
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof SolrDocumentConfiguration).isTrue();
    }

}
