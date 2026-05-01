/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.valkey.communication;

import io.valkey.UnifiedJedis;
import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class DefaultValkeyBucketManagerFactory implements ValkeyBucketManagerFactory {

    private static final Jsonb JSON = JsonbSupplier.getInstance().get();

    private final UnifiedJedis jedis;

    DefaultValkeyBucketManagerFactory(UnifiedJedis jedis) {
        this.jedis = jedis;
    }


    @Override
    public ValkeyBucketManager apply(String bucketName) {
        requireNonNull(bucketName, "bucket name is required");

        return new ValkeyBucketManager(bucketName, JSON, jedis);
    }

    @Override
    public <T> List<T> getList(String bucketName, Class<T> clazz) {
        requireNonNull(bucketName, "bucket name is required");
        requireNonNull(clazz, "Class type is required");
        return new ValkeyList<>(jedis, clazz, bucketName);
    }

    @Override
    public <T> Set<T> getSet(String bucketName, Class<T> clazz) {
        requireNonNull(bucketName, "bucket name is required");
        requireNonNull(clazz, "Class type is required");
        return new ValkeySet<>(jedis, clazz, bucketName);
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName, Class<T> clazz) {
        requireNonNull(bucketName, "bucket name is required");
        requireNonNull(clazz, "Class type is required");
        return new ValkeyQueue<>(jedis, clazz, bucketName);
    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName, Class<K> keyValue, Class<V> valueValue) {
        requireNonNull(bucketName, "bucket name is required");
        requireNonNull(valueValue, "Class type is required");
        return new RedisMap<>(jedis, keyValue, valueValue, bucketName);
    }

    @Override
    public SortedSet getSortedSet(String key) throws NullPointerException {
        requireNonNull(key, "key is required");
        return new DefaultSortedSet(jedis, key);
    }

    @Override
    public Counter getCounter(String key) throws NullPointerException {
        requireNonNull(key, "key is required");
        return new DefaultCounter(key, jedis);
    }


    @Override
    public void close() {
        jedis.close();
    }

    @Override
    public String toString() {
        return "RedisBucketManagerFactory{" + "jedisPool=" + jedis +
                '}';
    }
}
