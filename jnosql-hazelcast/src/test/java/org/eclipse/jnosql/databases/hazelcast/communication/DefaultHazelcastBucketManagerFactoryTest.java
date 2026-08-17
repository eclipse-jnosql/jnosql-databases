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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


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
        assertThat(list).isNotNull();
    }

    @Test
    public void shouldReturnSet() {
        Set<String> set = managerFactory.getSet("set_sample", String.class);
        assertThat(set).isNotNull();
    }

    @Test
    public void shouldReturnQueue() {
        Queue<String> queue = managerFactory.getQueue("queue_sample", String.class);
        assertThat(queue).isNotNull();
    }

    @Test
    public void shouldReturnMap() {
        Map<String, String> map = managerFactory.getMap("map_sample", String.class, String.class);
        assertThat(map).isNotNull();
    }


    @Test
    public void shouldReturnErrorWhenNullParameterList() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getList(null, String.class));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterSet() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getSet(null, String.class));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterQueue() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getQueue(null, String.class));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterMap() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getMap(null, String.class, String.class));
    }

    //
    @Test
    public void shouldReturnListHazelcast() {
        List<String> list = managerFactory.getList("list_sample");
        assertThat(list).isNotNull();
    }

    @Test
    public void shouldReturnSetHazelcast() {
        Set<String> set = managerFactory.getSet("set_sample");
        assertThat(set).isNotNull();
    }

    @Test
    public void shouldReturnQueueHazelcast() {
        Queue<String> queue = managerFactory.getQueue("queue_sample");
        assertThat(queue).isNotNull();
    }

    @Test
    public void shouldReturnMapHazelcast() {
        Map<String, String> map = managerFactory.getMap("map_sample");
        assertThat(map).isNotNull();
    }


    @Test
    public void shouldReturnErrorWhenNullParameterListHazelcast() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getList(null));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterSetHazelcast() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getSet(null));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterQueueHazelcast() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getQueue(null));
    }

    @Test
    public void shouldReturnErrorWhenNullParameterMapHazelcast() {
        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> managerFactory.getMap(null));
    }

}