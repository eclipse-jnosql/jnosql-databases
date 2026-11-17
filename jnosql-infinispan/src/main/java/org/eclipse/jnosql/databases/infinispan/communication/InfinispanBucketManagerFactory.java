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

import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.infinispan.commons.api.BasicCacheContainer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * The Infinispan implementation of {@link BucketManagerFactory}. Each bucket is an Infinispan cache with that
 * name.
 *
 * <p>Only {@link #getMap(String, Class, Class)} is supported among the collection factories: it returns the
 * cache itself as a {@link Map}. {@link #getList(String, Class)}, {@link #getSet(String, Class)} and
 * {@link #getQueue(String, Class)} always throw {@link UnsupportedOperationException}. {@link #close()} does
 * not stop the underlying cache manager.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * InfinispanBucketManagerFactory factory = new InfinispanKeyValueConfiguration().apply(Settings.settings());
 * InfinispanBucketManager users = factory.apply("users");
 * Map<String, String> cache = factory.getMap("users", String.class, String.class);
 * }</pre>
 *
 * @see InfinispanKeyValueConfiguration
 */
public class InfinispanBucketManagerFactory implements BucketManagerFactory {

    private final BasicCacheContainer cacheContainer;

    InfinispanBucketManagerFactory(BasicCacheContainer cacheContainer) {
        this.cacheContainer = cacheContainer;
    }

    @Override
    public InfinispanBucketManager apply(String bucketName) {
        Objects.requireNonNull(bucketName, "bucketName is required");
        return new InfinispanBucketManager(cacheContainer.getCache(bucketName), bucketName);
    }

    @Override
    public <T> List<T> getList(String bucketName, Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Set<T> getSet(String bucketName, Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName, Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName, Class<K> keyValue, Class<V> valueValue) {
        return cacheContainer.getCache(bucketName);
    }

    @Override
    public void close() {

    }
}
