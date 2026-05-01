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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(1, users.size());
    }

    @Test
    public void shouldRemove() {
        users.add(userOtavioJava);
        users.add(felipe);
        users.remove(felipe);

        assertEquals(1, users.size());
        assertThat(users).isNotIn(felipe);
   }

    @Test
    public void shouldRemoveAll() {
        users.add(userOtavioJava);
        users.add(felipe);
        users.removeAll(Arrays.asList(felipe, userOtavioJava));

        assertEquals(0, users.size());
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
        assertEquals(2, count);
    }

    @Test
    public void shouldContains() {
        users.add(userOtavioJava);
        assertTrue(users.contains(userOtavioJava));
    }

    @Test
    public void shouldContainsAll() {
        users.add(userOtavioJava);
        users.add(felipe);
        assertTrue(users.containsAll(Arrays.asList(userOtavioJava, felipe)));
    }

    @Test
    public void shouldReturnSize() {
        users.add(userOtavioJava);
        users.add(felipe);
        assertEquals(2, users.size());
    }

    @Test
    public void shouldClear() {
        users.add(userOtavioJava);
        users.add(felipe);

        users.clear();
        assertTrue(users.isEmpty());
    }

    @Test
    public void shouldThrowExceptionRetainAll() {
        assertThrows(UnsupportedOperationException.class, () -> users.retainAll(Collections.singletonList(userOtavioJava)));
    }

    @AfterEach
    public void dispose() {
        users.clear();
    }
}
