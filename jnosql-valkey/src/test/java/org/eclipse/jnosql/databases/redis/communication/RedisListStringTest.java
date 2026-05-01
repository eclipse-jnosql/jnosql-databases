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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisListStringTest {


    private static final String FRUITS = "fruits-string";

    private BucketManagerFactory keyValueEntityManagerFactory;

    private List<String> fruits;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        fruits = keyValueEntityManagerFactory.getList(FRUITS, String.class);
    }

    @Test
    public void shouldReturnsList() {
        assertNotNull(fruits);
    }

    @Test
    public void shouldAddList() {
        assertTrue(fruits.isEmpty());
        fruits.add("banana");
        assertFalse(fruits.isEmpty());
        String banana = fruits.get(0);
        assertNotNull(banana);
        assertEquals(banana, "banana");
    }
    
    @Test
    public void shouldAddAll() {
        fruits.addAll(Arrays.asList("banana", "orange"));
        assertEquals(2, fruits.size());
    }

    @Test
    public void shouldSetList() {
        fruits.add("banana");
        fruits.add(0, "orange");
        assertEquals(2, fruits.size());

        assertEquals(fruits.get(0), "orange");
        assertEquals(fruits.get(1), "banana");

        fruits.set(0, "waterMelon");
        assertEquals(fruits.get(0), "waterMelon");
        assertEquals(fruits.get(1), "banana");

    }

    @Test
    public void shouldRemoveList() {
        fruits.add("banana");
        fruits.add("orange");
        fruits.add("watermellon");

        fruits.remove("banana");
        assertThat(fruits).isNotIn("banana");
    }

    @Test
    public void shouldReturnIndexOf() {
        fruits.add("orange");
        fruits.add("banana");
        fruits.add("watermellon");
        fruits.add("banana");
        assertEquals(1, fruits.indexOf("banana"));
        assertEquals(3, fruits.lastIndexOf("banana"));

        assertTrue(fruits.contains("banana"));
        assertEquals(-1, fruits.indexOf("melon"));
        assertEquals(-1, fruits.lastIndexOf("melon"));
    }

    @Test
    public void shouldReturnContains() {
        fruits.add("orange");
        fruits.add("banana");
        fruits.add("watermellon");
        assertTrue(fruits.contains("banana"));
        assertFalse(fruits.contains("melon"));
        assertTrue(fruits.containsAll(Arrays.asList("banana", "orange")));
        assertFalse(fruits.containsAll(Arrays.asList("banana", "melon")));

    }

    @SuppressWarnings("unused")
    @Test
    public void shouldIterate() {
        fruits.add("melon");
        fruits.add("banana");
        int count = 0;
        for (String fruiCart : fruits) {
            count++;
        }
        assertEquals(2, count);
        fruits.remove(0);
        fruits.remove(0);
        count = 0;
        for (String fruiCart : fruits) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void shouldClear(){
        fruits.add("orange");
        fruits.add("banana");
        fruits.add("watermellon");

        fruits.clear();
        assertTrue(fruits.isEmpty());
    }

    @AfterEach
    public void end() {
        fruits.clear();
    }
}
