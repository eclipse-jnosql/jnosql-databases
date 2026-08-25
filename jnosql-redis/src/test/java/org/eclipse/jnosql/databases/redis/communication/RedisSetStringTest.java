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

package org.eclipse.jnosql.databases.redis.communication;


import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Arrays;
import java.util.Set;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisSetStringTest {


    private BucketManagerFactory keyValueEntityManagerFactory;

    private Set<String> users;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        users = keyValueEntityManagerFactory.getSet("social-media-string", String.class);
    }

    @Test
    public void shouldAddUsers() {
        users.add("otaviojava");
        assertThat(users.size()).isEqualTo(1);

        String user = users.iterator().next();
        assertThat(user).isEqualTo("otaviojava");
    }

    @Test
    public void shouldRemoveSet() {
        users.add("otaviojava");
        users.remove("otaviojava");
        assertThat(users.isEmpty()).isTrue();
    }


    @SuppressWarnings("unused")
    @Test
    public void shouldIterate() {
        users.add("otaviojava");
        users.add("otaviojava");
        users.add("felipe");
        users.add("otaviojava");
        users.add("felipe");
        int count = 0;
        for (String user : users) {
            count++;
        }
        assertThat(count).isEqualTo(2);
        users.remove("otaviojava");
        users.remove("felipe");
        count = 0;
        for (String user : users) {
            count++;
        }
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void shouldClear() {
        users.add("otaviojava");
        users.clear();
        assertThat(users.isEmpty()).isTrue();
    }

    @Test
    public void shouldContains() {
        users.add("otaviojava");
        assertThat(users.contains("otaviojava")).isTrue();
    }

    @Test
    public void shouldContainsAll() {
        users.add("otaviojava");
        users.add("furlaneto");
        users.add("joao");
        assertThat(users.containsAll(Arrays.asList("furlaneto", "otaviojava"))).isTrue();
    }

    @Test
    public void shouldReturnSize() {
        users.add("otaviojava");
        users.add("furlaneto");
        users.add("joao");
        assertThat(users.size()).isEqualTo(3);
    }
    
    @AfterEach
    public void dispose() {
        users.clear();
    }
}
