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

package org.eclipse.jnosql.databases.hazelcast.communication;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;


public class KeyValueEntityManagerFactoryTest {

    public static final String BUCKET_NAME = "bucketName";
    private BucketManagerFactory managerFactory;

    @BeforeEach
    public void setUp() {
        KeyValueConfiguration configuration = new HazelcastKeyValueConfiguration();
        managerFactory = configuration.apply(Settings.builder().build());
    }

    @Test
    public void shouldCreateKeyValueEntityManager(){
        BucketManager keyValueEntityManager = managerFactory.apply(BUCKET_NAME);
        assertThat(keyValueEntityManager).isNotNull();
    }

    @Test
    public void shouldCreateMap(){
        Map<String, String> map = managerFactory.getMap(BUCKET_NAME, String.class, String.class);
        assertThat(map).isNotNull();
    }

    @Test
    public void shouldCreateSet(){
        Set<String> set = managerFactory.getSet(BUCKET_NAME, String.class);
        assertThat(set).isNotNull();
    }

    @Test
    public void shouldCreateList(){
        List<String> list = managerFactory.getList(BUCKET_NAME, String.class);
        assertThat(list).isNotNull();
    }

    @Test
    public void shouldCreateQueue(){
        Queue<String> queue = managerFactory.getQueue(BUCKET_NAME, String.class);
        assertThat(queue).isNotNull();
    }

}