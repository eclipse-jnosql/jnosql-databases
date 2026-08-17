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
        assertThat(fruits).isNotNull();
    }

    @Test
    public void shouldAddList() {
        assertThat(fruits.isEmpty()).isTrue();
        fruits.add("banana");
        assertThat(fruits.isEmpty()).isFalse();
        String banana = fruits.getFirst();
        assertThat(banana).isNotNull();
        assertThat("banana").isEqualTo(banana);
    }
    
    @Test
    public void shouldAddAll() {
        fruits.addAll(Arrays.asList("banana", "orange"));
        assertThat(fruits.size()).isEqualTo(2);
    }

    @Test
    public void shouldSetList() {
        fruits.add("banana");
        fruits.addFirst("orange");
        assertThat(fruits.size()).isEqualTo(2);

        assertThat("orange").isEqualTo(fruits.get(0));
        assertThat("banana").isEqualTo(fruits.get(1));

        fruits.set(0, "waterMelon");
        assertThat("waterMelon").isEqualTo(fruits.get(0));
        assertThat("banana").isEqualTo(fruits.get(1));

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
        assertThat(fruits.indexOf("banana")).isEqualTo(1);
        assertThat(fruits.lastIndexOf("banana")).isEqualTo(3);

        assertThat(fruits.contains("banana")).isTrue();
        assertThat(fruits.indexOf("melon")).isEqualTo(-1);
        assertThat(fruits.lastIndexOf("melon")).isEqualTo(-1);
    }

    @Test
    public void shouldReturnContains() {
        fruits.add("orange");
        fruits.add("banana");
        fruits.add("watermellon");
        assertThat(fruits.contains("banana")).isTrue();
        assertThat(fruits.contains("melon")).isFalse();
        assertThat(fruits.containsAll(Arrays.asList("banana", "orange"))).isTrue();
        assertThat(fruits.containsAll(Arrays.asList("banana", "melon"))).isFalse();

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
        assertThat(count).isEqualTo(2);
        fruits.removeFirst();
        fruits.removeFirst();
        count = 0;
        for (String fruiCart : fruits) {
            count++;
        }
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void shouldClear(){
        fruits.add("orange");
        fruits.add("banana");
        fruits.add("watermellon");

        fruits.clear();
        assertThat(fruits.isEmpty()).isTrue();
    }

    @AfterEach
    public void end() {
        fruits.clear();
    }
}
