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

package org.eclipse.jnosql.databases.memcached.communication;


import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class MemcachedBucketManagerTest {

    private BucketManager keyValueEntityManager;

    private BucketManagerFactory keyValueEntityManagerFactory;

    private User otavio = new User("otavio");
    private KeyValueEntity entityOtavio = KeyValueEntity.of("otavio", Value.of(otavio));

    private User soro = new User("soro");
    private KeyValueEntity entitySoro = KeyValueEntity.of("soro", Value.of(soro));

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        keyValueEntityManager = keyValueEntityManagerFactory.apply("users-entity");
    }


    @Test
    public void shouldPutValue() {
        keyValueEntityManager.put("otavio", otavio);
        Optional<Value> otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(this.otavio);
    }

    @Test
    public void shouldPutKeyValue() {
        keyValueEntityManager.put(entityOtavio);
        Optional<Value> otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(this.otavio);
    }

    @Test
    public void shouldPutValueDuration() throws InterruptedException {
        keyValueEntityManager.put(entityOtavio, Duration.ofSeconds(1L));
        Optional<Value> otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isTrue();
        TimeUnit.SECONDS.sleep(3L);
        otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isFalse();
    }


    @Test
    public void shouldPutIterableKeyValue() {

        keyValueEntityManager.put(asList(entitySoro, entityOtavio));
        Optional<Value> otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(this.otavio);

        Optional<Value> soro = keyValueEntityManager.get("soro");
        assertThat(soro.isPresent()).isTrue();
        assertThat(soro.get().get(User.class)).isEqualTo(this.soro);
    }

    @Test
    public void shouldMultiGet() {
        User user = new User("otavio");
        KeyValueEntity keyValue = KeyValueEntity.of("otavio", Value.of(user));
        keyValueEntityManager.put(keyValue);
        assertThat(keyValueEntityManager.get("otavio")).isNotNull();


    }

    @Test
    public void shouldRemoveKey() {

        keyValueEntityManager.put(entityOtavio);
        assertThat(keyValueEntityManager.get("otavio").isPresent()).isTrue();
        keyValueEntityManager.delete("otavio");
        assertThat(keyValueEntityManager.get("otavio").isPresent()).isFalse();
    }

    @Test
    public void shouldRemoveMultiKey() {

        keyValueEntityManager.put(asList(entitySoro, entityOtavio));
        List<String> keys = asList("otavio", "soro");
        Iterable<Value> values = keyValueEntityManager.get(keys);
        assertThat(StreamSupport.stream(values.spliterator(), false)
                .map(value -> value.get(User.class)).collect(Collectors.toList()))
                .contains(otavio, soro);
        keyValueEntityManager.delete(keys);
        Iterable<Value> users = values;
        assertThat(StreamSupport.stream(keyValueEntityManager.get(keys).spliterator(), false).count()).isEqualTo(0L);
    }


}
