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


import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.UpsertOptions;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import static org.eclipse.jnosql.communication.ValueUtil.convert;

/**
 * The couchbase implementation to {@link BucketManager}
 * <br/>
 * Closing a {@link CouchbaseBucketManager} has no effect.
 */
public class CouchbaseBucketManager implements BucketManager {

    private final Bucket bucket;

    private final String bucketName;

    private final Collection collection;

    private final String collectionName;

    private final String scopeName;


    CouchbaseBucketManager(Bucket bucket, String bucketName, String scopeName, String collectionName) {
        this.bucket = bucket;
        this.bucketName = bucketName;
        this.collectionName = collectionName;
        this.scopeName = scopeName;
        Scope scope = bucket.scope(scopeName);
        this.collection = scope.collection(collectionName);
    }

    @Override
    public String name() {
        return bucketName;
    }

    @Override
    public <K, V> void put(K key, V value) {
        requireNonNull(key, "key is required");
        requireNonNull(value, "value is required");
        waitBucketBeReadyAndGet(() -> collection.upsert(key.toString(), value));
    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        requireNonNull(entity, "entity is required");
        put(entity.key(), convert(Value.of(entity.value())));
    }

    @Override
    public void put(final KeyValueEntity entity, final Duration ttl) {
        requireNonNull(entity, "entity is required");
        requireNonNull(ttl, "ttl is required");
        waitBucketBeReadyAndDo(() -> {
            String key = entity.key(String.class);
            Object value = convert(Value.of(entity.value()));
            collection.upsert(key, value, UpsertOptions.upsertOptions().expiry(ttl));
        });
    }

    @Override
    public void put(Iterable<KeyValueEntity> keyValueEntities) {
        requireNonNull(keyValueEntities, "keyValueEntities is required");
        keyValueEntities.forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> keyValueEntities, Duration ttl) {
        requireNonNull(keyValueEntities, "keyValueEntities is required");
        requireNonNull(ttl, "ttl is required");
        keyValueEntities.forEach(k -> this.put(k, ttl));
    }

    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {
        requireNonNull(key, "key is required");
        try {
            return waitBucketBeReadyAndGet(() -> {
                GetResult result = this.collection.get(key.toString());
                return Optional.of(new CouchbaseValue(result));
            });
        } catch (DocumentNotFoundException exp) {
            return Optional.empty();
        }
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) {
        requireNonNull(keys, "keys is required");
        return stream(keys.spliterator(), false)
                .map(this::get)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    @Override
    public <K> void delete(K key) {
        requireNonNull(key, "key is required");
        waitBucketBeReadyAndDo(() -> collection.remove(key.toString()));
    }

    private void waitBucketBeReadyAndDo(Runnable runnable) {
        bucket.waitUntilReady(bucket.environment().timeoutConfig().kvDurableTimeout());
        runnable.run();
    }


    private <T> T waitBucketBeReadyAndGet(Supplier<T> supplier) {
        bucket.waitUntilReady(bucket.environment().timeoutConfig().kvDurableTimeout());
        return supplier.get();
    }


    @Override
    public <K> void delete(Iterable<K> keys) {
        requireNonNull(keys, "keys is required");
        keys.forEach(this::delete);
    }

    /**
     * Closing a {@link CouchbaseBucketManager} has no effect.
     */
    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "CouchbaseBucketManager{" +
                "bucket=" + bucket +
                ", bucketName='" + bucketName + '\'' +
                ", collection=" + collection +
                ", collectionName='" + collectionName + '\'' +
                '}';
    }
}
