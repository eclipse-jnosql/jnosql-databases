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

package org.eclipse.jnosql.databases.hbase.communication;

import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class HBaseColumnConfigurationTest {


    @Test
    public void shouldCreatesColumnManagerFactory() {
        var configuration = new HBaseColumnConfiguration();
        assertNotNull(configuration.apply(Settings.builder().build()));
    }

    @Test
    public void shouldCreatesColumnManagerFactoryFromConfiguration() {
        var configuration = new HBaseColumnConfiguration();
        assertNotNull(configuration.apply(Settings.builder().build()));
    }

    @Test
    public void shouldReturnErrorCreatesColumnManagerFactory() {
        assertThrows(NullPointerException.class, () -> new HBaseColumnConfiguration(null));
    }
}