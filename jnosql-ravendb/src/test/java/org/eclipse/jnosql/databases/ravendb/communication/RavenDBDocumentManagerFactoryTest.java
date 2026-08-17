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

package org.eclipse.jnosql.databases.ravendb.communication;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class RavenDBDocumentManagerFactoryTest {

    private static RavenDBDocumentConfiguration configuration;

    @BeforeAll
    public static void setUp() {
        configuration = new RavenDBDocumentConfiguration();
    }

    @Test
    public void shouldCreateEntityManager() {
        RavenDBDocumentManagerFactory ravenDBFactory = configuration.apply(DocumentConfigurationUtils.INSTANCE.getSettings());
        assertThat(ravenDBFactory.apply("database")).isNotNull();
    }

    @Test
    public void shouldReturnNPEWhenSettingsIsNull() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> configuration.apply(null));
    }


}