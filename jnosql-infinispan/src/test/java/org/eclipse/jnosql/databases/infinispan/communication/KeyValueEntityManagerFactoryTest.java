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
 *   The Infinispan Team
 */
package org.eclipse.jnosql.databases.infinispan.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class KeyValueEntityManagerFactoryTest {

    public static final String BUCKET_NAME = "bucketName";
    private BucketManagerFactory managerFactory;

    @BeforeEach
    public void setUp() {
        KeyValueConfiguration configuration = new InfinispanKeyValueConfiguration();
        Settings settings = Settings.builder()
                .put(InfinispanConfigurations.CONFIG, "infinispan.xml")
                .build();
        managerFactory = configuration.apply(settings);
    }

    @Test
    public void shouldCreateKeyValueEntityManager(){
        BucketManager keyValueEntityManager = managerFactory.apply(BUCKET_NAME);
        assertNotNull(keyValueEntityManager);
    }

    @Test
    public void shouldCreateMap(){
        Map<String, String> map = managerFactory.getMap(BUCKET_NAME, String.class, String.class);
        assertNotNull(map);
    }

    @Test
    public void shouldCreateSet(){
        assertThrows(UnsupportedOperationException.class, () -> managerFactory.getSet(BUCKET_NAME, String.class));
    }

    @Test
    public void shouldCreateList(){
        assertThrows(UnsupportedOperationException.class, () -> managerFactory.getList(BUCKET_NAME, String.class));
    }

    @Test
    public void shouldCreateQueue(){
        assertThrows(UnsupportedOperationException.class, () -> managerFactory.getQueue(BUCKET_NAME, String.class));
    }

}
