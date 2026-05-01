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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisListTest {


    private static final String FRUITS = "fruits";
    private ProductCart banana = new ProductCart("banana", BigDecimal.ONE);
    private ProductCart orange = new ProductCart("orange", BigDecimal.ONE);
    private ProductCart waterMelon = new ProductCart("waterMelon", BigDecimal.TEN);
    private ProductCart melon = new ProductCart("melon", BigDecimal.ONE);

    private BucketManagerFactory keyValueEntityManagerFactory;

    private List<ProductCart> fruits;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        fruits = keyValueEntityManagerFactory.getList(FRUITS, ProductCart.class);
    }

    @Test
    public void shouldReturnsList() {
        assertNotNull(fruits);
    }

    @Test
    public void shouldAddList() {
        assertTrue(fruits.isEmpty());
        fruits.add(banana);
        assertFalse(fruits.isEmpty());
        ProductCart banana = fruits.get(0);
        assertNotNull(banana);
        assertEquals(banana.name(), "banana");
    }

    @Test
    public void shouldSetList() {
        fruits.add(banana);
        fruits.add(0, orange);
        assertEquals(2, fruits.size());

        assertEquals(fruits.get(0).name(), "orange");
        assertEquals(fruits.get(1).name(), "banana");

        fruits.set(0, waterMelon);
        assertEquals(fruits.get(0).name(), "waterMelon");
        assertEquals(fruits.get(1).name(), "banana");
    }

    @Test
    public void shouldRemoveList() {
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);

        fruits.remove(waterMelon);
        assertThat(fruits).isNotIn(waterMelon);
    }

    @Test
    public void shouldRemoveAll() {
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);

        fruits.removeAll(Arrays.asList(orange, banana));
        assertEquals(1, fruits.size());
        assertThat(fruits).contains(waterMelon);
    }

    @Test
    public void shouldRemoveWithIndex() {
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);

        fruits.remove(0);
        assertThat(fruits).hasSize(2).isNotIn(orange);
    }

    @Test
    public void shouldReturnIndexOf() {
        fruits.add(new ProductCart("orange", BigDecimal.ONE));
        fruits.add(banana);
        fruits.add(new ProductCart("watermellon", BigDecimal.ONE));
        fruits.add(banana);
        assertEquals(1, fruits.indexOf(banana));
        assertEquals(3, fruits.lastIndexOf(banana));

        assertTrue(fruits.contains(banana));
        assertEquals(-1, fruits.indexOf(melon));
        assertEquals(-1, fruits.lastIndexOf(melon));
    }

    @Test
    public void shouldReturnContains() {
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);
        assertTrue(fruits.contains(banana));
        assertFalse(fruits.contains(melon));
        assertTrue(fruits.containsAll(Arrays.asList(banana, orange)));
        assertFalse(fruits.containsAll(Arrays.asList(banana, melon)));
    }

    @SuppressWarnings("unused")
    @Test
    public void shouldIterate() {
        fruits.add(melon);
        fruits.add(banana);
        int count = 0;
        for (ProductCart fruiCart : fruits) {
            count++;
        }
        assertEquals(2, count);
        fruits.remove(0);
        fruits.remove(0);
        count = 0;
        for (ProductCart fruiCart : fruits) {
            count++;
        }
        assertEquals(0, count);
    }

    @Test
    public void shouldClear(){
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);

        fruits.clear();
        assertTrue(fruits.isEmpty());
    }

    @Test
    public void shouldThrowExceptionRetainAll() {
        assertThrows(UnsupportedOperationException.class, () -> fruits.retainAll(Collections.singletonList(orange)));
    }

    @AfterEach
    public void end() {
        fruits.clear();
    }
}
