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
import com.basho.riak.client.api.cap.UnresolvedConflictException;
import com.basho.riak.client.api.commands.kv.DeleteValue;
import com.basho.riak.client.api.commands.kv.FetchValue;
import com.basho.riak.client.api.commands.kv.FetchValue.Response;
import com.basho.riak.client.api.commands.kv.StoreValue;
import com.basho.riak.client.core.query.Namespace;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.driver.ValueJSON;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class RiakBucketManager implements BucketManager {

    private final RiakClient client;

    private final Namespace nameSpace;

    private final String bucketName;

    RiakBucketManager(RiakClient client, Namespace nameSpace, String bucketName) {
        this.client = client;
        this.nameSpace = nameSpace;
        this.bucketName = bucketName;
    }

    @Override
    public String name() {
        return bucketName;
    }

    @Override
    public <K, V> void put(K key, V value) throws NullPointerException {
        put(KeyValueEntity.of(key, value));
    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        put(entity, Duration.ZERO);
    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl)
            throws NullPointerException, UnsupportedOperationException {

        Object key = entity.key();
        Object value = entity.value();

        StoreValue storeValue = RiakUtils.createStoreValue(key, value, nameSpace, ttl);

        try {
            client.execute(storeValue);
        } catch (ExecutionException | InterruptedException e) {
            throw new RiakCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities) throws NullPointerException {
        StreamSupport.stream(entities.spliterator(), false).forEach(this::put);
    }

    @Override
    public void put(Iterable<KeyValueEntity> entities, Duration ttl)
            throws NullPointerException, UnsupportedOperationException {

        StreamSupport.stream(entities.spliterator(), false).forEach(e -> put(e, ttl));
    }

    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {
        Objects.requireNonNull(key, "key is required");
        if (key.toString().isEmpty()) {
            throw new RiakCommunicationException("The Key is irregular", new IllegalStateException());
        }

        FetchValue fetchValue = RiakUtils.createFetchValue(nameSpace, key);
        try {

            FetchValue.Response response = client.execute(fetchValue);

            String valueFetch = response.getValue(String.class);
            if (Objects.nonNull(valueFetch) && !valueFetch.isEmpty()) {
                return Optional.of(ValueJSON.of(valueFetch));
            }

        } catch (ExecutionException | InterruptedException e) {
            throw new RiakCommunicationException(e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) throws NullPointerException {

        return StreamSupport.stream(keys.spliterator(), false)
                .map(k -> RiakUtils.createLocation(nameSpace, k))
                .map(l -> new FetchValue.Builder(l).build())
                .map(f -> {
                            try {
                                return client.execute(f);
                            } catch (ExecutionException | InterruptedException e) {
                                throw new RiakCommunicationException(e.getMessage(), e);
                            }
                        }
                )
                .filter(Response::hasValues)
                .map(r -> {

                    try {
                        return r.getValue(String.class);
                    } catch (UnresolvedConflictException e) {
                        throw new RiakCommunicationException(e.getMessage(), e);
                    }

                })
                .filter(s -> Objects.nonNull(s) && !s.isEmpty()).map(ValueJSON::of)
                .collect(toList());
    }


    @Override
    public <K> void delete(K key) throws NullPointerException {

        DeleteValue deleteValue = RiakUtils.createDeleteValue(nameSpace, key);

        try {
            client.execute(deleteValue);
        } catch (ExecutionException | InterruptedException e) {
            throw new RiakCommunicationException(e.getMessage(), e);
        }
    }

    @Override
    public <K> void delete(Iterable<K> keys) throws NullPointerException {
        StreamSupport.stream(keys.spliterator(), false).forEach(this::delete);
    }

    @Override
    public void close() {
        client.shutdown();
    }
}
