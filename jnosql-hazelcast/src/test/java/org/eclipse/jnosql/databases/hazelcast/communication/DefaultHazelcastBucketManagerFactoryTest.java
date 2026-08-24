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
package org.eclipse.jnosql.databases.hazelcast.communication;

import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultHazelcastBucketManagerFactoryTest {

    private HazelcastBucketManagerFactory managerFactory;

    @BeforeEach
    public void setUp() {
        HazelcastKeyValueConfiguration configuration = new HazelcastKeyValueConfiguration();
        managerFactory = configuration.apply(Settings.builder().build());
    }


    @Test
    public void shouldReturnList() {
        List<String> list = managerFactory.getList("list_sample", String.class);
        assertNotNull(list);
    }

    @Test
    public void shouldReturnSet() {
        Set<String> set = managerFactory.getSet("set_sample", String.class);
        assertNotNull(set);
    }

    @Test
    public void shouldReturnQueue() {
        Queue<String> queue = managerFactory.getQueue("queue_sample", String.class);
        assertNotNull(queue);
    }

    @Test
    public void shouldReturnMap() {
        Map<String, String> map = managerFactory.getMap("map_sample", String.class, String.class);
        assertNotNull(map);
    }


    @Test
    public void shouldReturnErrorWhenNullParameterList() {
        assertThrows(NullPointerException.class, () -> managerFactory.getList(null, String.class));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterSet() {
        assertThrows(NullPointerException.class, () -> managerFactory.getSet(null, String.class));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterQueue() {
        assertThrows(NullPointerException.class, () -> managerFactory.getQueue(null, String.class));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterMap() {
        assertThrows(NullPointerException.class, () -> managerFactory.getMap(null, String.class, String.class));
    }

    //
    @Test
    public void shouldReturnListHazelcast() {
        List<String> list = managerFactory.getList("list_sample");
        assertNotNull(list);
    }

    @Test
    public void shouldReturnSetHazelcast() {
        Set<String> set = managerFactory.getSet("set_sample");
        assertNotNull(set);
    }

    @Test
    public void shouldReturnQueueHazelcast() {
        Queue<String> queue = managerFactory.getQueue("queue_sample");
        assertNotNull(queue);
    }

    @Test
    public void shouldReturnMapHazelcast() {
        Map<String, String> map = managerFactory.getMap("map_sample");
        assertNotNull(map);
    }


    @Test
    public void shouldReturnErrorWhenNullParameterListHazelcast() {
        assertThrows(NullPointerException.class, () -> managerFactory.getList(null));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterSetHazelcast() {
        assertThrows(NullPointerException.class, () -> managerFactory.getSet(null));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterQueueHazelcast() {
        assertThrows(NullPointerException.class, () -> managerFactory.getQueue(null));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterMapHazelcast() {
        assertThrows(NullPointerException.class, () -> managerFactory.getMap(null));
    }

}