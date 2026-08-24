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
package org.eclipse.jnosql.databases.memcached.communication;

import net.spy.memcached.MemcachedClient;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.StreamSupport.stream;

final class MemcachedBucketManager implements BucketManager {

    private static final int NO_EXP = 0;
    private final MemcachedClient client;
    private final String bucketName;

    MemcachedBucketManager(MemcachedClient client, String bucketName) {
        this.client = client;
        this.bucketName = bucketName;
    }


    @Override
    public void put(KeyValueEntity entity) {
        requireNonNull(entity, "entity is required");
        put(entity.key(), entity.value());
    }


    @Override
    public String name() {
        return bucketName;
    }

    @Override
    public <K, V> void put(K key, V value) {
        requireNonNull(key, "key is required");
        requireNonNull(value, "value is required");
        set(key, value, NO_EXP);

    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl) {
        requireNonNull(entity, "entity is required");
        requireNonNull(ttl, "ttl is required");
        set(entity.key(), entity.value(), (int) ttl.getSeconds());
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities) {
        requireNonNull(entities, "entities is required");
        entities.forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities, Duration ttl) {
        requireNonNull(entities, "entities is required");
        requireNonNull(ttl, "ttl is required");
        entities.forEach(e -> this.put(e, ttl));
    }

    @Override
    public <K> Optional<Value> get(K key) {
        requireNonNull(key, "key is required");
        return ofNullable(client.get(getKey(key))).map(Value::of);
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) {
        requireNonNull(keys, "keys is required");

        return stream(keys.spliterator(), false)
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public <K> void delete(K key) {
        requireNonNull(key, "key is required");
        client.delete(getKey(key));
    }

    @Override
    public <K> void delete(Iterable<K> keys) {
        requireNonNull(keys, "keys is required");
        stream(keys.spliterator(), false).forEach(this::delete);
    }

    @Override
    public void close() {
    }

    private <K> String getKey(K key) {
        return bucketName + ':' + key.toString();
    }

    private void set(Object key, Object value, int exp) {
        client.set(getKey(key), exp, value);
    }
}
