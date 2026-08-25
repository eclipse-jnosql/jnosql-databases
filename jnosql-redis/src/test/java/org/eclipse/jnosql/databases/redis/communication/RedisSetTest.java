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
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisSetTest {

    private BucketManagerFactory keyValueEntityManagerFactory;
    private User userOtavioJava = new User("otaviojava");
    private User felipe = new User("ffrancesquini");
    private Set<User> users;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        users = keyValueEntityManagerFactory.getSet("social-media", User.class);
    }

    @Test
    public void shouldAddUsers() {
        users.add(userOtavioJava);
        assertThat(users.size()).isEqualTo(1);
    }

    @Test
    public void shouldRemove() {
        users.add(userOtavioJava);
        users.add(felipe);
        users.remove(felipe);

        assertThat(users.size()).isEqualTo(1);
        assertThat(users).isNotIn(felipe);
   }

    @Test
    public void shouldRemoveAll() {
        users.add(userOtavioJava);
        users.add(felipe);
        users.removeAll(Arrays.asList(felipe, userOtavioJava));

        assertThat(users.size()).isEqualTo(0);
    }

    @SuppressWarnings("unused")
    @Test
    public void shouldIterate() {
        users.add(userOtavioJava);
        users.add(userOtavioJava);
        users.add(felipe);
        users.add(userOtavioJava);
        users.add(felipe);
        int count = 0;
        for (User user : users) {
            count++;
        }
        assertThat(count).isEqualTo(2);
    }

    @Test
    public void shouldContains() {
        users.add(userOtavioJava);
        assertThat(users.contains(userOtavioJava)).isTrue();
    }

    @Test
    public void shouldContainsAll() {
        users.add(userOtavioJava);
        users.add(felipe);
        assertThat(users.containsAll(Arrays.asList(userOtavioJava, felipe))).isTrue();
    }

    @Test
    public void shouldReturnSize() {
        users.add(userOtavioJava);
        users.add(felipe);
        assertThat(users.size()).isEqualTo(2);
    }

    @Test
    public void shouldClear() {
        users.add(userOtavioJava);
        users.add(felipe);

        users.clear();
        assertThat(users.isEmpty()).isTrue();
    }

    @Test
    public void shouldThrowExceptionRetainAll() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> users.retainAll(Collections.singletonList(userOtavioJava)));
    }

    @AfterEach
    public void dispose() {
        users.clear();
    }
}
