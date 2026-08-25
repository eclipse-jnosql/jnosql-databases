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


import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class RedisBucketManagerTest {

    private BucketManager keyValueEntityManager;

    private BucketManagerFactory keyValueEntityManagerFactory;

    private User userOtavio = new User("otavio");
    private KeyValueEntity keyValueOtavio = KeyValueEntity.of("otavio", Value.of(userOtavio));

    private User userSoro = new User("soro");
    private KeyValueEntity keyValueSoro = KeyValueEntity.of("soro", Value.of(userSoro));

    @BeforeEach
    public void init() {
        keyValueEntityManagerFactory = KeyValueDatabase.INSTANCE.get();
        keyValueEntityManager = keyValueEntityManagerFactory.apply("users-entity");
    }


    @Test
    public void shouldPutValue() {
        keyValueEntityManager.put("otavio", userOtavio);
        Optional<Value> otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(userOtavio);
    }

    @Test
    public void shouldPutKeyValue() {
        keyValueEntityManager.put(keyValueOtavio);
        Optional<Value> otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(userOtavio);
    }

    @Test
    public void shouldPutIterableKeyValue() {
        keyValueEntityManager.put(asList(keyValueSoro, keyValueOtavio));
        Optional<Value> otavio = keyValueEntityManager.get("otavio");
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(userOtavio);

        Optional<Value> soro = keyValueEntityManager.get("soro");
        assertThat(soro.isPresent()).isTrue();
        assertThat(soro.get().get(User.class)).isEqualTo(userSoro);
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
        keyValueEntityManager.put(keyValueOtavio);
        assertThat(keyValueEntityManager.get("otavio").isPresent()).isTrue();
        keyValueEntityManager.delete("otavio");
        assertThat(keyValueEntityManager.get("otavio").isPresent()).isFalse();
    }

    @Test
    public void shouldRemoveMultiKey() {
        keyValueEntityManager.put(asList(keyValueSoro, keyValueOtavio));
        List<String> keys = asList("otavio", "soro");
        Iterable<Value> values = keyValueEntityManager.get(keys);
        assertThat(StreamSupport.stream(values.spliterator(), false)
                .map(value -> value.get(User.class)).collect(Collectors.toList()))
                .contains(userOtavio, userSoro);
        keyValueEntityManager.delete(keys);
        assertThat(StreamSupport.stream(keyValueEntityManager.get(keys).spliterator(), false).count()).isEqualTo(0L);
    }

    @AfterEach
    public void remove() {
        keyValueEntityManager.delete(Arrays.asList("otavio", "soro"));
    }
}
