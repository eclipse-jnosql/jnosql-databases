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
package org.eclipse.jnosql.databases.memcached.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class KeyValueConfigurationTest {

    private MemcachedKeyValueConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new MemcachedKeyValueConfiguration();
    }

    @Test
    public void shouldCreateKeyValueFactory() {
        Map<String, Object> map = new HashMap<>();
        map.put(MemcachedConfigurations.HOST.get()+".1","localhost:11211");
        BucketManagerFactory managerFactory = configuration.apply(Settings.of(map));
        assertThat(managerFactory).isNotNull();
    }

    @Test
    public void shouldReturnFromConfiguration() {
        KeyValueConfiguration configuration = KeyValueConfiguration.getConfiguration();
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof KeyValueConfiguration).isTrue();
    }

    @Test
    public void shouldReturnFromConfigurationQuery() {
        MemcachedKeyValueConfiguration configuration = KeyValueConfiguration
                .getConfiguration(MemcachedKeyValueConfiguration.class);
        assertThat(configuration).isNotNull();
        assertThat(configuration instanceof MemcachedKeyValueConfiguration).isTrue();
    }

}