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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.arangodb.communication;


import com.arangodb.ArangoDB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import org.eclipse.jnosql.communication.driver.ValueJSON;
import org.eclipse.jnosql.communication.keyvalue.BucketManager;
import org.eclipse.jnosql.communication.keyvalue.KeyValueEntity;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * The ArangoDB implementation to {@link BucketManager} it does not support TTL methods:
 * <p>{@link BucketManager#put(Iterable, Duration)}</p>
 * <p>{@link BucketManager#put(Iterable, Duration)}</p>
 */
public class ArangoDBBucketManager implements BucketManager, ArangoDBAccessor {


    private static final String KEY = "_key";
    private static final String VALUE = "_value";
    private static final Function<JsonObject, String> TO_JSON = e -> e.getString(VALUE);
    private static final Jsonb JSONB = JsonbSupplier.getInstance().get();

    private final ArangoDB arangoDB;

    private final String bucketName;
    private final String namespace;


    ArangoDBBucketManager(ArangoDB arangoDB, String bucketName, String namespace) {
        this.arangoDB = arangoDB;
        this.bucketName = bucketName;
        this.namespace = namespace;
    }

    @Override
    public String name() {
        return bucketName;
    }

    @Override
    public <K, V> void put(K key, V value) throws NullPointerException {
        Objects.requireNonNull(key, "Key is required");
        Objects.requireNonNull(value, "value is required");
        JsonObject jsonObject = Json.createObjectBuilder()
                .add(KEY, key.toString())
                .add(VALUE, JSONB.toJson(value))
                .build();
        if (arangoDB.db(bucketName).collection(namespace).documentExists(key.toString())) {
            arangoDB.db(bucketName).collection(namespace).deleteDocument(key.toString());
        }
        arangoDB.db(bucketName).collection(namespace)
                .insertDocument(jsonObject);
    }

    @Override
    public void put(KeyValueEntity entity) throws NullPointerException {
        put(entity.key(), entity.value());
    }


    @Override
    public  void put(Iterable<KeyValueEntity> keyValueEntities) throws NullPointerException {
        keyValueEntities.forEach(this::put);
    }


    @Override
    public <K> Optional<Value> get(K key) throws NullPointerException {
        Objects.requireNonNull(key, "Key is required");
        JsonObject entity = arangoDB.db(bucketName).collection(namespace)
                .getDocument(key.toString(), JsonObject.class);

        return ofNullable(entity)
                .map(TO_JSON)
                .map(ValueJSON::of);

    }

    @Override
    public <K> Iterable<Value> get(Iterable<K> keys) throws NullPointerException {
        return stream(keys.spliterator(), false)
                .map(Object::toString)
                .map(k -> arangoDB.db(bucketName).collection(namespace)
                        .getDocument(k, JsonObject.class))
                .filter(Objects::nonNull)
                .map(TO_JSON)
                .map(ValueJSON::of)
                .collect(toList());
    }

    @Override
    public <K> void delete(K key) throws NullPointerException {
        arangoDB.db(bucketName).collection(namespace).deleteDocument(key.toString());
    }

    @Override
    public <K> void delete(Iterable<K> keys) throws NullPointerException {
        Objects.requireNonNull(keys, "Keys is required");

        arangoDB.db(bucketName).collection(namespace)
                .deleteDocuments(stream(keys.spliterator(), false)
                        .map(Object::toString).collect(toList()));
    }

    @Override
    public void close() {
        arangoDB.shutdown();
    }

    @Override
    public void put(Iterable<KeyValueEntity> keyValueEntities, Duration ttl) throws NullPointerException, UnsupportedOperationException {
        throw new UnsupportedOperationException("ArangoDB does not support TTL");
    }

    @Override
    public void put(KeyValueEntity entity, Duration ttl) throws NullPointerException, UnsupportedOperationException {
        throw new UnsupportedOperationException("ArangoDB does not support TTL");
    }

    @Override
    public ArangoDB getArangoDB() {
        return arangoDB;
    }

}
