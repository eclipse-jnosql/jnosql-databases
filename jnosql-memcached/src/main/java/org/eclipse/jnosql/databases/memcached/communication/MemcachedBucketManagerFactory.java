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
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

final class MemcachedBucketManagerFactory implements BucketManagerFactory {

    private final MemcachedClient client;

    MemcachedBucketManagerFactory(MemcachedClient client) {
        this.client = client;
    }

    @Override
    public MemcachedBucketManager apply(String bucketName) {
        Objects.requireNonNull(bucketName, "bucketName is required");
        return new MemcachedBucketManager(client, bucketName);
    }

    @Override
    public void close() {
    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName, Class<K> keyValue, Class<V> valueValue) {
        throw new UnsupportedOperationException("Memcached does not support Map");
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName, Class<T> type) {
        throw new UnsupportedOperationException("Memcached does not support queue");
    }

    @Override
    public <T>  Set<T>  getSet(String bucketName, Class<T>  type) {
        throw new UnsupportedOperationException("Memcached does not support set");
    }

    @Override
    public <T> List<T>  getList(String bucketName, Class<T> type) {
        throw new UnsupportedOperationException("This method is not supported");
    }
}
