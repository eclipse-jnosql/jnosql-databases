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


import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.driver.ValueJSON;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * The redis implementation to {@link BucketManager}
 */
public class RedisBucketManager implements BucketManager {

    private final String nameSpace;
    private final Jsonb jsonB;

    private final UnifiedJedis jedis;

    RedisBucketManager(String nameSpace, Jsonb provider, UnifiedJedis jedis) {
        this.nameSpace = nameSpace;
        this.jsonB = provider;
        this.jedis = jedis;
    }

    @Override
    public String name() {
        return nameSpace;
    }

    @Override
    public <K, V> void put(K key, V value) throws NullPointerException {
        Objects.requireNonNull(value, "Value is required");
        Objects.requireNonNull(key, "key is required");
        String valideKey = RedisUtils.createKeyWithNameSpace(key.toString(), nameSpace);
        jedis.set(valideKey, jsonB.toJson(value));
    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        put(entity.key(), entity.value());
    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl) throws NullPointerException, UnsupportedOperationException {
        put(entity);
        String valideKey = RedisUtils.createKeyWithNameSpace(entity.key().toString(), nameSpace);
        jedis.expire(valideKey, (int) ttl.getSeconds());
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities) throws NullPointerException {
        StreamSupport.stream(entities.spliterator(), false).forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities, Duration ttl) throws NullPointerException, UnsupportedOperationException {
        StreamSupport.stream(entities.spliterator(), false).forEach(this::put);
        StreamSupport.stream(entities.spliterator(), false).map(KeyValueEntity::key)
                .map(k -> RedisUtils.createKeyWithNameSpace(k.toString(), nameSpace))
                .forEach(k -> jedis.expire(k, (int) ttl.getSeconds()));
    }

    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {
        String value = jedis.get(RedisUtils.createKeyWithNameSpace(key.toString(), nameSpace));
        if (value != null && !value.isEmpty()) {
            return Optional.of(ValueJSON.of(value));
        }
        return Optional.empty();
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) throws NullPointerException {
        return StreamSupport.stream(keys.spliterator(), false)
                .map(k -> jedis.get(RedisUtils.createKeyWithNameSpace(k.toString(), nameSpace)))
                .filter(value -> value != null && !value.isEmpty())
                .map(ValueJSON::of).collect(toList());
    }

    @Override
    public <K> void delete(K key) {
        jedis.del(RedisUtils.createKeyWithNameSpace(key.toString(), nameSpace));
    }

    @Override
    public <K> void delete(Iterable<K> keys) {
        StreamSupport.stream(keys.spliterator(), false).forEach(this::delete);
    }

    @Override
    public void close() {
        jedis.close();
    }
}
