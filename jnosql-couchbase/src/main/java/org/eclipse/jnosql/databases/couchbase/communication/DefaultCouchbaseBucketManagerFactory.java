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

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.datastructures.CouchbaseArrayList;
import com.couchbase.client.java.datastructures.CouchbaseArraySet;
import com.couchbase.client.java.kv.ArrayListOptions;
import com.couchbase.client.java.kv.ArraySetOptions;
import com.couchbase.client.java.kv.MapOptions;
import com.couchbase.client.java.kv.QueueOptions;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * The couchbase implementation of BucketManagerFactory. That has support the structure {@link Map}, {@link Set},
 * {@link Queue}, {@link List}. Each structure has this specific implementation.
 */
class DefaultCouchbaseBucketManagerFactory implements CouchbaseBucketManagerFactory {

    static final String QUEUE = ":queue";
    static final String SET = ":set";
    static final String LIST = ":list";

    private final CouchbaseSettings settings;
    private final Cluster cluster;


    DefaultCouchbaseBucketManagerFactory(CouchbaseSettings settings) {
        this.settings = settings;
        this.cluster = this.settings.getCluster();
    }


    @Override
    public CouchbaseBucketManager apply(String bucketName) {
        requireNonNull(bucketName, "bucket is required");
        Bucket bucket = cluster.bucket(bucketName);
        String scopeName = settings.getScope().orElseGet(() -> bucket.defaultScope().name());
        String collection = settings.getCollection().orElseGet(() -> bucket.defaultCollection().name());
        return new CouchbaseBucketManager(bucket, bucketName, scopeName, collection);
    }

    @Override
    public BucketManager getBucketManager(String bucketName, String collection) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(collection, "collection is required");
        Bucket bucket = cluster.bucket(bucketName);
        String scopeName = settings.getScope().orElseGet(() -> bucket.defaultScope().name());
        return new CouchbaseBucketManager(bucket, bucketName, scopeName, collection);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Map<K, V> getMap(String bucketName, Class<K> keyValue, Class<V> valueValue) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(valueValue, "valueValue is required");
        requireNonNull(keyValue, "keyValue is required");

        if (!String.class.isAssignableFrom(keyValue)) {
            throw new UnsupportedOperationException("Couchbase Map does not support a not String key instead of: "
                    + keyValue);
        }
        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);
        return (Map<K, V>)
                new com.couchbase.client.java.datastructures.CouchbaseMap<>(bucketName + ":map",
                        collection, valueValue,
                        MapOptions.mapOptions());

    }

    @Override
    public <K, V> Map<K, V> getMap(String bucketName, String key, Class<K> keyType, Class<V> valueType) {

        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(key, "key is required");
        requireNonNull(valueType, "valueValue is required");
        requireNonNull(keyType, "keyValue is required");

        if (!String.class.isAssignableFrom(keyType)) {
            throw new UnsupportedOperationException("Couchbase Map does not support a not String key instead of: "
                    + keyType);
        }

        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);

        return (Map<K, V>)
                new com.couchbase.client.java.datastructures.
                        CouchbaseMap<>(key, collection, valueType,
                        MapOptions.mapOptions());
    }

    @Override
    public <T> Queue<T> getQueue(String bucketName, Class<T> type) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(type, "type is required");

        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);

        return new com.couchbase.client.java.datastructures.CouchbaseQueue<>(bucketName + QUEUE, collection, type,
                QueueOptions.queueOptions());

    }

    @Override
    public <T> Set<T> getSet(String bucketName, Class<T> type) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(type, "type is required");

        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);

        return new CouchbaseArraySet<>(bucketName + SET, collection, type, ArraySetOptions.arraySetOptions());
    }

    @Override
    public <T> List<T> getList(String bucketName, Class<T> type) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(type, "type is required");
        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);
        return new CouchbaseArrayList<>(bucketName + LIST, collection, type, ArrayListOptions.arrayListOptions());
    }


    @Override
    public <T> Queue<T> getQueue(String bucketName, String key, Class<T> type) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(type, "valueValue is required");
        requireNonNull(key, "key is required");
        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);

        return new com.couchbase.client.java.datastructures.CouchbaseQueue<>(key + QUEUE, collection, type,
                QueueOptions.queueOptions());

    }

    @Override
    public <T> Set<T> getSet(String bucketName, String key, Class<T> type) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(type, "valueValue is required");
        requireNonNull(key, "key is required");
        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);

        return new CouchbaseArraySet<>(key, collection, type, ArraySetOptions.arraySetOptions());

    }

    @Override
    public <T> List<T> getList(String bucketName, String key, Class<T> type) {
        requireNonNull(bucketName, "bucketName is required");
        requireNonNull(type, "valueValue is required");
        requireNonNull(key, "key is required");
        Bucket bucket = this.cluster.bucket(bucketName);
        Collection collection = bucket.collection(bucketName);
        return new CouchbaseArrayList<>(key, collection, type, ArrayListOptions.arrayListOptions());
    }


    @Override
    public void close() {
        cluster.close();
    }
}
