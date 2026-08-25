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
package org.eclipse.jnosql.databases.riak.communication;

import com.basho.riak.client.api.RiakClient;
import com.basho.riak.client.core.RiakCluster;
import com.basho.riak.client.core.query.Namespace;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * The riak implementation to {@link BucketManagerFactory} that returns {@link RiakBucketManager}
 * This implementation just has support to {@link RiakBucketManagerFactory#apply(String)}
 * So, these metdhos will returns {@link UnsupportedOperationException}
 * <p>{@link BucketManagerFactory#getList(String, Class)}
 * <p>{@link BucketManagerFactory#getSet(String, Class)}</p>
 * <p>{@link BucketManagerFactory#getQueue(String, Class)}</p>
 * <p>{@link BucketManagerFactory#getMap(String, Class, Class)}</p>
 */
public class RiakBucketManagerFactory implements BucketManagerFactory {

    private final RiakCluster cluster;

    RiakBucketManagerFactory(RiakCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public RiakBucketManager apply(String bucketName) throws UnsupportedOperationException {
        Objects.requireNonNull(bucketName, "bucketName is required");
        cluster.start();
        RiakClient riakClient = new RiakClient(cluster);
        Namespace quotesBucket = new Namespace(bucketName);

        return new RiakBucketManager(riakClient, quotesBucket, bucketName);
    }

    @Override
    public <T> List<T> getList(String bucketName, Class<T> clazz) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The riak does not support getList method");
    }

    @Override
    public <T> Set<T> getSet(String bucketName, Class<T> clazz) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The riak does not support getSet method");
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName, Class<T> clazz) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The riak does not support getQueue method");
    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName, Class<K> keyValue, Class<V> valueValue)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The riak does not support getMap method");
    }

    @Override
    public void close() {
        cluster.shutdown();
    }

}
