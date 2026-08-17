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

import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.databases.hazelcast.communication.model.ProductCart;
import org.eclipse.jnosql.databases.hazelcast.communication.util.KeyValueEntityManagerFactoryUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class ListTest {


    private static final String FRUITS = "fruits";
    private ProductCart banana = new ProductCart("banana", BigDecimal.ONE);
    private ProductCart orange = new ProductCart("orange", BigDecimal.ONE);
    private ProductCart waterMelon = new ProductCart("waterMelon", BigDecimal.TEN);
    private ProductCart melon = new ProductCart("melon", BigDecimal.ONE);

    private BucketManagerFactory keyValueEntityManagerFactory;

    private List<ProductCart> fruits;

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory =  KeyValueEntityManagerFactoryUtils.get();
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
        fruits.add(banana);
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
        for (ProductCart fruiCart: fruits) {
            count++;
        }
        assertThat(count).isEqualTo(2);
        fruits.removeFirst();
        fruits.removeFirst();
        count = 0;
        for (ProductCart fruiCart: fruits) {
            count++;
        }
        assertThat(count).isEqualTo(0);
    }
    @AfterEach
    public  void end() {
        fruits.clear();
    }
}
