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
package org.eclipse.jnosql.databases.memcached.communication;

import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public class MemcachedBucketManagerFactoryTest {

    private MemcachedBucketManagerFactory managerFactory;

    @BeforeEach
    public void setUp() {
        MemcachedKeyValueConfiguration configuration = new MemcachedKeyValueConfiguration();
        Settings settings = Settings.builder()
                .put(MemcachedConfigurations.HOST.get()+".1", "localhost:11211")
                .build();
        managerFactory = configuration.apply(settings);
    }

    @Test
    public void shouldReturnErrorList() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> managerFactory.getList(null, String.class));
    }

    @Test
    public void shouldReturnErrorSet() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> managerFactory.getSet(null, String.class));
    }

    @Test
    public void shouldReturnErrorQueue() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> managerFactory.getQueue(null, String.class));
    }

    @Test
    public void shouldReturnErrorMap() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> managerFactory.getMap(null, String.class, String.class));
    }

}