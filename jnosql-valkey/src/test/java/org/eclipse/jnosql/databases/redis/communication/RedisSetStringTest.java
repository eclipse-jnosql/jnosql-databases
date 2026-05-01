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
import java.util.Set;

import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(1, users.size());

        String user = users.iterator().next();
        assertEquals("otaviojava", user);
    }

    @Test
    public void shouldRemoveSet() {
        users.add("otaviojava");
        users.remove("otaviojava");
        assertTrue(users.isEmpty());
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
        assertEquals(2, count);
        users.remove("otaviojava");
        users.remove("felipe");
        count = 0;
        for (String user : users) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void shouldClear() {
        users.add("otaviojava");
        users.clear();
        assertTrue(users.isEmpty());
    }

    @Test
    public void shouldContains() {
        users.add("otaviojava");
        assertTrue(users.contains("otaviojava"));
    }

    @Test
    public void shouldContainsAll() {
        users.add("otaviojava");
        users.add("furlaneto");
        users.add("joao");
        assertTrue(users.containsAll(Arrays.asList("furlaneto", "otaviojava")));
    }

    @Test
    public void shouldReturnSize() {
        users.add("otaviojava");
        users.add("furlaneto");
        users.add("joao");
        assertEquals(3, users.size());
    }
    
    @AfterEach
    public void dispose() {
        users.clear();
    }
}
