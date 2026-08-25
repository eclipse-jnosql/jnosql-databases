/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class OracleNoSQLKeyValueConfigurationTest {

    @Test
    void shouldReturnErrorWhenMapSettingsIsNull() {
        OracleNoSQLKeyValueConfiguration configuration = new OracleNoSQLKeyValueConfiguration();
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> configuration.apply(null));
    }

    @Test
    void shouldReturnFromConfiguration() {
        KeyValueConfiguration configuration = KeyValueConfiguration.getConfiguration();
        assertThat(configuration).isNotNull()
                .isInstanceOf(KeyValueConfiguration.class)
                .isInstanceOf(OracleNoSQLKeyValueConfiguration.class);
    }
}