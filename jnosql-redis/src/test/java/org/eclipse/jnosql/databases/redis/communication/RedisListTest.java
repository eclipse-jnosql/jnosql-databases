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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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
        assertThat(fruits).isNotNull();
    }

    @Test
    public void shouldAddList() {
        assertThat(fruits.isEmpty()).isTrue();
        fruits.add(banana);
        assertThat(fruits.isEmpty()).isFalse();
        ProductCart banana = fruits.getFirst();
        assertThat(banana).isNotNull();
        assertThat("banana").isEqualTo(banana.name());
    }

    @Test
    public void shouldSetList() {
        fruits.add(banana);
        fruits.addFirst(orange);
        assertThat(fruits.size()).isEqualTo(2);

        assertThat("orange").isEqualTo(fruits.get(0).name());
        assertThat("banana").isEqualTo(fruits.get(1).name());

        fruits.set(0, waterMelon);
        assertThat("waterMelon").isEqualTo(fruits.get(0).name());
        assertThat("banana").isEqualTo(fruits.get(1).name());
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
        assertThat(fruits.size()).isEqualTo(1);
        assertThat(fruits).contains(waterMelon);
    }

    @Test
    public void shouldRemoveWithIndex() {
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);

        fruits.removeFirst();
        assertThat(fruits).hasSize(2).isNotIn(orange);
    }

    @Test
    public void shouldReturnIndexOf() {
        fruits.add(new ProductCart("orange", BigDecimal.ONE));
        fruits.add(banana);
        fruits.add(new ProductCart("watermellon", BigDecimal.ONE));
        fruits.add(banana);
        assertThat(fruits.indexOf(banana)).isEqualTo(1);
        assertThat(fruits.lastIndexOf(banana)).isEqualTo(3);

        assertThat(fruits.contains(banana)).isTrue();
        assertThat(fruits.indexOf(melon)).isEqualTo(-1);
        assertThat(fruits.lastIndexOf(melon)).isEqualTo(-1);
    }

    @Test
    public void shouldReturnContains() {
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);
        assertThat(fruits.contains(banana)).isTrue();
        assertThat(fruits.contains(melon)).isFalse();
        assertThat(fruits.containsAll(Arrays.asList(banana, orange))).isTrue();
        assertThat(fruits.containsAll(Arrays.asList(banana, melon))).isFalse();
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
        assertThat(count).isEqualTo(2);
        fruits.removeFirst();
        fruits.removeFirst();
        count = 0;
        for (ProductCart fruiCart : fruits) {
            count++;
        }
        assertThat(count).isEqualTo(0);
    }

    @Test
    public void shouldClear(){
        fruits.add(orange);
        fruits.add(banana);
        fruits.add(waterMelon);

        fruits.clear();
        assertThat(fruits.isEmpty()).isTrue();
    }

    @Test
    public void shouldThrowExceptionRetainAll() {
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> fruits.retainAll(Collections.singletonList(orange)));
    }

    @AfterEach
    public void end() {
        fruits.clear();
    }
}
