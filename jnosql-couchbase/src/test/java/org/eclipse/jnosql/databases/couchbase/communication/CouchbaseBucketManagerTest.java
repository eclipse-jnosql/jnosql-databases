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
package org.eclipse.jnosql.databases.couchbase.communication;

import org.assertj.core.api.Assertions;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;
import static org.assertj.core.api.Assertions.assertThat;
@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
public class CouchbaseBucketManagerTest {

    private static final String KEY_SORO = "soro";
    private static final String KEY_OTAVIO = "otavio";

    private static BucketManager manager;
    private static BucketManagerFactory factory;

    private User userOtavio = new User(KEY_OTAVIO);
    private KeyValueEntity entityOtavio = KeyValueEntity.of(KEY_OTAVIO, Value.of(userOtavio));

    private User userSoro = new User(KEY_SORO);
    private KeyValueEntity soroEntity = KeyValueEntity.of(KEY_SORO, Value.of(userSoro));

    @BeforeAll
    public static void beforeAll() {
        Settings settings = Database.INSTANCE.getSettings();
        CouchbaseKeyValueConfiguration configuration = Database.INSTANCE.getKeyValueConfiguration();
        factory = configuration.apply(settings);
        manager = factory.apply(CouchbaseUtil.BUCKET_NAME);
    }

    @AfterAll
    public static void afterClass() {
        Settings settings = Database.INSTANCE.getSettings();
        CouchbaseKeyValueConfiguration configuration = Database.INSTANCE.getKeyValueConfiguration();
        BucketManagerFactory keyValueEntityManagerFactory = configuration.apply(settings);
        BucketManager keyValueEntityManager = keyValueEntityManagerFactory.apply(CouchbaseUtil.BUCKET_NAME);
        keyValueEntityManager.delete(KEY_OTAVIO);
        keyValueEntityManager.delete(KEY_SORO);
    }

    @Test
    public void shouldPutValue() {
        manager.put(KEY_OTAVIO, userOtavio);
        Optional<Value> otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(userOtavio);
    }

    @Test
    public void shouldPutValues() {

        List<KeyValueEntity> entities = asList(KeyValueEntity.of(KEY_OTAVIO, userOtavio),
                KeyValueEntity.of(KEY_SORO, userSoro));

        manager.put(entities);
        Optional<Value> otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(userOtavio);

        Optional<Value> soro = manager.get(KEY_SORO);
        assertThat(soro.isPresent()).isTrue();
        assertThat(soro.get().get(User.class)).isEqualTo(userSoro);
    }


    @Test
    public void shouldPutPrimitivesValues() {
        manager.put("integer", 1);
        Optional<Value> integer = manager.get("integer");
        assertThat(integer.isPresent()).isTrue();
        assertThat(integer.get().get(Integer.class)).isEqualTo(Integer.valueOf(1));
    }

    @Test
    public void shouldPutValueTtl() throws InterruptedException {

        manager.put(KeyValueEntity.of(KEY_OTAVIO, userOtavio), Duration.ofSeconds(1L));

        Optional<Value> otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isTrue();
        Thread.sleep(2_000);
        otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isFalse();
    }

    @Test
    public void shouldPutValuesTtl() throws InterruptedException {

        manager.put(singleton(KeyValueEntity.of(KEY_OTAVIO, userOtavio)), Duration.ofSeconds(1L));
        Optional<Value> otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isTrue();
        Thread.sleep(2_000);
        otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isFalse();
    }



    @Test
    public void shouldPutKeyValue() {
        manager.put(entityOtavio);
        Optional<Value> otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(userOtavio);
    }

    @Test
    public void shouldPutIterableKeyValue() {


        manager.put(asList(soroEntity, entityOtavio));
        Optional<Value> otavio = manager.get(KEY_OTAVIO);
        assertThat(otavio.isPresent()).isTrue();
        assertThat(otavio.get().get(User.class)).isEqualTo(userOtavio);

        Optional<Value> soro = manager.get(KEY_SORO);
        assertThat(soro.isPresent()).isTrue();
        assertThat(soro.get().get(User.class)).isEqualTo(userSoro);
    }

    @Test
    public void shouldMultiGet() {
        User user = new User(KEY_OTAVIO);
        KeyValueEntity keyValue = KeyValueEntity.of(KEY_OTAVIO, Value.of(user));
        manager.put(keyValue);
        assertThat(manager.get(KEY_OTAVIO)).isNotNull();


    }

    @Test
    public void shouldRemoveKey() {
        manager.put(entityOtavio);
        assertThat(manager.get(KEY_OTAVIO).isPresent()).isTrue();
        manager.delete(KEY_OTAVIO);
        assertThat(manager.get(KEY_OTAVIO).isPresent()).isFalse();
    }

    @Test
    public void shouldRemoveMultiKey() {

        manager.put(asList(soroEntity, entityOtavio));
        List<String> keys = asList(KEY_OTAVIO, KEY_SORO);
        Iterable<Value> values = manager.get(keys);
        Assertions.assertThat(StreamSupport.stream(values.spliterator(), false)
                .map(value -> value.get(User.class)).collect(Collectors.toList()))
                .contains(userOtavio, userSoro);
        manager.delete(keys);
        assertThat(StreamSupport.stream(manager.get(keys).spliterator(), false).count()).isEqualTo(0L);
    }


}