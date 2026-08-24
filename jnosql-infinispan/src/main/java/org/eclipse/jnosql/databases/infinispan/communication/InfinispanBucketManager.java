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
 *   The Infinispan Team
 */

package org.eclipse.jnosql.databases.infinispan.communication;

import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;
import org.infinispan.commons.api.BasicCache;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * The Infinispan implementation of {@link BucketManager}
 */
public class InfinispanBucketManager implements BucketManager {

    private final BasicCache cache;

    private final String bucket;

    InfinispanBucketManager(BasicCache cache, String bucket) {
        this.cache = cache;
        this.bucket = bucket;
    }

    @Override
    public String name() {
        return bucket;
    }

    @Override
    public <K, V> void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        cache.put(entity.key(), entity.value());
    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl) {
        cache.put(entity.key(), entity.value(), ttl.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities) throws NullPointerException {
        StreamSupport.stream(entities.spliterator(), false).forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities, Duration ttl) throws NullPointerException, UnsupportedOperationException {
        StreamSupport.stream(entities.spliterator(), false).forEach(kv -> this.put(kv, ttl));
    }

    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {
        Object value = cache.get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Value.of(value));
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) throws NullPointerException {
        return StreamSupport.stream(keys.spliterator(), false).map((Function<K, Object>) cache::get).filter(Objects::nonNull)
                .map(Value::of).collect(Collectors.toList());
    }

    @Override
    public <K> void delete(K key) {
        cache.remove(key);
    }

    @Override
    public <K> void delete(Iterable<K> keys) {
        StreamSupport.stream(keys.spliterator(), false).forEach(this::delete);
    }

    @Override
    public void close() {
    }
}
