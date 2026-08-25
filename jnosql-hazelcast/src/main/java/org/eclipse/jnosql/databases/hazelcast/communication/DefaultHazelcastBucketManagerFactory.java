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
package org.eclipse.jnosql.databases.hazelcast.communication;

import com.hazelcast.core.HazelcastInstance;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class DefaultHazelcastBucketManagerFactory implements HazelcastBucketManagerFactory {

    private final HazelcastInstance hazelcastInstance;

    DefaultHazelcastBucketManagerFactory(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public HazelcastBucketManager apply(String bucketName) {
        requireNonNull(bucketName, "bucketName is required");
        return new DefaultHazelcastBucketManager(hazelcastInstance.getMap(bucketName), bucketName);
    }

    @Override
    public <T> List<T> getList(String bucketName, Class<T> clazz) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(clazz, "clazz is required");
        return hazelcastInstance.getList(bucketName);
    }

    @Override
    public <T> Set<T> getSet(String bucketName, Class<T> clazz) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(clazz, "clazz is required");
        return hazelcastInstance.getSet(bucketName);
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName, Class<T> clazz) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(clazz, "clazz is required");
        return hazelcastInstance.getQueue(bucketName);
    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName, Class<K> keyValue, Class<V> valueValue) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(keyValue, "keyValue is required");
        requireNonNull(valueValue, "valueValue is required");
        return hazelcastInstance.getMap(bucketName);
    }

    @Override
    public void close() {

    }

    @Override
    public <T> List<T> getList(String bucketName) throws UnsupportedOperationException, NullPointerException {
        requireNonNull(bucketName, "bucketName is required");
        return hazelcastInstance.getList(bucketName);
    }

    @Override
    public <T> Set<T> getSet(String bucketName) throws UnsupportedOperationException, NullPointerException {
        requireNonNull(bucketName, "bucketName is required");
        return hazelcastInstance.getSet(bucketName);
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName) throws UnsupportedOperationException, NullPointerException {
        requireNonNull(bucketName, "bucketName is required");
        return hazelcastInstance.getQueue(bucketName);
    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName) throws UnsupportedOperationException, NullPointerException {
        requireNonNull(bucketName, "bucketName is required");
        return hazelcastInstance.getMap(bucketName);
    }
}
