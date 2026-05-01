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

package org.eclipse.jnosql.databases.redis.communication;

import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisBucketManagerFactoryTest {

    public static final String BUCKET_NAME = "bucketName";
    private BucketManagerFactory managerFactory;

    @BeforeEach
    public void setUp() {
        managerFactory = KeyValueDatabase.INSTANCE.get();
    }

    @Test
    public void shouldCreateKeyValueEntityManager() {
        BucketManager keyValueEntityManager = managerFactory.apply(BUCKET_NAME);
        assertNotNull(keyValueEntityManager);
    }

    @Test
    public void shouldCreateMap() {
        Map<String, String> map = managerFactory.getMap(BUCKET_NAME, String.class, String.class);
        assertNotNull(map);
    }

    @Test
    public void shouldCreateSet() {
        Set<String> set = managerFactory.getSet(BUCKET_NAME, String.class);
        assertNotNull(set);
    }

    @Test
    public void shouldCreateList() {
        List<String> list = managerFactory.getList(BUCKET_NAME, String.class);
        assertNotNull(list);
    }

    @Test
    public void shouldCreateQueue() {
        Queue<String> queue = managerFactory.getQueue(BUCKET_NAME, String.class);
        assertNotNull(queue);
    }

}